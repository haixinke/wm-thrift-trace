package com.wm.spring.boot.autoconfigure.rpc.thrift;

import com.facebook.swift.service.ThriftClientManager;
import com.wm.rpc.thrift.ThriftConfiguration;
import com.wm.rpc.thrift.client.AbstractThriftClientConfiguration;
import com.wm.rpc.thrift.client.ThriftClientPoolFactory;
import com.wm.rpc.thrift.identity.IdentityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ThriftClientProperties.class)
@AutoConfigureBefore(AbstractThriftClientConfiguration.class)
@EnableDiscoveryClient
public class ThriftClientAutoConfiguration extends ThriftConfiguration {

    @Autowired
    ThriftClientProperties properties;

    @Bean
    public ThriftClientManager thriftClientManager() {
        return new ThriftClientManager(thriftCodecManager());
    }

    @Bean
    public ThriftClientPoolFactory thriftClientPoolFactory() {
        return new ThriftClientPoolFactory(thriftClientManager(), properties.getMaxConnections());
    }

    @Bean
    @ConditionalOnMissingBean(IdentityProvider.class)
    public IdentityProvider identityProvider() {
        return ()->0l;
    }

}
