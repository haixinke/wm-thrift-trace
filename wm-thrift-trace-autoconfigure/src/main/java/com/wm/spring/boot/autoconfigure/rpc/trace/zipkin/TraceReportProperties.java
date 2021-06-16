package com.wm.spring.boot.autoconfigure.rpc.trace.zipkin;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Desc: trace report
 *
 * @author wangmin
 * Date: 2021/3/2
 * Time: 4:11 下午
 */
@Data
@ConfigurationProperties(prefix = "wm.trace.report")
public class TraceReportProperties {

    private boolean enabled = true;

    private float sampleRate = 1.0f;

    /**
     * log或kafka
     */
    private String  transport = "log";

    private String kafkaBootstrapServers = "127.0.0.1:9092";

    private String topic = "zipkin";

}
