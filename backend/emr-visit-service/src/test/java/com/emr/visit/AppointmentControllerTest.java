package com.emr.visit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AppointmentControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createAppointmentGeneratesAppointmentNoAndPendingStatus() {
        Map<String, Object> request = Map.of(
                "patientId", 1001,
                "patientName", "测试患者",
                "doctorId", 1,
                "doctorName", "王医生",
                "departmentId", 1,
                "departmentName", "心内科",
                "appointmentTime", "2026-06-23 09:30:00",
                "remark", "初诊"
        );

        Map response = restTemplate.postForObject("/appointments", request, Map.class);
        Map data = (Map) response.get("data");

        assertThat(response).containsEntry("code", 0);
        assertThat((String) data.get("appointmentNo")).startsWith("APPT");
        assertThat(data).containsEntry("status", "pending");
        assertThat(data).containsEntry("patientName", "测试患者");
        assertThat(data).containsEntry("doctorName", "王医生");
    }

    @Test
    void listAndDetailReturnCreatedAppointment() {
        Map<String, Object> request = Map.of(
                "patientId", 2001,
                "patientName", "列表患者",
                "doctorId", 2,
                "doctorName", "李医生",
                "departmentId", 2,
                "departmentName", "儿科",
                "appointmentTime", "2026-06-24 10:00:00"
        );

        Map createResponse = restTemplate.postForObject("/appointments", request, Map.class);
        Map<?, ?> created = (Map<?, ?>) createResponse.get("data");
        Number id = (Number) created.get("id");

        Map listResponse = restTemplate.getForObject("/appointments?patientId=2001", Map.class);
        List<?> rows = (List<?>) ((Map<?, ?>) listResponse.get("data")).get("rows");

        Map detailResponse = restTemplate.getForObject("/appointments/" + id.longValue(), Map.class);
        Map detail = (Map) detailResponse.get("data");

        assertThat(listResponse).containsEntry("code", 0);
        assertThat(rows).isNotEmpty();
        assertThat(detailResponse).containsEntry("code", 0);
        assertThat(detail).containsEntry("patientName", "列表患者");
        assertThat(detail).containsEntry("doctorName", "李医生");
    }

    @Test
    void cancelAppointmentUpdatesStatusAndReason() {
        Map<String, Object> request = Map.of(
                "patientId", 3001,
                "patientName", "取消患者",
                "doctorId", 1,
                "doctorName", "王医生",
                "departmentId", 1,
                "departmentName", "心内科",
                "appointmentTime", "2026-06-25 11:00:00"
        );
        Map createResponse = restTemplate.postForObject("/appointments", request, Map.class);
        Number id = (Number) ((Map<?, ?>) createResponse.get("data")).get("id");

        Map cancelResponse = restTemplate.postForObject(
                "/appointments/" + id.longValue() + "/cancel",
                Map.of("cancelReason", "时间冲突"),
                Map.class
        );
        Map cancelled = (Map) cancelResponse.get("data");

        assertThat(cancelResponse).containsEntry("code", 0);
        assertThat(cancelled).containsEntry("status", "cancelled");
        assertThat(cancelled).containsEntry("cancelReason", "时间冲突");
    }
}
