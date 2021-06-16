package com.wm.spring.boot.autoconfigure.rpc.trace;

import brave.opentracing.BraveSpanContext;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * Desc: 生成traceId
 * User: wangmin
 * Date: 2019/5/5
 * Time: 7:32 PM
 */
@AllArgsConstructor
public class TraceIdFilter implements Filter {

    private String applicationName;

    private final static String TRANCEID = "TraceId";
    private final static String LOGID = "LogId";
    private static final String X_B_3_TRACE_ID = "X-B3-TraceId";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String traceId = "";
        Object spanContext = httpRequest.getAttribute("io.opentracing.contrib.web.servlet.filter.TracingFilter.activeSpanContext");
        if (spanContext instanceof BraveSpanContext) {
            BraveSpanContext braveSpanContext = (BraveSpanContext) spanContext;
            traceId = braveSpanContext.unwrap().traceIdString();
        }
        if (StringUtils.isEmpty(traceId)) {
            String logId = UUID.randomUUID().toString().replaceAll("-","");
            ThreadContext.put(LOGID, applicationName + "-" +logId);
        } else {
            ThreadContext.put(LOGID, traceId);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
