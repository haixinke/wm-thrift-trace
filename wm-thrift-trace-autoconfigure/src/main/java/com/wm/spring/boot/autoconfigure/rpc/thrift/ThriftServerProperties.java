package com.wm.spring.boot.autoconfigure.rpc.thrift;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wm.rpc.thrift.server")
public class ThriftServerProperties {
    private int listenPort = 8080;

    private int clientIdleTimeoutSeconds = 5*60;

    private int queuedResponseLimit = 16;

    private int workerThreadCount = Runtime.getRuntime().availableProcessors() * 2;
    private int bossThreadCount = 1;
}
