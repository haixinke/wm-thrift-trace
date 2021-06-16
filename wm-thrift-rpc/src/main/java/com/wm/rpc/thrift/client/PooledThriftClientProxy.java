package com.wm.rpc.thrift.client;

import com.wm.rpc.thrift.expand.ProduceExpand;
import com.wm.rpc.thrift.trace.SpanDecorator;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apache.commons.pool2.ObjectPool;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class PooledThriftClientProxy<T> implements InvocationHandler {
    private final ObjectPool<T> pool;

    private final Tracer tracer;

    private final List<ProduceExpand> produceExpandList;

    public PooledThriftClientProxy(ObjectPool<T> pool,Tracer tracer,List<ProduceExpand> produceExpandList) {
        this.pool = pool;
        this.tracer = tracer;
        this.produceExpandList = produceExpandList;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {

        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "equals":
                    return equals(Proxy.getInvocationHandler(objects[0]));
                case "hashCode":
                    return hashCode();
                default:
                    throw new UnsupportedOperationException();
            }
        }

        T client = pool.borrowObject();

        Span span = tracer.activeSpan();
        try {
            if (!CollectionUtils.isEmpty(produceExpandList)) {
                produceExpandList.forEach(ProduceExpand::produce);
            }
            Object result = Proxy.getInvocationHandler(client).invoke(o, method, objects);
            pool.returnObject(client);
            return result;
        } catch (Exception e) {
            pool.invalidateObject(client);
            if (span != null) {
                SpanDecorator.onError(e, span);
                span.finish();
            }
            throw e;
        }
    }
}
