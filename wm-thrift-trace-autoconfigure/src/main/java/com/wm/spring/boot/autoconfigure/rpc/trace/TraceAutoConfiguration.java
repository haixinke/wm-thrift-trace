package com.wm.spring.boot.autoconfigure.rpc.trace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Desc: 服务跟踪config
 * User: wangmin
 * Date: 2019/6/11
 * Time: 9:25 PM
 */
@Configuration
@EnableConfigurationProperties(RequestLogProperties.class)
public class TraceAutoConfiguration {

    @Autowired
    private RequestLogProperties requestLogProperties;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * request打印
     *
     * @return
     */
    @Bean
    public RequestLogFilter requestLogFilter() {
        RequestLogFilter filter = new RequestLogFilter(requestLogProperties.isEnabled());
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("[");
        return filter;
    }

    /**
     * 生产traceId
     *
     * @return
     */
    @Bean
    public TraceIdFilter traceIdFilter() {
        TraceIdFilter traceIdFilter = new TraceIdFilter(applicationName);
        return traceIdFilter;
    }

}
