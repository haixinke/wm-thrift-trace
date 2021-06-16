package com.wm.rpc.thrift.trace;

import com.wm.rpc.thrift.expand.ExpandContexts;
import com.wm.rpc.thrift.identity.IdentityProduceTCompactProtocol;
import com.wm.rpc.thrift.identity.IdentityProvider;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.opentracing.tag.Tags;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.HashMap;
import java.util.Map;

/**
 * Desc: TraceClientProtocol
 *
 * @author wangmin
 * Date: 2021/2/9
 * Time: 2:49 下午
 */
public class TraceProduceTProtocol extends IdentityProduceTCompactProtocol {

    static final String SPAN = "span";
    public static final String EXPAND = "expand";
    private final Tracer tracer;
    public final static ThreadLocal<Span> CLIENT_LOCAL_ACTIVE_SPAN = new ThreadLocal<>();
    private final boolean finishSpan;
    private final ClientSpanDecorator spanDecorator;

    /**
     * span magic number
     */
    static final short SPAN_FIELD_ID = 9999;

    /**
     * expand magic number
     */
    static final short EXPAND_FIELD_ID = 10000;
    private boolean oneWay;
    private boolean injected;
    private boolean expand;

    public TraceProduceTProtocol(TTransport transport, Tracer tracer, ClientSpanDecorator spanDecorator, IdentityProvider identityProvider) {
        super(transport, identityProvider);
        this.tracer = tracer;
        this.finishSpan = true;
        this.spanDecorator = spanDecorator;
    }

    public TraceProduceTProtocol(TTransport transport, Tracer tracer,
                                 boolean finishSpan, IdentityProvider identityProvider) {
        super(transport, identityProvider);
        this.tracer = tracer;
        this.finishSpan = finishSpan;
        this.spanDecorator = new DefaultClientSpanDecorator();
    }

    @Override
    public void writeMessageBegin(TMessage tMessage) throws TException {
        Span span;
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(tMessage.name).withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

        //server(client) produce
        if (TraceConsumeTProtocol.SERVER_LOCAL_ACTIVE_SPAN.get() != null) {
            spanBuilder.asChildOf(TraceConsumeTProtocol.SERVER_LOCAL_ACTIVE_SPAN.get());
        }
        span = spanBuilder.start();
        CLIENT_LOCAL_ACTIVE_SPAN.set(span);
        oneWay = tMessage.type == TMessageType.ONEWAY;
        injected = false;
        spanDecorator.decorate(span, tMessage);
        if (ExpandContexts.getExpandContext() != null && !ExpandContexts.getExpandContext().isEmpty()) {
            expand = true;
        }
        super.writeMessageBegin(tMessage);
    }

    @Override
    public void writeFieldStop() throws TException {
        if (!injected) {
            Span span = CLIENT_LOCAL_ACTIVE_SPAN.get();
            if (span != null) {
                Map<String, String> map = new HashMap<>();
                tracer.inject(span.context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(map));
                super.writeFieldBegin(new TField(SPAN, TType.MAP, SPAN_FIELD_ID));
                writeMapStruct(map);
                injected = true;
            }
        }

        if (expand) {
            super.writeFieldBegin(new TField(EXPAND, TType.MAP, EXPAND_FIELD_ID));
            writeMapStruct(ExpandContexts.getExpandContext());
            expand = false;
        }
        super.writeFieldStop();
    }

    private void writeMapStruct(Map<String, String> map) throws TException {
        super.writeMapBegin(new TMap(TType.STRING, TType.STRING, map.size()));
        for (Map.Entry<String, String> entry : map.entrySet()) {
            super.writeString(entry.getKey());
            super.writeString(entry.getValue());
        }
        super.writeMapEnd();
        super.writeFieldEnd();
    }

    @Override
    public void writeMessageEnd() throws TException {
        try {
            super.writeMessageEnd();
        } finally {
            Span span = CLIENT_LOCAL_ACTIVE_SPAN.get();
            if (span != null && oneWay && finishSpan) {
                span.finish();
                CLIENT_LOCAL_ACTIVE_SPAN.remove();
            }
        }
    }

    @Override
    public TMessage readMessageBegin() throws TException {
        try {
            return super.readMessageBegin();
        } catch (TTransportException tte) {
            Span span = CLIENT_LOCAL_ACTIVE_SPAN.get();
            if (span != null) {
                spanDecorator.onError(tte, span);
                if (finishSpan) {
                    span.finish();
                    CLIENT_LOCAL_ACTIVE_SPAN.remove();
                }
            }
            throw tte;
        }
    }

    @Override
    public void readMessageEnd() throws TException {
        try {
            super.readMessageEnd();
        } finally {
            Span span = CLIENT_LOCAL_ACTIVE_SPAN.get();
            if (span != null && finishSpan) {
                span.finish();
                CLIENT_LOCAL_ACTIVE_SPAN.remove();
            }
        }
    }

    public static class Factory implements TProtocolFactory {
        private final Tracer tracer;
        private final boolean finishSpan;
        private final IdentityProvider identityProvider;

        public Factory(Tracer tracer, boolean finishSpan, IdentityProvider identityProvider) {
            this.tracer = tracer;
            this.finishSpan = finishSpan;
            this.identityProvider = identityProvider;
        }

        @Override
        public TProtocol getProtocol(TTransport trans) {
            return new TraceProduceTProtocol(trans, tracer, finishSpan, identityProvider);
        }
    }
}
