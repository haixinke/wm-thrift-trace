package com.facebook.nifty.client;

import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.nifty.client.NiftyClientConnector;
import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import lombok.extern.slf4j.Slf4j;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.net.InetSocketAddress;

@Slf4j
public abstract class AbstractLoadBalanceConnector<T extends NiftyClientChannel>  implements NiftyClientConnector<T> {
    private final TDuplexProtocolFactory protocolFactory;

    private LoadBalancerClient loadBalancer;

    private String discoveryName;

    public AbstractLoadBalanceConnector(TDuplexProtocolFactory protocolFactory, LoadBalancerClient loadBalancer, String discoveryName) {
        this.protocolFactory = protocolFactory;
        this.loadBalancer = loadBalancer;
        this.discoveryName = discoveryName;
    }

    @Override
    public ChannelFuture connect(ClientBootstrap bootstrap) {
        return bootstrap.connect(getAddress());
    }


    protected InetSocketAddress getAddress() {
        ServiceInstance serviceInstance = loadBalancer.choose(discoveryName);
        if(serviceInstance == null) {
            log.warn("there is no {} server instance discovered!", discoveryName);
            throw new RuntimeException("there is no server instance discovered!");
        }
       return new InetSocketAddress(serviceInstance.getHost(), serviceInstance.getPort());
    }

    protected TDuplexProtocolFactory getProtocolFactory()
    {
        return protocolFactory;
    }

}
