package com.emr.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 系统服务审计日志发布器。
 * 直接调用系统服务的 `/syslogs` 接口，避免再次经过网关导致递归写日志。
 */
@Component
public class SystemServiceOperationAuditPublisher implements OperationAuditPublisher {

    private final WebClient webClient;

    public SystemServiceOperationAuditPublisher(
            WebClient.Builder webClientBuilder,
            @Value("${EMR_AUDIT_SYSTEM_SERVICE_BASE_URL:http://localhost:8108}") String systemServiceBaseUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(systemServiceBaseUrl).build();
    }

    /**
     * 将审计事件写入系统服务。
     * 审计链路是附加能力，不能反向影响主请求，因此统一吞掉远端异常并返回空完成信号。
     */
    @Override
    public Mono<Void> publish(OperationAuditEvent event) {
        if (event == null) {
            return Mono.empty();
        }
        return webClient.post()
                .uri("/syslogs")
                .bodyValue(event.toRequestBody())
                .retrieve()
                .bodyToMono(String.class)
                .then()
                .onErrorResume(exception -> Mono.empty());
    }
}
