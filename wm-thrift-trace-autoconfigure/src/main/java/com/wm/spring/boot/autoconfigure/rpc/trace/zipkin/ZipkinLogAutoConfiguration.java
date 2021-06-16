package com.wm.spring.boot.autoconfigure.rpc.trace.zipkin;

import io.opentracing.contrib.java.spring.zipkin.starter.ZipkinAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import zipkin2.Span;
import zipkin2.reporter.Reporter;

/**
 * Desc: zipkin log配置
 *
 * @author wangmin
 * Date: 2021/2/24
 * Time: 3:20 下午
 */
@Slf4j
@Configuration
@Import(ZipkinSampleConfiguration.class)
@EnableConfigurationProperties(TraceReportProperties.class)
@ConditionalOnProperty(name = "wm.trace.report.transport", havingValue = "log", matchIfMissing = true)
@AutoConfigureBefore(ZipkinAutoConfiguration.class)
public class ZipkinLogAutoConfiguration {

    @Autowired
    private TraceReportProperties traceReportProperties;

    @Bean
    public Reporter<Span> reporter() {
        return span -> {
            if (traceReportProperties.isEnabled()) {
                if (!log.isInfoEnabled()) {
                    return;
                }
                if (span != null) {
                    log.info("[" + span.toString() + "]");
                }
            }
        };
    }

}
