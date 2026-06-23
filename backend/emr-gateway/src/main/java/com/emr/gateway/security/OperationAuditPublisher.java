package com.emr.gateway.security;

import reactor.core.publisher.Mono;

/**
 * 审计日志发布器。
 * 网关只负责在合适的时机产生日志事件，具体如何落库交给发布器实现，降低过滤器和下游系统的耦合。
 */
@FunctionalInterface
public interface OperationAuditPublisher {

    /**
     * 发布一条审计日志事件。
     * 发布失败不能影响主业务请求，因此调用方会在链路中自行兜底异常。
     */
    Mono<Void> publish(OperationAuditEvent event);
}
