package com.wm.spring.boot.autoconfigure.rpc.thrift;

import com.facebook.nifty.core.NettyServerConfigBuilder;
import com.facebook.nifty.core.NettyServerTransport;
import com.facebook.nifty.core.ThriftServerDef;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.wm.rpc.thrift.common.ThriftServiceHandler;
import com.wm.rpc.thrift.server.ThriftServerConfiguration;
import com.wm.rpc.thrift.trace.AsyncTraceConfig;
import io.airlift.units.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnBean(annotation = ThriftServiceHandler.class)
@EnableConfigurationProperties(ThriftServerProperties.class)
@Import(AsyncTraceConfig.class)
@Slf4j
public class ThriftServerAutoConfiguration extends ThriftServerConfiguration implements ApplicationRunner {

    @Autowired
    ThriftServerProperties properties;

    @Override
    public void run(ApplicationArguments args) {

        ThriftServerDef serverDef = new ThriftServerDefBuilder().withProcessor(niftyProcessor())
                .clientIdleTimeout(
                        new Duration(properties.getClientIdleTimeoutSeconds(),
                                TimeUnit.SECONDS)
                )
                .protocol(tProtocolFactory())
                .limitQueuedResponsesPerConnection(properties.getQueuedResponseLimit())
                .listen(properties.getListenPort()).build();


        log.info("Nifty transport config : workerThreadCount => {} , bossThreadCount => {}",properties.getWorkerThreadCount(),properties.getBossThreadCount());
        final NettyServerTransport server = new NettyServerTransport(
                serverDef, new NettyServerConfigBuilder().setBossThreadCount(properties.getBossThreadCount()).setWorkerThreadCount(properties.getWorkerThreadCount()).build(), new DefaultChannelGroup());

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try {
                server.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

    }
}
