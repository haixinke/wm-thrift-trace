package com.wm.rpc.thrift.trace;

import com.wm.rpc.thrift.expand.ExpandContexts;
import com.wm.rpc.thrift.identity.IdentityConsumeTCompactProtocol;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import org.apache.logging.log4j.ThreadContext;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TTransport;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Desc: TraceServerProtocol
 *
 * @author wangmin
 * Date: 2021/2/9
 * Time: 3:01 下午
 */
public class TraceConsumeTProtocol extends IdentityConsumeTCompactProtocol {

    private final static String X_B_3_TRACE_ID = "X-B3-TraceId";
    public final static String LOG_ID = "LogId";
    private final Tracer tracer;
    private TMessage message;
    private Tracer.SpanBuilder spanBuilder;
    private boolean nextSpan;
    private boolean expand;
    private final List<String> mapElements = new ArrayList<>();
    private final List<String> expandElements = new ArrayList<>();
    private Span activeSpan;
    public final static ThreadLocal<Span> SERVER_LOCAL_ACTIVE_SPAN = new ThreadLocal<>();

    public TraceConsumeTProtocol(TTransport transport, Tracer tracer) {
        super(transport);
        this.tracer = tracer;
    }

    @Override
    public TMessage readMessageBegin() throws TException {
        message = super.readMessageBegin();
        spanBuilder = tracer.buildSpan(message.name)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
        return message;
    }

    @Override
    public ByteBuffer readBinary() throws TException {
        ByteBuffer byteBuffer = super.readBinary();
        if (nextSpan) {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes, 0, bytes.length);
            mapElements.add(new String(bytes));
        }
        if (expand) {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes, 0, bytes.length);
            expandElements.add(new String(bytes));
        }
        return byteBuffer;
    }

    @Override
    public TField readFieldBegin() throws TException {
        TField tField = super.readFieldBegin();
        if (tField.id == TraceProduceTProtocol.SPAN_FIELD_ID && tField.type == TType.MAP) {
            nextSpan = true;
        }
        if (tField.id == TraceProduceTProtocol.EXPAND_FIELD_ID && tField.type == TType.MAP) {
            expand = true;
        }
        return tField;
    }

    @Override
    public void readFieldEnd() throws TException {
        if (nextSpan) {
            nextSpan = false;
            buildSpan();
        }
        if (expand) {
            expand = false;
            injectExpand();
        }
        super.readFieldEnd();
    }

    @Override
    public void readMessageEnd() throws TException {
        if (activeSpan == null) {
            activeSpan = spanBuilder.start();
            SpanDecorator.decorate(activeSpan, message);
            SERVER_LOCAL_ACTIVE_SPAN.set(activeSpan);
        }
        super.readMessageEnd();
    }

    @Override
    public void writeMessageBegin(TMessage tMessage) throws TException {
        if (SERVER_LOCAL_ACTIVE_SPAN.get() != null) {
            SERVER_LOCAL_ACTIVE_SPAN.get().setTag(SpanDecorator.MESSAGE_TYPE, tMessage.type);
        }
        super.writeMessageBegin(tMessage);
    }

    public static class Factory implements TProtocolFactory {
        private final Tracer tracer;

        public Factory(Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public TProtocol getProtocol(TTransport trans) {
            return new TraceConsumeTProtocol(trans, tracer);
        }
    }

    /**
     * 注入expand
     */
    private void injectExpand(){
        if (expandElements.isEmpty()) {
            return;
        }

        Map<String, String> expandContext = new HashMap<>();
        for (int i = 0; i < expandElements.size() - 1; i += 2) {
            expandContext.put(expandElements.get(i), expandElements.get(i + 1));
        }
        expandContext.forEach(ExpandContexts::put);
    }

    /**
     * 构建span
     */
    private void buildSpan() {
        if (mapElements.isEmpty()) {
            return;
        }

        Map<String, String> mapSpanContext = new HashMap<>();
        for (int i = 0; i < mapElements.size() - 1; i += 2) {
            mapSpanContext.put(mapElements.get(i), mapElements.get(i + 1));
        }

        //注入LogId
        ThreadContext.put(LOG_ID, mapSpanContext.getOrDefault(X_B_3_TRACE_ID, ""));
        SpanContext parent = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(mapSpanContext));

        if (parent != null) {
            spanBuilder.asChildOf(parent);
        }
        activeSpan = spanBuilder.start();
        SpanDecorator.decorate(activeSpan, message);
        SERVER_LOCAL_ACTIVE_SPAN.set(activeSpan);
    }

    public void closeSpan() {
        if (activeSpan != null) {
            activeSpan.finish();
            activeSpan = null;
            SERVER_LOCAL_ACTIVE_SPAN.remove();
        }
    }
}
