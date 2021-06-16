package com.wm.rpc.thrift.identity;

public interface IdentityProvider {
    String CONTEXT_KEY = "identity";

    Long get();
}
