package com.emr.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkflowControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createAndAuditTask() {
        Map task = restTemplate.postForObject("/workflow/tasks", Map.of(
                "businessType", "medical_record",
                "businessId", 1,
                "applicantId", 1,
                "applicantName", "王医生",
                "assigneeRole", "director"
        ), Map.class);
        Number taskId = (Number) ((Map<?, ?>) task.get("data")).get("id");

        Map audit = restTemplate.postForObject("/workflow/tasks/" + taskId.longValue() + "/audit", Map.of(
                "auditorId", 2,
                "auditorName", "主任A",
                "auditResult", "approved",
                "auditOpinion", "同意"
        ), Map.class);
        Map auditData = (Map) audit.get("data");

        assertThat(task).containsEntry("code", 0);
        assertThat(audit).containsEntry("code", 0);
        assertThat(auditData).containsEntry("status", "approved");
    }
}
