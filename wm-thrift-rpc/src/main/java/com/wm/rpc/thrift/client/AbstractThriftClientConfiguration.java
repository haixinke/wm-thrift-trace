package com.wm.rpc.thrift.client;

import com.facebook.nifty.client.FramedClientChannel;
import com.facebook.nifty.client.FramedLoadBalanceConnector;
import com.facebook.nifty.client.NiftyClientConnector;
import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.wm.rpc.thrift.client.PooledThriftClientProxy;
import com.wm.rpc.thrift.client.ThriftClientPoolFactory;
import com.wm.rpc.thrift.expand.ProduceExpand;
import com.wm.rpc.thrift.identity.IdentityProvider;
import com.wm.rpc.thrift.multiplex.TMultiplexedProtocolFactory;
import com.wm.rpc.thrift.trace.TraceProduceTProtocol;
import io.opentracing.Tracer;
import org.apache.commons.pool2.ObjectPool;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@ComponentScan
public abstract class AbstractThriftClientConfiguration {

    @Autowired
    ThriftClientPoolFactory thriftClientPoolFactory;

    @Autowired
    IdentityProvider identityProvider;

    @Autowired
    private LoadBalancerClient loadBalancer;

    @Autowired
    private Environment env;

    @Autowired
    private Tracer tracer;

    @Autowired
    ApplicationContext applicationContext;


    public NiftyClientConnector<FramedClientChannel> connector(String serviceName){
        return new FramedLoadBalanceConnector( tProtocolFactory(serviceName), loadBalancer, getDiscoveryName());
    }

    protected abstract String getDiscoveryName();


    public <T> T getClient(Class<T> type) {
        Map<String, ProduceExpand> produceExpandMap = applicationContext.getBeansOfType(ProduceExpand.class);
        List<ProduceExpand> produceExpandList = new ArrayList<>();
        if (produceExpandMap != null && !produceExpandMap.isEmpty()) {
            produceExpandList.addAll(produceExpandMap.values());
        }
        String clientTimeoutKey = "client." + type.getSimpleName() + ".timeout";
        String timeoutValue = env.getProperty(clientTimeoutKey);
        if (StringUtils.isEmpty(timeoutValue)) {
            String serviceName = type.getSimpleName();
            ObjectPool<T> pool = thriftClientPoolFactory.create(connector(serviceName), type);
            return type.cast(
                    Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                            new PooledThriftClientProxy<>(pool,tracer,produceExpandList))
            );
        } else {
            return getClientReadTimeout(type,Integer.parseInt(timeoutValue),produceExpandList);
        }
    }

    public <T> T getClientReadTimeout(Class<T> type, int readTimeoutSeconds,List<ProduceExpand> produceExpandList) {
        String serviceName = type.getSimpleName();
        ObjectPool<T> pool = thriftClientPoolFactory.createWithReadTimeout(connector(serviceName), type, readTimeoutSeconds);
        return type.cast(
                Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                        new PooledThriftClientProxy<>(pool,tracer,produceExpandList))
        );
    }

    public TDuplexProtocolFactory tProtocolFactory(String serviceName) {
        TProtocolFactory factory = new TMultiplexedProtocolFactory(
                new TraceProduceTProtocol.Factory(tracer,true,identityProvider),
                serviceName);
        return TDuplexProtocolFactory.fromSingleFactory(factory);
    }
}
