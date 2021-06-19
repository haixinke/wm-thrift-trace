# Thrift Trace Start Instrumentation
## 背景
Thrift是facebook开发的高性能的服务通信框架，很多公司使用该框架实现微服务的rpc通信，但是使用过程中，发现市面上没有一套针对该框架的服务链路追踪的实现，
这给性能定位及问题排查带来了很大的麻烦。基于上述原因，通过改造thrift的通信协议，基于[openingTrace](https://opentracing.io/specification/)标准，集成filebeat、kafka、elk、
[zipkin-server](https://github.com/openzipkin/zipkin/tree/master/zipkin-server)、[zipkin-dependencies](https://github.com/openzipkin/zipkin-dependencies)
等组件实现微服务的链路追踪、统计功能。
该框架集成了[swift](https://github.com/facebookarchive/swift)、[nifty](https://github.com/facebookarchive/nifty)。
基于spring boot框架。

## 功能
- 支持Http请求自动创建链路信息
- 微服务RPC之间创建、传递链路信息
- 微服务RPC支持扩展信息传递
- 支持采样功能关闭
- 支持采样率设置
- 支持多线程之间传递链路信息

## 整体架构图
### 直接推送kafka(网上扒的图)
![trace](https://user-images.githubusercontent.com/6084920/122634526-37751000-d111-11eb-9f10-5b48992bbdda.png)

### 本地落日志(自己画的，凑合看)
![QQ20210619-153133](https://user-images.githubusercontent.com/6084920/122634954-b408ee00-d113-11eb-8a52-93171c189def.png)


简单的系统，微服务数量不多、并发不大，可以用上面的架构，足以满足，该架构缺点就是所有服务都要依赖kafka。
如果微服务数量很多，并发也比较大，第一步推送kafka调整为落地本地日志，然后通过filebeat离线采集日志，推送kafka，后面的流程都是一样的。我们公司有大概七八十个微服务，每天千万级的调用量，所以我们选择链路信息落地本地日志的方式。

## 使用
### 安装

mvn clean install -Dmaven.test.skip=true

### 引用（client与server）
    <dependency>
            <groupId>com.wm.spring.boot</groupId>
            <artifactId>wm-thrift-trace-starter</artifactId>
            <version>1.0.0</version>
    </dependency>
    
### Spring
- 支持spring-boot 1.5.4.RELEASE、2.1.0.RELEASE版本，别的版本没有验证，可能不兼容
- 支持spring-cloud Edgware.SR6、Finchley.SR2，别的版本没有验证，可能不兼容

### 通用配置
| 属性 | 描述 |
| --- | --- |
| wm.trace.report.enabled | 是否开启链路追踪，默认true，开启 |
| wm.trace.report.sample-rate | 采样率，默认为1.0，全部采样 |
| wm.trace.report.transport | 采样通道，默认为kafka，需要指定如下两个参数，如果落地本地日志，设为log |
| wm.trace.report.kafka-bootstrap-servers | 如果通道设置为kafka，该属性指定kafka的地址，包括端口 |
| wm.trace.report.topic | kafka的topic，默认为zipkin |

### server端配置
| 属性 | 描述 |
| --- | --- |
| wm.rpc.thrift.server.listenPort | server端rpc通信端口，默认8080 |
| wm.rpc.thrift.server.clientIdleTimeoutSeconds | client端空闲超时时间，默认300秒|
| wm.rpc.thrift.server.queuedResponseLimit | 每个链接的队列响应限制个数，默认16 |
| wm.rpc.thrift.server.bossThreadCount | netty的boss管理线程数量，默认为1 |
| wm.rpc.thrift.server.workerThreadCount | netty的工作线程数量 |

### client端配置
| 属性 | 描述 |
| --- | --- |
| wm.rpc.thrift.client.maxConnections | client与server的最大连接数，默认为32 |
| client.[server端服务接口名称].timeout | client与某个指定服务的连接超时时间，默认为10秒 |

### 微服务log4j2.xml调整(链路信息本地落日志的方式，直接推送kafka可以忽略)
- appenders目录下新增RollingFile
```
<RollingFile name="TRACE-FILE" fileName="/data/logs/xxx-xxx.zipkin.log" filePattern="/data/logs/xxx-xxx-%d{yyyy-MM-dd}-%i.zipkin.log">
   <PatternLayout pattern="%msg%n" />
   <Policies>
      <TimeBasedTriggeringPolicy modulate="true" interval="1" />
      <SizeBasedTriggeringPolicy size="2GB"/>
   </Policies>
</RollingFile>
```
- loggers目录下新增logger或AsyncLogger
``` 
<Logger name="com.wm.spring.boot.autoconfigure.rpc.trace.zipkin" level="info" additivity="false">
    <appender-ref ref="TRACE-FILE" />
</Logger>
或
<AsyncLogger name="com.wm.spring.boot.autoconfigure.rpc.trace.zipkin" level="info" additivity="false">
    <appender-ref ref="TRACE-FILE" />
</AsyncLogger>
``` 

### server端


### client端


