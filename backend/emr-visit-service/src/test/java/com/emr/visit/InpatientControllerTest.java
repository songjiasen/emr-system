package com.emr.visit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InpatientControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void admissionAndDischargeFlow() {
        Map admission = restTemplate.postForObject("/inpatients/admissions", Map.of(
                "patientId", 1001,
                "patientName", "测试患者",
                "wardNo", "A01",
                "bedNo", "01",
                "admissionTime", "2026-06-23 12:00:00"
        ), Map.class);
        Number admissionId = (Number) ((Map<?, ?>) admission.get("data")).get("id");

        Map discharge = restTemplate.postForObject("/inpatients/admissions/" + admissionId.longValue() + "/discharge", Map.of(
                "dischargeTime", "2026-06-25 10:00:00",
                "dischargeReason", "治疗结束"
        ), Map.class);
        Map admissionData = (Map) admission.get("data");
        Map dischargeData = (Map) discharge.get("data");

        assertThat(admission).containsEntry("code", 0);
        assertThat(admissionData).containsEntry("status", "in_hospital");
        assertThat(discharge).containsEntry("code", 0);
        assertThat(dischargeData).containsEntry("status", "discharged");
    }
}
