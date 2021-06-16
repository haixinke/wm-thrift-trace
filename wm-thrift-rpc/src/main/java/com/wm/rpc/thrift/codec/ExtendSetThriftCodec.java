package com.wm.rpc.thrift.codec;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.internal.builtin.SetThriftCodec;
import com.facebook.swift.codec.metadata.ThriftType;
import org.apache.thrift.protocol.TProtocol;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Desc: Set codec
 *
 * @author wangmin
 * Date: 2020/5/25
 * Time: 3:10 下午
 */
public class ExtendSetThriftCodec<T> extends SetThriftCodec<T> {


    public ExtendSetThriftCodec(ThriftType type, ThriftCodec elementCodec) {
        super(type, elementCodec);
    }

    @Override
    public void write(Set<T> value, TProtocol protocol) throws Exception {
        if (!CollectionUtils.isEmpty(value)) {
            value = value.stream().filter(t -> t != null).collect(Collectors.toSet());
        }
        super.write(value, protocol);
    }
}
