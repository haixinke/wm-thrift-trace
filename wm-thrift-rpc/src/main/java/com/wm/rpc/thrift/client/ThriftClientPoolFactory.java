package com.wm.rpc.thrift.client;


import com.facebook.nifty.client.FramedClientChannel;
import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.nifty.client.NiftyClientConnector;
import com.facebook.swift.service.ThriftClientManager;
import io.airlift.units.Duration;
import lombok.AllArgsConstructor;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.concurrent.TimeUnit;

import static com.facebook.swift.service.ThriftClientConfig.*;

@AllArgsConstructor
public class ThriftClientPoolFactory {

    private ThriftClientManager thriftClientManager;

    private int maxConnections;

    public <T> ObjectPool<T> create(NiftyClientConnector<FramedClientChannel> clientConnector, Class<T> type) {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMaxTotal(maxConnections);

        ObjectPool<T> pool = new GenericObjectPool<>(new BasePooledObjectFactory<T>(){

            @Override
            public T create() throws Exception {
                NiftyClientChannel channel = thriftClientManager.createChannel(clientConnector).get();
                return thriftClientManager.createClient(channel,type);
            }

            @Override
            public PooledObject<T> wrap(T obj) {
                return new DefaultPooledObject<>(obj);
            }

            @Override
            public void destroyObject(PooledObject<T> p) {
                thriftClientManager.getRequestChannel(p.getObject()).close();
            }

            @Override
            public boolean validateObject(PooledObject<T> p) {
                NiftyClientChannel channel = ((NiftyClientChannel)thriftClientManager.getRequestChannel(p.getObject()));
                return channel.getNettyChannel().isConnected();
            }
        }, poolConfig
        );

        return pool;
    }

    public <T> ObjectPool<T> createWithReadTimeout(NiftyClientConnector<FramedClientChannel> clientConnector, Class<T> type, int readTimeoutSeconds) {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMaxTotal(maxConnections);

        ObjectPool<T> pool = new GenericObjectPool<>(new BasePooledObjectFactory<T>(){

            @Override
            public T create() throws Exception {
                // NiftyClientChannel channel = thriftClientManager.createChannel(clientConnector).get();
                NiftyClientChannel channel = thriftClientManager.createChannel(clientConnector,
                        DEFAULT_CONNECT_TIMEOUT,
                        DEFAULT_RECEIVE_TIMEOUT,
                        new Duration(readTimeoutSeconds, TimeUnit.SECONDS),
                        DEFAULT_WRITE_TIMEOUT,
                        DEFAULT_MAX_FRAME_SIZE,
                        thriftClientManager.getDefaultSocksProxy()).get();
                return thriftClientManager.createClient(channel,type);
            }

            @Override
            public PooledObject<T> wrap(T obj) {
                return new DefaultPooledObject<>(obj);
            }

            @Override
            public void destroyObject(PooledObject<T> p) {
                thriftClientManager.getRequestChannel(p.getObject()).close();
            }

            @Override
            public boolean validateObject(PooledObject<T> p) {
                NiftyClientChannel channel = ((NiftyClientChannel)thriftClientManager.getRequestChannel(p.getObject()));
                return channel.getNettyChannel().isConnected();
            }
        }, poolConfig
        );

        return pool;
    }
}
