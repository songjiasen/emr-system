package com.emr.record;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MedicalRecordPersistenceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createRecordPersistsToMedicalRecordTable() {
        Map response = restTemplate.postForObject("/medical-records", Map.of(
                "appointmentId", 11,
                "appointmentNo", "APPT202606260011",
                "patientId", 4101,
                "patientName", "落库病历患者",
                "doctorId", 21,
                "doctorName", "落库医生",
                "visitTime", "2026-06-26 09:10:00",
                "chiefComplaint", "头晕",
                "diagnosis", "血压波动",
                "treatmentAdvice", "监测血压"
        ), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from binglixinxi where appointment_id = ? and patient_id = ? and doctor_id = ? and archive_status = ?",
                Integer.class,
                11L,
                4101L,
                21L,
                "not_submitted"
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void listRecordsReadsPersistedRows() {
        jdbcTemplate.update(
                "insert into binglixinxi (record_no, appointment_id, appointment_no, patient_id, patient_name, doctor_id, doctor_name, visit_time, diagnosis, treatment_advice, archive_status) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "MR202606260001",
                31L,
                "APPT202606260031",
                4201L,
                "数据库病历患者",
                32L,
                "数据库医生",
                "2026-06-26 10:00:00",
                "胃肠炎",
                "清淡饮食",
                "not_submitted"
        );

        Map response = restTemplate.getForObject("/medical-records?patientId=4201", Map.class);
        List<?> rows = (List<?>) ((Map<?, ?>) response.get("data")).get("rows");

        assertThat(response).containsEntry("code", 0);
        assertThat(rows).anySatisfy(item -> {
            Map row = (Map) item;
            assertThat(row).containsEntry("patientName", "数据库病历患者");
            assertThat(row).containsEntry("diagnosis", "胃肠炎");
        });
    }

    @Test
    void updateRecordPersistsDiagnosisChange() {
        Map createResponse = restTemplate.postForObject("/medical-records", Map.of(
                "patientId", 4301,
                "patientName", "更新病历患者",
                "doctorId", 41,
                "doctorName", "更新医生",
                "visitTime", "2026-06-26 11:00:00",
                "diagnosis", "初始诊断"
        ), Map.class);
        Number id = (Number) ((Map<?, ?>) createResponse.get("data")).get("id");

        restTemplate.put("/medical-records/" + id.longValue(), Map.of(
                "diagnosis", "更新后诊断",
                "treatmentAdvice", "继续观察"
        ));

        Map row = jdbcTemplate.queryForMap(
                "select diagnosis, treatment_advice from binglixinxi where id = ?",
                id.longValue()
        );

        assertThat(row).containsEntry("diagnosis", "更新后诊断");
        assertThat(row).containsEntry("treatment_advice", "继续观察");
    }

    @Test
    void archivedRecordCannotBeUpdated() {
        jdbcTemplate.update(
                "insert into binglixinxi (record_no, patient_id, patient_name, doctor_id, doctor_name, visit_time, diagnosis, archive_status) values (?, ?, ?, ?, ?, ?, ?, ?)",
                "MR202606260002",
                4401L,
                "归档患者",
                42L,
                "归档医生",
                "2026-06-26 12:00:00",
                "归档诊断",
                "archived"
        );
        Long id = jdbcTemplate.queryForObject(
                "select id from binglixinxi where record_no = ?",
                Long.class,
                "MR202606260002"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/medical-records/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("diagnosis", "尝试修改")),
                Map.class
        );

        assertThat(response.getBody()).containsEntry("code", 400);
        assertThat(response.getBody()).containsEntry("message", "已归档病历不允许修改");
    }

    @Test
    void doctorCannotCreateMedicalRecordForAnotherDoctor() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "5101");
        headers.add("X-Username", "doctor_demo");
        headers.add("X-Role-Code", "doctor");
        headers.add("X-User-Table", "yisheng");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/medical-records",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "patientId", 5102,
                        "patientName", "跨医生患者",
                        "doctorId", 9999,
                        "doctorName", "越权医生",
                        "visitTime", "2026-06-26 15:00:00",
                        "diagnosis", "越权录入"
                ), headers),
                Map.class
        );

        assertThat(response.getBody()).containsEntry("code", 400);
        assertThat(response.getBody()).containsEntry("message", "当前登录医生只能为自己创建病历");
    }
}
