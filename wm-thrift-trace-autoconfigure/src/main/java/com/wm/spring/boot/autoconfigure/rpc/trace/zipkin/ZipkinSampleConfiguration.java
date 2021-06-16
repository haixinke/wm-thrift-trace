package com.wm.spring.boot.autoconfigure.rpc.trace.zipkin;

import brave.sampler.BoundarySampler;
import brave.sampler.Sampler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Desc: 采样率配置
 *
 * @author wangmin
 * Date: 2021/3/19
 * Time: 10:58 上午
 */
@Configuration
@EnableConfigurationProperties(TraceReportProperties.class)
public class ZipkinSampleConfiguration {

    @Autowired
    private TraceReportProperties traceReportProperties;

    @Bean
    public Sampler sampler() {
        return BoundarySampler.create(traceReportProperties.getSampleRate());
    }

}
