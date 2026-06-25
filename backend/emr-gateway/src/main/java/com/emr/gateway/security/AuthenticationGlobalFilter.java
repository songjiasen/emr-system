package com.emr.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 网关统一认证过滤器。
 * 在路由转发前完成 Token 验证、角色鉴权和可信身份头注入，业务服务无需重复解析 Token。
 */
@Component
public class AuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final GatewayAccessPolicy accessPolicy;
    private final TokenValidator tokenValidator;
    private final OperationAuditPublisher operationAuditPublisher;

    public AuthenticationGlobalFilter(
            GatewayAccessPolicy accessPolicy,
            TokenValidator tokenValidator,
            OperationAuditPublisher operationAuditPublisher
    ) {
        this.accessPolicy = accessPolicy;
        this.tokenValidator = tokenValidator;
        this.operationAuditPublisher = operationAuditPublisher;
    }

    /**
     * 执行网关认证链。
     * 先清理客户端可能伪造的身份头，再由认证服务生成可信上下文，保证下游只接收网关注入的数据。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getURI().getPath();
        if (accessPolicy.isPublic(method, path)) {
            return chain.filter(clearIdentityHeaders(exchange));
        }

        String token = exchange.getRequest().getHeaders().getFirst("Token");
        if (token == null || token.isBlank()) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "未登录或Token已过期");
        }

        return tokenValidator.validate(token.trim())
                .flatMap(context -> {
                    if (!accessPolicy.isAllowed(context.roleCode(), method, path)) {
                        return writeError(exchange, HttpStatus.FORBIDDEN, "当前角色无权访问该功能")
                                .thenReturn(Boolean.TRUE);
                    }
                    ServerWebExchange trustedExchange = withTrustedContext(exchange, context);
                    long startTime = System.currentTimeMillis();
                    return chain.filter(trustedExchange)
                            .then(publishAuditLogIfNecessary(trustedExchange, context, startTime))
                            .thenReturn(Boolean.TRUE);
                })
                .switchIfEmpty(Mono.defer(() -> writeError(exchange, HttpStatus.UNAUTHORIZED, "Token无效或已过期")
                        .thenReturn(Boolean.FALSE)))
                .then()
                .onErrorResume(exception -> writeError(exchange, HttpStatus.UNAUTHORIZED, "Token校验失败"));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private ServerWebExchange clearIdentityHeaders(ServerWebExchange exchange) {
        return exchange.mutate().request(builder -> builder.headers(headers -> {
            headers.remove("X-User-Id");
            headers.remove("X-Username");
            headers.remove("X-Role-Code");
            headers.remove("X-User-Table");
            headers.remove("X-Department-Id");
            headers.remove("X-Department-Name");
        })).build();
    }

    private ServerWebExchange withTrustedContext(ServerWebExchange exchange, AuthContext context) {
        return exchange.mutate().request(builder -> builder.headers(headers -> {
            headers.remove("X-User-Id");
            headers.remove("X-Username");
            headers.remove("X-Role-Code");
            headers.remove("X-User-Table");
            headers.remove("X-Department-Id");
            headers.remove("X-Department-Name");
            headers.set("X-User-Id", String.valueOf(context.userId()));
            headers.set("X-Username", context.username());
            headers.set("X-Role-Code", context.roleCode());
            headers.set("X-User-Table", context.tableName());
            if (context.departmentId() != null) {
                headers.set("X-Department-Id", String.valueOf(context.departmentId()));
            }
            if (context.departmentName() != null && !context.departmentName().isBlank()) {
                headers.set("X-Department-Name", context.departmentName());
            }
        })).build();
    }

    /**
     * 按审计口径决定是否发布日志。
     * 当前只记录受保护写请求的成功结果，排除 `/syslogs` 自身，避免系统日志接口形成递归写入。
     */
    private Mono<Void> publishAuditLogIfNecessary(ServerWebExchange exchange, AuthContext context, long startTime) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getURI().getPath();
        if (!shouldPublishAuditLog(method, path) || !isSuccessfulResponse(exchange.getResponse().getStatusCode())) {
            return Mono.empty();
        }
        return operationAuditPublisher.publish(buildAuditEvent(exchange, context, method, path, startTime));
    }

    /**
     * 判断当前请求是否属于需要审计的写操作。
     * 这里按 HTTP 语义收敛为 POST、PUT、DELETE、PATCH，减少对只读查询接口的噪音记录。
     */
    private boolean shouldPublishAuditLog(HttpMethod method, String path) {
        if (method == null || path == null || path.isBlank()) {
            return false;
        }
        if (path.endsWith("/syslogs")) {
            return false;
        }
        return HttpMethod.POST.equals(method)
                || HttpMethod.PUT.equals(method)
                || HttpMethod.DELETE.equals(method)
                || Objects.equals(method.name(), "PATCH");
    }

    /**
     * 组装审计事件。
     * 请求体在网关层不做缓存读取，这里先记录 query string，既保留筛选条件，也避免破坏下游读取请求体。
     */
    private OperationAuditEvent buildAuditEvent(
            ServerWebExchange exchange,
            AuthContext context,
            HttpMethod method,
            String path,
            long startTime
    ) {
        String query = exchange.getRequest().getURI().getRawQuery();
        String requestUri = query == null || query.isBlank() ? path : path + "?" + query;
        String ipAddress = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : String.valueOf(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        return new OperationAuditEvent(
                context.userId(),
                context.username(),
                context.roleCode(),
                method.name() + " " + path,
                requestUri,
                method.name(),
                query,
                ipAddress,
                System.currentTimeMillis() - startTime
        );
    }

    /**
     * 判断响应是否可以视为成功。
     * Spring 网关在未显式设置状态码时通常代表默认 200，这里把 `null` 也视为成功，避免遗漏正常写请求。
     */
    private boolean isSuccessfulResponse(HttpStatusCode status) {
        return status == null || status.is2xxSuccessful() || status.is3xxRedirection();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + status.value() + ",\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
