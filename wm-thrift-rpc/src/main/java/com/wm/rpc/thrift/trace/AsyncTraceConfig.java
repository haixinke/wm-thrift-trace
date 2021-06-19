package com.wm.rpc.thrift.trace;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Desc: AsyncTraceConfig
 *
 * @author wangmin
 * Date: 2021/3/4
 * Time: 2:27 下午
 */
@Slf4j
@Configuration
@ConditionalOnBean(annotation = EnableAsync.class)
public class AsyncTraceConfig implements AsyncConfigurer, ApplicationContextAware {

    @Autowired
    private Tracer tracer;

    private ApplicationContext applicationContext;

    private TaskDecorator taskDecorator;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init(){
        Map<String, TaskDecorator> taskDecoratorMap = applicationContext.getBeansOfType(TaskDecorator.class);
        if (taskDecoratorMap != null && !taskDecoratorMap.isEmpty()) {
            taskDecorator = taskDecoratorMap.values().iterator().next();
        } else {
            taskDecorator = new TraceTaskDecorator();
        }
    }

    @Bean
//    @ConditionalOnMissingBean
    public Executor traceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(taskDecorator);
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return traceExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            Span parentSpan = TraceConsumeTProtocol.SERVER_LOCAL_ACTIVE_SPAN.get();
            if (parentSpan != null) {
                Tracer.SpanBuilder spanBuilder = tracer.buildSpan(method.getName()).withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
                spanBuilder.asChildOf(parentSpan);
                Span span = spanBuilder.start();
                SpanDecorator.onError(ex, span);
                span.finish();
            }
            if (log.isErrorEnabled()) {
                log.error(String.format("Unexpected error occurred invoking async " +
                        "method '%s'.", method), ex);
            }
        };
    }
}
