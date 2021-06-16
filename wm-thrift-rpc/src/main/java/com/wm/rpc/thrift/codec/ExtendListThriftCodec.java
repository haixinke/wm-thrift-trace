package com.wm.rpc.thrift.codec;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.internal.builtin.ListThriftCodec;
import com.facebook.swift.codec.metadata.ThriftType;
import org.apache.thrift.protocol.TProtocol;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Desc: List codec
 *
 * @author wangmin
 * Date: 2020/5/25
 * Time: 1:44 下午
 */
public class ExtendListThriftCodec<T> extends ListThriftCodec<T> {


    public ExtendListThriftCodec(ThriftType type, ThriftCodec<T> elementCodec) {
        super(type, elementCodec);
    }

    @Override
    public void write(List<T> value, TProtocol protocol) throws Exception {
        if (!CollectionUtils.isEmpty(value)) {
            value = value.stream().filter(t -> t != null).collect(Collectors.toList());
        }
        super.write(value, protocol);
    }
}
