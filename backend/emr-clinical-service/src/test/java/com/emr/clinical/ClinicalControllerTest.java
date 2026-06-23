package com.emr.clinical;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClinicalControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createAndExecuteMedicalOrder() {
        Map order = restTemplate.postForObject("/medical-orders", Map.of(
                "patientId", 1001,
                "patientName", "测试患者",
                "doctorId", 1,
                "doctorName", "王医生",
                "content", "每日测血压"
        ), Map.class);
        Number orderId = (Number) ((Map<?, ?>) order.get("data")).get("id");
        Map orderData = (Map) order.get("data");

        Map audit = restTemplate.postForObject("/medical-orders/" + orderId.longValue() + "/audit-result", Map.of(
                "auditResult", "approved",
                "auditOpinion", "同意执行"
        ), Map.class);

        Map execution = restTemplate.postForObject("/medical-orders/" + orderId.longValue() + "/execute", Map.of(
                "nurseId", 10,
                "nurseName", "护士A",
                "executionResult", "已执行"
        ), Map.class);
        Map executionData = (Map) execution.get("data");

        assertThat(order).containsEntry("code", 0);
        assertThat(orderData).containsEntry("status", "pending_audit");
        assertThat(audit).containsEntry("code", 0);
        assertThat(execution).containsEntry("code", 0);
        assertThat(executionData).containsEntry("executionResult", "已执行");
    }
}
