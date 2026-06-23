package com.emr.gateway.security;

import reactor.core.publisher.Mono;

/**
 * Token 校验端口。
 * 过滤器只依赖该接口，避免把认证服务调用细节耦合进权限判断。
 */
@FunctionalInterface
public interface TokenValidator {

    Mono<AuthContext> validate(String token);
}
