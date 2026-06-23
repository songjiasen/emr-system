package com.emr.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthPersistenceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerPersistsPatientAccountWithPasswordHash() {
        String username = "db_patient_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String password = "patient123";
        Map response = restTemplate.postForObject("/auth/patient/register", Map.of(
                "username", username,
                "password", password,
                "name", "落库患者",
                "gender", "女",
                "phone", "13800001234"
        ), Map.class);
        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from emr_user.huanzhe where username = ?",
                Integer.class,
                username
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);

        Map<String, Object> patientRow = jdbcTemplate.queryForMap(
                "select username, password, name, phone from emr_user.huanzhe where username = ?",
                username
        );
        assertThat(patientRow).containsEntry("username", username);
        assertThat(patientRow).containsEntry("name", "落库患者");
        assertThat(patientRow).containsEntry("phone", "13800001234");
        assertThat(String.valueOf(patientRow.get("password"))).isNotEqualTo(password);
        assertThat(String.valueOf(patientRow.get("password"))).startsWith("$2");
    }

    @Test
    void loginPersistsTokenAndLogoutRemovesIt() {
        String username = "db_token_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String password = "token123";
        restTemplate.postForObject("/auth/patient/register", Map.of(
                "username", username,
                "password", password,
                "name", "令牌患者",
                "gender", "男",
                "phone", "13900001234"
        ), Map.class);

        Map loginResponse = restTemplate.postForObject("/auth/login", Map.of(
                "username", username,
                "password", password,
                "roleCode", "patient"
        ), Map.class);
        Map loginData = (Map) loginResponse.get("data");
        String token = String.valueOf(loginData.get("token"));
        Integer persistedCount = jdbcTemplate.queryForObject(
                "select count(*) from emr_auth.token where token = ? and username = ? and role_code = ? and table_name = ?",
                Integer.class,
                token,
                username,
                "patient",
                "huanzhe"
        );

        assertThat(loginResponse).containsEntry("code", 0);
        assertThat(persistedCount).isEqualTo(1);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        Map logoutResponse = restTemplate.postForObject("/auth/logout", new HttpEntity<>(null, headers), Map.class);
        Integer remainingCount = jdbcTemplate.queryForObject(
                "select count(*) from emr_auth.token where token = ?",
                Integer.class,
                token
        );

        assertThat(logoutResponse).containsEntry("code", 0);
        assertThat(remainingCount).isZero();
    }
}
