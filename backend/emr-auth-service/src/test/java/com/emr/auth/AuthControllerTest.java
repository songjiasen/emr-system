package com.emr.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginReturnsTokenAndRole() {
        Map<String, String> request = Map.of(
                "username", "admin",
                "password", "admin123",
                "roleCode", "admin"
        );

        Map response = restTemplate.postForObject("/auth/login", request, Map.class);
        Map data = (Map) response.get("data");

        assertThat(response).containsEntry("code", 0);
        assertThat(data).containsEntry("username", "admin");
        assertThat(data).containsEntry("roleCode", "admin");
        assertThat((String) data.get("token")).isNotBlank();
        assertThat((String) data.get("expireAt")).isNotBlank();
    }

    @Test
    void patientRegisterReturnsCreatedPatient() {
        Map<String, String> request = Map.of(
                "username", "patient_register_case",
                "password", "patient123",
                "name", "测试患者",
                "gender", "女",
                "phone", "13800000000"
        );

        Map response = restTemplate.postForObject("/auth/patient/register", request, Map.class);
        Map data = (Map) response.get("data");

        assertThat(response).containsEntry("code", 0);
        assertThat(data).containsEntry("username", "patient_register_case");
        assertThat(data).containsEntry("name", "测试患者");
        assertThat(data).containsEntry("roleCode", "patient");
    }

    @Test
    void loginRejectsWrongPassword() {
        Map<String, String> request = Map.of(
                "username", "admin",
                "password", "wrong-password",
                "roleCode", "admin"
        );

        Map response = restTemplate.postForObject("/auth/login", request, Map.class);

        assertThat(response).containsEntry("code", 400);
        assertThat(response).containsEntry("message", "账号、密码或角色不正确");
    }

    @Test
    void registeredPatientCanLoginAndChangePassword() {
        String username = "patient_password_case";
        restTemplate.postForObject("/auth/patient/register", Map.of(
                "username", username,
                "password", "old-password",
                "name", "密码测试患者",
                "gender", "男",
                "phone", "13900000001"
        ), Map.class);

        Map loginResponse = restTemplate.postForObject("/auth/login", Map.of(
                "username", username,
                "password", "old-password",
                "roleCode", "patient"
        ), Map.class);
        assertThat(loginResponse).containsEntry("code", 0);

        Map changeResponse = restTemplate.postForObject("/auth/password/change", Map.of(
                "username", username,
                "oldPassword", "old-password",
                "newPassword", "new-password"
        ), Map.class);
        assertThat(changeResponse).containsEntry("code", 0);

        Map oldPasswordResponse = restTemplate.postForObject("/auth/login", Map.of(
                "username", username,
                "password", "old-password",
                "roleCode", "patient"
        ), Map.class);
        assertThat(oldPasswordResponse).containsEntry("code", 400);

        Map newPasswordResponse = restTemplate.postForObject("/auth/login", Map.of(
                "username", username,
                "password", "new-password",
                "roleCode", "patient"
        ), Map.class);
        assertThat(newPasswordResponse).containsEntry("code", 0);
    }

    @Test
    void logoutInvalidatesIssuedToken() {
        Map loginResponse = restTemplate.postForObject("/auth/login", Map.of(
                "username", "admin",
                "password", "admin123",
                "roleCode", "admin"
        ), Map.class);
        Map loginData = (Map) loginResponse.get("data");
        String token = (String) loginData.get("token");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);

        restTemplate.postForObject("/auth/logout", new HttpEntity<>(null, headers), Map.class);
        Map validateResponse = restTemplate.postForObject(
                "/auth/token/validate",
                new HttpEntity<>(null, headers),
                Map.class
        );

        assertThat(validateResponse).containsEntry("code", 400);
        assertThat(validateResponse).containsEntry("message", "Token无效或已过期");
    }
}
