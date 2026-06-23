package com.emr.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "emr.auth.token-ttl=PT0S"
)
class TokenExpirationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void expiredTokenCannotBeValidated() {
        Map loginResponse = restTemplate.postForObject("/auth/login", Map.of(
                "username", "admin",
                "password", "admin123",
                "roleCode", "admin"
        ), Map.class);
        Map loginData = (Map) loginResponse.get("data");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", String.valueOf(loginData.get("token")));

        Map validateResponse = restTemplate.postForObject(
                "/auth/token/validate",
                new HttpEntity<>(null, headers),
                Map.class
        );

        assertThat(validateResponse).containsEntry("code", 400);
        assertThat(validateResponse).containsEntry("message", "Token无效或已过期");
    }
}
