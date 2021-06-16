package com.wm.rpc.thrift.common;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ThriftStruct
@NoArgsConstructor
@Setter
public class ApplicationException extends RuntimeException {
    static final Gson gson = new Gson();

    @ThriftField(1)
    public String getCode() {
        return super.getMessage();
    }


    @Getter(onMethod = @__(@ThriftField(2)))
    private String param;

    @ThriftConstructor
    public ApplicationException(String code, String param) {
        super(code);
        this.param = gson.toJson(param);
    }

    public ApplicationException(Enum code, Object param) {
        this(code.name(), gson.toJson(param));
    }

    public ApplicationException(String code) {
        super(code);
    }

    public ApplicationException(Enum code) {
        super(code.name());
    }


}
