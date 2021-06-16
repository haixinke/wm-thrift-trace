package com.wm.spring.boot.autoconfigure.rpc.trace.zipkin;

import io.opentracing.contrib.java.spring.zipkin.starter.ZipkinAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.kafka.KafkaSender;

/**
 * Desc: zipkin kafka配置
 *
 * @author wangmin
 * Date: 2021/2/24
 * Time: 3:20 下午
 */
@Configuration
@Import(ZipkinSampleConfiguration.class)
@EnableConfigurationProperties(TraceReportProperties.class)
@ConditionalOnProperty(name = "wm.trace.report.transport", havingValue = "kafka")
@AutoConfigureBefore(ZipkinAutoConfiguration.class)
public class ZipkinKafkaAutoConfiguration {

    @Autowired
    private TraceReportProperties traceReportProperties;

    @Bean
    public Reporter<Span> reporter() {
        return AsyncReporter.builder(kafkaSender()).build();
    }

    @Bean
    public Sender kafkaSender(){
        return KafkaSender.newBuilder().bootstrapServers(traceReportProperties.getKafkaBootstrapServers()).topic(traceReportProperties.getTopic()).build();
    }
}
