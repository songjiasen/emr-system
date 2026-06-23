package com.emr.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointReturnsServiceName() {
        Map response = restTemplate.getForObject("/health", Map.class);

        assertThat(response).containsEntry("code", 0);
        assertThat((Map) response.get("data")).containsEntry("service", "emr-auth-service");
    }
}
