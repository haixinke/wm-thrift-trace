package com.wm.rpc.thrift.server;

import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.swift.service.ExtendThriftServiceProcessor;
import com.google.common.collect.Lists;
import com.wm.rpc.thrift.expand.ConsumeExpand;
import com.wm.rpc.thrift.ThriftConfiguration;
import com.wm.rpc.thrift.common.ThriftServiceHandler;
import com.wm.rpc.thrift.multiplex.TMultiplexedNiftyProcessor;
import com.wm.rpc.thrift.trace.TraceConsumeTProtocol;
import io.opentracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class ThriftServerConfiguration extends ThriftConfiguration {
    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    private Tracer tracer;

    public TDuplexProtocolFactory tProtocolFactory() {
        return TDuplexProtocolFactory.fromSingleFactory(new TraceConsumeTProtocol.Factory(tracer));
    }

    @Bean
    public NiftyProcessor niftyProcessor() {
        List<Object> servicesBeans = applicationContext.getBeansWithAnnotation(ThriftServiceHandler.class)
                .values().stream().collect(Collectors.toList());
        Map<String, ConsumeExpand> consumeExpandMap = applicationContext.getBeansOfType(ConsumeExpand.class);
        List<ConsumeExpand> consumeExpandList = new ArrayList<>();
        if (consumeExpandMap != null && !consumeExpandMap.isEmpty()) {
            consumeExpandList.addAll(consumeExpandMap.values());
        }
        TMultiplexedNiftyProcessor multiplexedNiftyProcessor = new TMultiplexedNiftyProcessor();
        for (Object serviceBean : servicesBeans) {
            ExtendThriftServiceProcessor processor = new ExtendThriftServiceProcessor(thriftCodecManager(), Lists.newArrayList(),consumeExpandList,
                    serviceBean);
            String interfaceName = getInterfaceName(serviceBean);
            multiplexedNiftyProcessor.registerProcessor(interfaceName, processor);
        }

        return multiplexedNiftyProcessor;
    }

    private String getInterfaceName(Object serviceBean) {
        if (serviceBean instanceof org.springframework.cglib.proxy.Factory) {
            return serviceBean.getClass().getSuperclass().getInterfaces()[0].getSimpleName();
        }

        return serviceBean.getClass().getInterfaces()[0].getSimpleName();
    }
}
