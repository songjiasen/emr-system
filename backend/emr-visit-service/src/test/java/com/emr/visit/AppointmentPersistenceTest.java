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
class AppointmentPersistenceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createAppointmentPersistsToAppointmentTable() {
        Map response = restTemplate.postForObject("/appointments", Map.of(
                "patientId", 4001,
                "patientName", "落库患者",
                "doctorId", 1,
                "doctorName", "王医生",
                "departmentId", 1,
                "departmentName", "心内科",
                "appointmentTime", "2026-06-26 09:00:00",
                "remark", "复诊"
        ), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from yuyueguahao where patient_id = ? and doctor_id = ? and status = ? and remark = ?",
                Integer.class,
                4001L,
                1L,
                "pending",
                "复诊"
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void cancelAppointmentUpdatesPersistedStatusAndReason() {
        Map createResponse = restTemplate.postForObject("/appointments", Map.of(
                "patientId", 4002,
                "patientName", "取消落库患者",
                "doctorId", 2,
                "doctorName", "李医生",
                "departmentId", 2,
                "departmentName", "儿科",
                "appointmentTime", "2026-06-26 10:00:00"
        ), Map.class);
        Number id = (Number) ((Map<?, ?>) createResponse.get("data")).get("id");

        Map cancelResponse = restTemplate.postForObject(
                "/appointments/" + id.longValue() + "/cancel",
                Map.of("cancelReason", "临时有事"),
                Map.class
        );

        Map row = jdbcTemplate.queryForMap(
                "select status, cancel_reason from yuyueguahao where id = ?",
                id.longValue()
        );

        assertThat(cancelResponse).containsEntry("code", 0);
        assertThat(row).containsEntry("status", "cancelled");
        assertThat(row).containsEntry("cancel_reason", "临时有事");
    }

    @Test
    void listAppointmentsReadsPersistedRows() {
        jdbcTemplate.update(
                "insert into yuyueguahao (appointment_no, patient_id, patient_name, doctor_id, doctor_name, department_id, department_name, appointment_time, status, remark) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "APPT202606260001",
                4999L,
                "数据库患者",
                8L,
                "数据库医生",
                5L,
                "数据库科室",
                "2026-06-26 11:00:00",
                "pending",
                "直接写库"
        );

        Map response = restTemplate.getForObject("/appointments?patientId=4999", Map.class);
        List<?> rows = (List<?>) ((Map<?, ?>) response.get("data")).get("rows");

        assertThat(response).containsEntry("code", 0);
        assertThat(rows).anySatisfy(item -> {
            Map row = (Map) item;
            assertThat(row).containsEntry("patientName", "数据库患者");
            assertThat(row).containsEntry("doctorName", "数据库医生");
        });
    }

    @Test
    void patientCannotQueryOrCancelOtherPatientsAppointment() {
        jdbcTemplate.update(
                "insert into yuyueguahao (appointment_no, patient_id, patient_name, doctor_id, doctor_name, appointment_time, status) values (?, ?, ?, ?, ?, ?, ?)",
                "APPT202606260099",
                5002L,
                "他人预约患者",
                18L,
                "他人预约医生",
                "2026-06-26 12:00:00",
                "pending"
        );
        Long appointmentId = jdbcTemplate.queryForObject(
                "select id from yuyueguahao where appointment_no = ?",
                Long.class,
                "APPT202606260099"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "5001");
        headers.add("X-Username", "patient_demo");
        headers.add("X-Role-Code", "patient");
        headers.add("X-User-Table", "huanzhe");

        ResponseEntity<Map> listResponse = restTemplate.exchange(
                "/appointments?patientId=5002",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        ResponseEntity<Map> cancelResponse = restTemplate.exchange(
                "/appointments/" + appointmentId + "/cancel",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("cancelReason", "越权取消"), headers),
                Map.class
        );

        assertThat(listResponse.getBody()).containsEntry("code", 400);
        assertThat(listResponse.getBody()).containsEntry("message", "当前登录患者只能查询自己的预约");
        assertThat(cancelResponse.getBody()).containsEntry("code", 400);
        assertThat(cancelResponse.getBody()).containsEntry("message", "当前登录患者无权操作该预约");
    }
}
