package com.wm.spring.boot.autoconfigure.rpc.thrift;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wm.rpc.thrift.client")
public class ThriftClientProperties {

    private int maxConnections = 32;
}
