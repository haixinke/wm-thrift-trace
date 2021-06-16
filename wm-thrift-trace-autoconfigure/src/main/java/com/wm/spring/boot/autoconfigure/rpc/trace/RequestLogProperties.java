package com.wm.spring.boot.autoconfigure.rpc.trace;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Desc: Request入参打印配置,默认打开
 * User: wangmin
 * Date: 2019/6/12
 * Time: 7:57 PM
 */
@Data
@ConfigurationProperties(prefix = "wm.request.log")
public class RequestLogProperties {

    private boolean enabled = true;

}
