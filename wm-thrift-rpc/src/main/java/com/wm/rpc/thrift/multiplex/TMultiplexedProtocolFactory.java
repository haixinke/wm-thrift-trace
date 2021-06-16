package com.wm.rpc.thrift.multiplex;


import lombok.AllArgsConstructor;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;

@AllArgsConstructor
public class TMultiplexedProtocolFactory implements TProtocolFactory {
    private TProtocolFactory delegateFactory;
    private String serviceName;


    @Override
    public TProtocol getProtocol(TTransport trans) {
        return new TMultiplexedProtocol(delegateFactory.getProtocol(trans), serviceName);
    }
}
