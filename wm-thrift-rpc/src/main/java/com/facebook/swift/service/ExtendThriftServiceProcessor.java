package com.facebook.swift.service;

import com.facebook.nifty.core.NiftyRequestContext;
import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.core.TNiftyTransport;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.metadata.ThriftMethodMetadata;
import com.facebook.swift.service.metadata.ThriftServiceMetadata;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.*;
import com.wm.rpc.thrift.expand.ConsumeExpand;
import com.wm.rpc.thrift.trace.SpanDecorator;
import com.wm.rpc.thrift.trace.TraceConsumeTProtocol;
import io.opentracing.tag.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.thrift.TApplicationException.INVALID_MESSAGE_TYPE;
import static org.apache.thrift.TApplicationException.UNKNOWN_METHOD;

/**
 * Desc: ExtendThriftServiceProcessor
 *
 * @author wangmin
 * Date: 2021/3/1
 * Time: 2:21 下午
 */
@Slf4j
@ThreadSafe
public class ExtendThriftServiceProcessor implements NiftyProcessor {

    private final Map<String, ExtandThriftMethodProcessor> methods;

    private final List<ThriftEventHandler> eventHandlers;

    /**
     * @param eventHandlers event handlers to attach to services
     * @param services      the services to expose; services must be thread safe
     */
    public ExtendThriftServiceProcessor(ThriftCodecManager codecManager, List<? extends ThriftEventHandler> eventHandlers, List<ConsumeExpand> consumeExpandList, Object... services) {
        this(codecManager, eventHandlers,consumeExpandList, ImmutableList.copyOf(services));
    }

    public ExtendThriftServiceProcessor(ThriftCodecManager codecManager, List<? extends ThriftEventHandler> eventHandlers, List<ConsumeExpand> consumeExpandList, List<?> services) {
        Preconditions.checkNotNull(codecManager, "codecManager is null");
        Preconditions.checkNotNull(services, "service is null");
        Preconditions.checkArgument(!services.isEmpty(), "services is empty");

        Map<String, ExtandThriftMethodProcessor> processorMap = newHashMap();
        for (Object service : services) {
            ThriftServiceMetadata serviceMetadata = new ThriftServiceMetadata(service.getClass(), codecManager.getCatalog());
            for (ThriftMethodMetadata methodMetadata : serviceMetadata.getMethods().values()) {
                String methodName = methodMetadata.getName();
                ExtandThriftMethodProcessor methodProcessor = new ExtandThriftMethodProcessor(service, serviceMetadata.getName(), methodMetadata, codecManager,consumeExpandList);
                if (processorMap.containsKey(methodName)) {
                    throw new IllegalArgumentException("Multiple @ThriftMethod-annotated methods named '" + methodName + "' found in the given services");
                }
                processorMap.put(methodName, methodProcessor);
            }
        }
        methods = ImmutableMap.copyOf(processorMap);
        this.eventHandlers = ImmutableList.copyOf(eventHandlers);
    }

    public Map<String, ExtandThriftMethodProcessor> getMethods() {
        return methods;
    }

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public ListenableFuture<Boolean> process(final TProtocol in, TProtocol out, RequestContext requestContext)
            throws TException {
        String methodName = null;
        int sequenceId = 0;

        try {
            final SettableFuture<Boolean> resultFuture = SettableFuture.create();
            TMessage message = in.readMessageBegin();
            methodName = message.name;
            sequenceId = message.seqid;

            // lookup method
            ExtandThriftMethodProcessor method = methods.get(methodName);
            if (method == null) {
                TProtocolUtil.skip(in, TType.STRUCT);
                createAndWriteApplicationException(out, requestContext, methodName, sequenceId, UNKNOWN_METHOD, "Invalid method name: '" + methodName + "'", null);
                return Futures.immediateFuture(true);
            }

            switch (message.type) {
                case TMessageType.CALL:
                case TMessageType.ONEWAY:
                    // Ideally we'd check the message type here to make the presence/absence of
                    // the "oneway" keyword annotating the method matches the message type.
                    // Unfortunately most clients send both one-way and two-way messages as CALL
                    // message type instead of using ONEWAY message type, and servers ignore the
                    // difference.
                    break;

                default:
                    TProtocolUtil.skip(in, TType.STRUCT);
                    createAndWriteApplicationException(out, requestContext, methodName, sequenceId, INVALID_MESSAGE_TYPE, "Received invalid message type " + message.type + " from client", null);
                    return Futures.immediateFuture(true);
            }

            // invoke method
            final ContextChain context = new ContextChain(eventHandlers, method.getQualifiedName(), requestContext);
            ListenableFuture<Boolean> processResult = method.process(in, out, sequenceId, context);

            Futures.addCallback(
                    processResult,
                    new FutureCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            context.done();
                            resultFuture.set(result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("Failed to process method [" + method.getName() + "] of service [" + method.getServiceName() + "]",t);
                            context.done();
                            resultFuture.setException(t);
                        }
                    }, MoreExecutors.directExecutor());

            return resultFuture;
        } catch (TApplicationException e) {
            // If TApplicationException was thrown send it to the client.
            // This could happen if for example, some of event handlers method threw an exception.
            writeApplicationException(out, requestContext, methodName, sequenceId, e);
            return Futures.immediateFuture(true);
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public static TApplicationException createAndWriteApplicationException(
            TProtocol outputProtocol,
            RequestContext requestContext,
            String methodName,
            int sequenceId,
            int errorCode,
            String errorMessage,
            Throwable cause)
            throws TException {
        // unexpected exception
        TApplicationException applicationException = new TApplicationException(errorCode, errorMessage);
        if (cause != null) {
            applicationException.initCause(cause);
        }

        return writeApplicationException(outputProtocol, requestContext, methodName, sequenceId, applicationException);
    }

    public static TApplicationException writeApplicationException(
            TProtocol outputProtocol,
            RequestContext requestContext,
            String methodName,
            int sequenceId,
            TApplicationException applicationException)
            throws TException {
        log.error(applicationException.getMessage(),applicationException);
        TNiftyTransport requestTransport = requestContext instanceof NiftyRequestContext ? ((NiftyRequestContext) requestContext).getNiftyTransport() : null;

        // Application exceptions are sent to client, and the connection can be reused
        outputProtocol.writeMessageBegin(new TMessage(methodName, TMessageType.EXCEPTION, sequenceId));

        if (TraceConsumeTProtocol.SERVER_LOCAL_ACTIVE_SPAN.get() != null) {
            TraceConsumeTProtocol.SERVER_LOCAL_ACTIVE_SPAN.get().setTag(Tags.ERROR.getKey(), Boolean.TRUE);
            SpanDecorator.onError(applicationException, TraceConsumeTProtocol.SERVER_LOCAL_ACTIVE_SPAN.get());
        }

        applicationException.write(outputProtocol);
        outputProtocol.writeMessageEnd();
        if (requestTransport != null) {
            requestTransport.setTApplicationException(applicationException);
        }
        outputProtocol.getTransport().flush();

        return applicationException;
    }


}
