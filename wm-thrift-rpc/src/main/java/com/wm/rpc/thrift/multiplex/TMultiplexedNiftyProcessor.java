package com.wm.rpc.thrift.multiplex;

import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.processor.NiftyProcessor;
import com.google.common.util.concurrent.ListenableFuture;
import com.wm.rpc.thrift.expand.ExpandContexts;
import com.wm.rpc.thrift.trace.TraceConsumeTProtocol;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;

import java.util.HashMap;
import java.util.Map;

public class TMultiplexedNiftyProcessor implements NiftyProcessor {
    private final Map<String,NiftyProcessor> SERVICE_PROCESSOR_MAP
            = new HashMap<>();

    public void registerProcessor(String serviceName, NiftyProcessor processor) {
        SERVICE_PROCESSOR_MAP.put(serviceName, processor);
    }

    @Override
    public ListenableFuture<Boolean> process(TProtocol in, TProtocol out, RequestContext requestContext) throws TException {
        TMessage message = in.readMessageBegin();

        if (message.type != TMessageType.CALL && message.type != TMessageType.ONEWAY) {
            throw new TException("This should not have happened!?");
        }

        int index = message.name.indexOf(TMultiplexedProtocol.SEPARATOR);
        if (index < 0) {
            throw new TException("Service name not found in message name: " + message.name + ".  Did you " +
                    "forget to use a TMultiplexProtocol in your client?");
        }

        String serviceName = message.name.substring(0, index);
        NiftyProcessor actualProcessor = SERVICE_PROCESSOR_MAP.get(serviceName);
        if (actualProcessor == null) {
            throw new TException("Service name not found: " + serviceName + ".  Did you forget " +
                    "to call registerProcessor()?");
        }

        TMessage standardMessage = new TMessage(
                message.name.substring(serviceName.length()+TMultiplexedProtocol.SEPARATOR.length()),
                message.type,
                message.seqid
        );
        ListenableFuture<Boolean> listenableFuture;
        try {
            listenableFuture = actualProcessor.process(new StoredMessageProtocol(in, standardMessage), out, requestContext);
        } finally {
            if (in instanceof TraceConsumeTProtocol) {
                TraceConsumeTProtocol traceConsumeTProtocol = (TraceConsumeTProtocol) in;
                traceConsumeTProtocol.closeSpan();
            }
            ExpandContexts.clearExpandContext();
        }
        return listenableFuture;
    }

    private static class StoredMessageProtocol extends TProtocolDecorator {
        TMessage messageBegin;
        public StoredMessageProtocol(TProtocol protocol, TMessage messageBegin) {
            super(protocol);
            this.messageBegin = messageBegin;
        }
        @Override
        public TMessage readMessageBegin() throws TException {
            return messageBegin;
        }
    }
}
