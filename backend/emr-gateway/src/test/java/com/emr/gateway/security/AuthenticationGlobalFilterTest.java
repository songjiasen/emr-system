package com.emr.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationGlobalFilterTest {

    private final GatewayAccessPolicy policy = new GatewayAccessPolicy();

    @Test
    void protectedEndpointWithoutTokenReturns401() {
        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(policy, token -> Mono.empty(), event -> Mono.empty());
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/cl584734139/medical-records", null);

        filter.filter(exchange, noOpChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"code\":401");
    }

    @Test
    void authenticatedRequestReceivesTrustedIdentityHeaders() {
        AuthContext context = new AuthContext(7L, "doctor_demo", "doctor", "yisheng", 1L, "心内科");
        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(policy, token -> Mono.just(context), event -> Mono.empty());
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/cl584734139/medical-records", "valid-token");
        AtomicReference<org.springframework.web.server.ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwarded.set(current);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("7");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Username")).isEqualTo("doctor_demo");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Role-Code")).isEqualTo("doctor");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Department-Id")).isEqualTo("1");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Department-Name")).isEqualTo("心内科");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void roleWithoutPermissionReturns403() {
        AuthContext context = new AuthContext(9L, "patient_demo", "patient", "huanzhe");
        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(policy, token -> Mono.just(context), event -> Mono.empty());
        MockServerWebExchange exchange = exchange(HttpMethod.POST, "/cl584734139/news", "valid-token");

        filter.filter(exchange, noOpChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"code\":403");
    }

    @Test
    void nurseCannotAccessWorkflowTaskCenter() {
        AuthContext context = new AuthContext(15L, "nurse_demo", "nurse", "users");
        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(policy, token -> Mono.just(context), event -> Mono.empty());
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/cl584734139/workflow/tasks", "valid-token");

        filter.filter(exchange, noOpChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"code\":403");
    }

    @Test
    void successfulMutationPublishesAuditLog() {
        AuthContext context = new AuthContext(1L, "admin", "admin", "users");
        List<OperationAuditEvent> published = new ArrayList<>();
        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(policy, token -> Mono.just(context), event -> {
            published.add(event);
            return Mono.empty();
        });
        MockServerWebExchange exchange = exchange(HttpMethod.POST, "/cl584734139/news?category=notice", "valid-token");

        filter.filter(exchange, current -> {
            current.getResponse().setStatusCode(HttpStatus.CREATED);
            return Mono.empty();
        }).block();

        assertThat(published).hasSize(1);
        assertThat(published.get(0).userId()).isEqualTo(1L);
        assertThat(published.get(0).username()).isEqualTo("admin");
        assertThat(published.get(0).roleCode()).isEqualTo("admin");
        assertThat(published.get(0).requestMethod()).isEqualTo("POST");
        assertThat(published.get(0).requestUri()).isEqualTo("/cl584734139/news?category=notice");
        assertThat(published.get(0).operation()).isEqualTo("POST /cl584734139/news");
    }

    @Test
    void getRequestDoesNotPublishAuditLog() {
        AuthContext context = new AuthContext(7L, "doctor_demo", "doctor", "yisheng");
        List<OperationAuditEvent> published = new ArrayList<>();
        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(policy, token -> Mono.just(context), event -> {
            published.add(event);
            return Mono.empty();
        });
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/cl584734139/medical-records", "valid-token");

        filter.filter(exchange, noOpChain()).block();

        assertThat(published).isEmpty();
    }

    @Test
    void syslogRequestDoesNotPublishAuditLog() {
        AuthContext context = new AuthContext(1L, "admin", "admin", "users");
        List<OperationAuditEvent> published = new ArrayList<>();
        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(policy, token -> Mono.just(context), event -> {
            published.add(event);
            return Mono.empty();
        });
        MockServerWebExchange exchange = exchange(HttpMethod.POST, "/cl584734139/syslogs", "valid-token");

        filter.filter(exchange, current -> {
            current.getResponse().setStatusCode(HttpStatus.CREATED);
            return Mono.empty();
        }).block();

        assertThat(published).isEmpty();
    }

    private MockServerWebExchange exchange(HttpMethod method, String path, String token) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.method(method, path);
        if (token != null) {
            builder.header("Token", token);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private GatewayFilterChain noOpChain() {
        return exchange -> Mono.empty();
    }
}
