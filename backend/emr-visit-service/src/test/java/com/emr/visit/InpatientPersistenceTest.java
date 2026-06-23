package com.emr.visit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InpatientPersistenceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createTriagePersistsToTriageTable() {
        Map response = restTemplate.postForObject("/triage-records", Map.of(
                "patientId", 6101,
                "patientName", "分诊患者",
                "nurseId", 701,
                "nurseName", "护士甲",
                "chiefComplaint", "发热",
                "triageLevel", "urgent"
        ), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from fenzhenjiandang where patient_id = ? and nurse_id = ? and triage_level = ? and status = ?",
                Integer.class,
                6101L,
                701L,
                "urgent",
                "created"
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void createAdmissionPersistsToAdmissionTable() {
        Map response = restTemplate.postForObject("/inpatients/admissions", Map.of(
                "patientId", 6201,
                "patientName", "入院患者",
                "doctorId", 801,
                "doctorName", "住院医生",
                "nurseId", 802,
                "nurseName", "住院护士",
                "wardNo", "A01",
                "bedNo", "02",
                "admissionTime", "2026-06-27 09:30:00",
                "reason", "观察治疗"
        ), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from ruyuanxinxi where patient_id = ? and ward_no = ? and bed_no = ? and status = ?",
                Integer.class,
                6201L,
                "A01",
                "02",
                "in_hospital"
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void createAdmissionRejectsOccupiedBed() {
        jdbcTemplate.update(
                "insert into ruyuanxinxi (admission_no, patient_id, patient_name, ward_no, bed_no, admission_time, status) values (?, ?, ?, ?, ?, ?, ?)",
                "ADM202606270099",
                6202L,
                "已占床患者",
                "A02",
                "08",
                "2026-06-27 08:30:00",
                "in_hospital"
        );

        Map response = restTemplate.postForObject("/inpatients/admissions", Map.of(
                "patientId", 6203,
                "patientName", "新入院患者",
                "wardNo", "A02",
                "bedNo", "08",
                "admissionTime", "2026-06-27 10:30:00"
        ), Map.class);

        assertThat(response).containsEntry("code", 400);
        assertThat(response).containsEntry("message", "当前床位已被占用");
    }

    @Test
    void dischargeCreatesDischargeRowAndUpdatesAdmissionStatus() {
        jdbcTemplate.update(
                "insert into ruyuanxinxi (admission_no, patient_id, patient_name, doctor_id, doctor_name, nurse_id, nurse_name, ward_no, bed_no, admission_time, reason, status) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "ADM202606270001",
                6301L,
                "出院患者",
                901L,
                "出院医生",
                902L,
                "出院护士",
                "B01",
                "03",
                "2026-06-27 10:00:00",
                "入院观察",
                "in_hospital"
        );
        Long admissionId = jdbcTemplate.queryForObject(
                "select id from ruyuanxinxi where admission_no = ?",
                Long.class,
                "ADM202606270001"
        );

        Map response = restTemplate.postForObject(
                "/inpatients/admissions/" + admissionId + "/discharge",
                Map.of(
                        "dischargeTime", "2026-06-28 11:00:00",
                        "dischargeReason", "病情稳定",
                        "dischargeSummary", "符合出院条件"
                ),
                Map.class
        );

        Integer dischargeCount = jdbcTemplate.queryForObject(
                "select count(*) from chuyuanxinxi where admission_id = ? and patient_id = ?",
                Integer.class,
                admissionId,
                6301L
        );
        String admissionStatus = jdbcTemplate.queryForObject(
                "select status from ruyuanxinxi where id = ?",
                String.class,
                admissionId
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(dischargeCount).isEqualTo(1);
        assertThat(admissionStatus).isEqualTo("discharged");
    }

    @Test
    void dischargeRejectsDuplicatedSubmission() {
        jdbcTemplate.update(
                "insert into ruyuanxinxi (admission_no, patient_id, patient_name, admission_time, status) values (?, ?, ?, ?, ?)",
                "ADM202606270003",
                6302L,
                "重复出院患者",
                "2026-06-27 10:00:00",
                "in_hospital"
        );
        Long admissionId = jdbcTemplate.queryForObject(
                "select id from ruyuanxinxi where admission_no = ?",
                Long.class,
                "ADM202606270003"
        );

        Map firstResponse = restTemplate.postForObject(
                "/inpatients/admissions/" + admissionId + "/discharge",
                Map.of("dischargeTime", "2026-06-28 09:00:00"),
                Map.class
        );
        Map secondResponse = restTemplate.postForObject(
                "/inpatients/admissions/" + admissionId + "/discharge",
                Map.of("dischargeTime", "2026-06-28 10:00:00"),
                Map.class
        );

        Integer dischargeCount = jdbcTemplate.queryForObject(
                "select count(*) from chuyuanxinxi where admission_id = ?",
                Integer.class,
                admissionId
        );

        assertThat(firstResponse).containsEntry("code", 0);
        assertThat(secondResponse).containsEntry("code", 400);
        assertThat(secondResponse).containsEntry("message", "当前入院记录已办理出院");
        assertThat(dischargeCount).isEqualTo(1);
    }

    @Test
    void admissionListReadsPersistedRows() {
        jdbcTemplate.update(
                "insert into ruyuanxinxi (admission_no, patient_id, patient_name, admission_time, status) values (?, ?, ?, ?, ?)",
                "ADM202606270002",
                6401L,
                "列表入院患者",
                "2026-06-27 13:00:00",
                "in_hospital"
        );

        Map response = restTemplate.getForObject("/inpatients/admissions?page=1&limit=10", Map.class);
        List<?> rows = (List<?>) ((Map<?, ?>) response.get("data")).get("rows");

        assertThat(response).containsEntry("code", 0);
        assertThat(rows).anySatisfy(item -> {
            Map row = (Map) item;
            assertThat(row).containsEntry("patientName", "列表入院患者");
            assertThat(row).containsEntry("status", "in_hospital");
        });
    }

    @Test
    void createTriageFallsBackToTrustedNurseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "7801");
        headers.add("X-Username", "nurse_demo");
        headers.add("X-Role-Code", "nurse");
        headers.add("X-User-Table", "users");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/triage-records",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "patientId", 6801,
                        "patientName", "头信息分诊患者",
                        "chiefComplaint", "胸闷"
                ), headers),
                Map.class
        );

        Map row = jdbcTemplate.queryForMap(
                "select nurse_id, nurse_name from fenzhenjiandang where patient_id = ?",
                6801L
        );

        assertThat(response.getBody()).containsEntry("code", 0);
        assertThat(row).containsEntry("nurse_id", 7801L);
        assertThat(row).containsEntry("nurse_name", "nurse_demo");
    }
}
