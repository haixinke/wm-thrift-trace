# Thrift Trace Instrumentation
## 背景
Thrift是facebook开发的高性能的服务通信框架，很多公司使用该框架实现微服务的rpc通信，但是使用过程中，发现市面上没有一套针对该框架的服务链路追踪的实现，
这给性能定位及问题排查带来了很大的麻烦。基于上述原因，通过改造thrift的通信协议，基于[openingTrace](https://opentracing.io/specification/)标准，集成filebeat、kafka、elk、
[zipkin-server](https://github.com/openzipkin/zipkin/tree/master/zipkin-server)、[zipkin-dependencies](https://github.com/openzipkin/zipkin-dependencies)
等组件实现微服务的链路追踪、统计功能。

## 整体架构图
[架构图](img/trace.png)

简单的系统，微服务数量不多、并发不大，可以用上面的架构，足以满足，该架构缺点就是所有服务都要依赖kafka。
如果微服务数量很多，并发也比较大，第一步推送kafka调整为落地本地日志，然后通过filebeat离线采集日志，推送kafka，后面的
流程都是一样的。我们公司有大概七八十个微服务，每天千万级的调用量，所以我们选择链路信息落地本地日志的方式。

## 使用
### 安装

mvn clean install -Dmaven.test.skip=true

### 引用
    <dependency>
            <groupId>com.wm.spring.boot</groupId>
            <artifactId>wm-thrift-trace-starter</artifactId>
            <version>1.0.0</version>
    </dependency>
    
### server端


### client端


