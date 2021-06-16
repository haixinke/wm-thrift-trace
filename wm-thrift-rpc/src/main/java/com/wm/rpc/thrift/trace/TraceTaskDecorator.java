package com.wm.rpc.thrift.trace;

import com.wm.rpc.thrift.expand.ExpandContexts;
import io.opentracing.Span;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Desc: trace task decorator
 *
 * @author wangmin
 * Date: 2021/3/2
 * Time: 1:06 下午
 */
public class TraceTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Span span = TraceConsumeTProtocol.SERVER_LOCAL_ACTIVE_SPAN.get();
        String logId = ThreadContext.get(TraceConsumeTProtocol.LOG_ID);
        Map<String,String> expandContext = ExpandContexts.getExpandContext();
        return () -> {
            if (span != null) {
                TraceConsumeTProtocol.SERVER_LOCAL_ACTIVE_SPAN.set(span);
            }
            if (logId != null) {
                ThreadContext.put(TraceConsumeTProtocol.LOG_ID,logId);
            }
            if (expandContext != null && !expandContext.isEmpty()) {
                expandContext.forEach(ExpandContexts::put);
            }
            try {
                runnable.run();
            } finally {
                TraceConsumeTProtocol.SERVER_LOCAL_ACTIVE_SPAN.remove();
                ExpandContexts.clearExpandContext();
            }
        };
    }
}
