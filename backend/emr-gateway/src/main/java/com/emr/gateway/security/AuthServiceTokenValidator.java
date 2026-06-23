package com.emr.gateway.security;

import com.emr.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 认证服务 Token 校验器。
 * 网关通过认证服务取得可信用户上下文，认证失败或认证服务不可用时统一视为无效 Token。
 */
@Component
public class AuthServiceTokenValidator implements TokenValidator {

    private final WebClient webClient;

    public AuthServiceTokenValidator(
            WebClient.Builder webClientBuilder,
            @Value("${EMR_AUTH_SERVICE_BASE_URL:http://localhost:8101}") String authServiceBaseUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(authServiceBaseUrl).build();
    }

    /**
     * 调用认证服务校验 Token。
     * 只有 code=0 且用户上下文完整时才向过滤器返回认证结果，避免异常响应被误当成登录成功。
     */
    @Override
    public Mono<AuthContext> validate(String token) {
        if (token == null || token.isBlank()) {
            return Mono.empty();
        }
        return webClient.post()
                .uri("/auth/token/validate")
                .header("Token", token.trim())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<AuthContext>>() {
                })
                .flatMap(response -> response != null
                        && Integer.valueOf(0).equals(response.code())
                        && response.data() != null
                        ? Mono.just(response.data())
                        : Mono.empty())
                .onErrorResume(exception -> Mono.empty());
    }
}
