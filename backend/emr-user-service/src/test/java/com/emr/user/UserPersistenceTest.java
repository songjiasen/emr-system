package com.emr.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserPersistenceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createDepartmentPersistsToDepartmentTable() {
        Map response = restTemplate.postForObject("/departments", Map.of(
                "name", "康复医学科",
                "sortNo", 6
        ), Map.class);
        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from keshileixing where name = ? and sort_no = ? and status = 1",
                Integer.class,
                "康复医学科",
                6
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void createDoctorPersistsToDoctorTableAndCanBeQueried() {
        String username = "doctor_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Map createResponse = restTemplate.postForObject("/user-management/doctors", Map.of(
                "username", username,
                "name", "赵医生",
                "phone", "13911112222",
                "departmentId", 8,
                "departmentName", "神经内科",
                "specialty", "头痛头晕"
        ), Map.class);
        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from yisheng where username = ? and name = ? and department_name = ? and specialty = ?",
                Integer.class,
                username,
                "赵医生",
                "神经内科",
                "头痛头晕"
        );
        Map listResponse = restTemplate.getForObject("/doctors", Map.class);
        List<?> rows = (List<?>) ((Map<?, ?>) listResponse.get("data")).get("rows");

        assertThat(createResponse).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
        assertThat(rows).anySatisfy(item -> {
            Map row = (Map) item;
            assertThat(row).containsEntry("username", username);
            assertThat(row).containsEntry("departmentName", "神经内科");
        });
    }

    @Test
    void createAdminAndPatientPersistToOwnTables() {
        String adminUsername = "admin_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String patientUsername = "patient_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Map adminResponse = restTemplate.postForObject("/user-management/admins", Map.of(
                "username", adminUsername,
                "name", "后台管理员",
                "phone", "13812345678"
        ), Map.class);
        Map patientResponse = restTemplate.postForObject("/user-management/patients", Map.of(
                "username", patientUsername,
                "name", "住院患者",
                "phone", "13712345678"
        ), Map.class);

        Integer adminCount = jdbcTemplate.queryForObject(
                "select count(*) from users where username = ? and role_code = ? and real_name = ? and status = 1",
                Integer.class,
                adminUsername,
                "admin",
                "后台管理员"
        );
        Integer patientCount = jdbcTemplate.queryForObject(
                "select count(*) from huanzhe where username = ? and name = ? and phone = ? and status = 1",
                Integer.class,
                patientUsername,
                "住院患者",
                "13712345678"
        );
        String adminPassword = jdbcTemplate.queryForObject(
                "select password from users where username = ?",
                String.class,
                adminUsername
        );

        assertThat(adminResponse).containsEntry("code", 0);
        assertThat(patientResponse).containsEntry("code", 0);
        assertThat(adminCount).isEqualTo(1);
        assertThat(patientCount).isEqualTo(1);
        assertThat(adminPassword).isNotBlank();
        assertThat(adminPassword).startsWith("$2");
    }
}
