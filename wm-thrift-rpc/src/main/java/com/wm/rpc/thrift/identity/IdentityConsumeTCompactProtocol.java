package com.wm.rpc.thrift.identity;

import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.core.RequestContexts;
import com.wm.rpc.thrift.identity.IdentityProvider;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.transport.TTransport;

public class IdentityConsumeTCompactProtocol extends TCompactProtocol {


    public IdentityConsumeTCompactProtocol(TTransport transport) {
        super(transport);
    }

    @Override
    public TMessage readMessageBegin() throws TException {
        TMessage message = super.readMessageBegin();
        RequestContext context = RequestContexts.getCurrentContext();
        context.setContextData(IdentityProvider.CONTEXT_KEY, readI64());
        return message;
    }
}
