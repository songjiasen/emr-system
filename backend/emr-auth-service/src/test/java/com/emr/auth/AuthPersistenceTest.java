package com.emr.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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

    @Test
    void doctorHeartDemoAccountCanLogin() {
        Map loginResponse = restTemplate.postForObject("/auth/login", Map.of(
                "username", "doctor_heart",
                "password", "123456",
                "roleCode", "doctor"
        ), Map.class);
        Map loginData = (Map) loginResponse.get("data");

        assertThat(loginResponse).containsEntry("code", 0);
        assertThat(loginData).containsEntry("username", "doctor_heart");
        assertThat(loginData).containsEntry("roleCode", "doctor");
    }

    @Test
    void nurseLoginAndTokenValidationReturnDepartmentScope() {
        String username = "nurse_scope_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String password = "nurse123";
        jdbcTemplate.update(
                "insert into emr_user.users (username, password, real_name, role_code, phone, department_id, department_name, status) values (?, ?, ?, ?, ?, ?, ?, ?)",
                username,
                new BCryptPasswordEncoder().encode(password),
                "心内科护士",
                "nurse",
                "13600000003",
                1L,
                "心内科",
                1
        );

        Map loginResponse = restTemplate.postForObject("/auth/login", Map.of(
                "username", username,
                "password", password,
                "roleCode", "nurse"
        ), Map.class);
        Map loginData = (Map) loginResponse.get("data");
        String token = String.valueOf(loginData.get("token"));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);

        Map validateResponse = restTemplate.postForObject("/auth/token/validate", new HttpEntity<>(null, headers), Map.class);
        Map validateData = (Map) validateResponse.get("data");

        assertThat(loginResponse).containsEntry("code", 0);
        assertThat(loginData).containsEntry("departmentId", 1);
        assertThat(loginData).containsEntry("departmentName", "心内科");
        assertThat(validateResponse).containsEntry("code", 0);
        assertThat(validateData).containsEntry("departmentId", 1);
        assertThat(validateData).containsEntry("departmentName", "心内科");
    }
}
