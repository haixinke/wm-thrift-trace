package com.wm.rpc.thrift.identity;

import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.core.RequestContexts;
import com.wm.rpc.thrift.identity.IdentityProvider;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.transport.TTransport;

public class IdentityProduceTCompactProtocol extends TCompactProtocol {

    IdentityProvider identityProvider;

    public IdentityProduceTCompactProtocol(TTransport transport, IdentityProvider identityProvider) {
        super(transport);
        this.identityProvider = identityProvider;
    }

    @Override
    public void writeMessageBegin(TMessage message) throws TException {
        super.writeMessageBegin(message);
        RequestContext context = RequestContexts.getCurrentContext();
        Object currentIdentity = null;
        if (context != null) {
            currentIdentity = context.getContextData(IdentityProvider.CONTEXT_KEY);
        }
        if (currentIdentity != null) {
            writeI64((Long)currentIdentity);
        } else {
            writeI64(identityProvider.get());
        }
    }
}
