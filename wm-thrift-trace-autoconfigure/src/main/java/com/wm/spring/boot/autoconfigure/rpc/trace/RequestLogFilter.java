package com.wm.spring.boot.autoconfigure.rpc.trace;

import lombok.AllArgsConstructor;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.servlet.http.HttpServletRequest;

/**
 * Desc: 请求打印
 * User: wangmin
 * Date: 2019/6/11
 * Time: 8:22 PM
 */
@AllArgsConstructor
public class RequestLogFilter extends CommonsRequestLoggingFilter {

    private boolean shouldLog;

    @Override
    protected boolean shouldLog(HttpServletRequest request) {
        return shouldLog;
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        logger.info(message);
    }

}
