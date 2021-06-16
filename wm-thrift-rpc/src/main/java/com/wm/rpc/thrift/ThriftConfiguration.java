package com.wm.rpc.thrift;

import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.wm.rpc.thrift.codec.ExtendThriftCodecManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 客户端与服务端需要保持一致的配置
 */
@Configuration
public class ThriftConfiguration {

    @Bean
    public ExtendThriftCodecManager thriftCodecManager() {
        ExtendThriftCodecManager igenThriftCodecManager = new ExtendThriftCodecManager();
        ThriftCatalog catalog = igenThriftCodecManager.getCatalog();
        catalog.addDefaultCoercions(AdditionalCoercions.class);
        return igenThriftCodecManager;
    }
}
