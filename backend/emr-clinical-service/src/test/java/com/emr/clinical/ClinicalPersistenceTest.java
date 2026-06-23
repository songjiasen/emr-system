package com.emr.clinical;

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
class ClinicalPersistenceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createMedicalOrderPersistsToOrderTable() {
        Map response = restTemplate.postForObject("/medical-orders", Map.of(
                "recordId", 9101,
                "patientId", 9201,
                "patientName", "医嘱患者",
                "doctorId", 9301,
                "doctorName", "开单医生",
                "content", "每日输液一次",
                "orderType", "long_term"
        ), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from yizhuxinxi where patient_id = ? and doctor_id = ? and status = ?",
                Integer.class,
                9201L,
                9301L,
                "pending_audit"
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void auditAndExecuteMedicalOrderPersistStatusAndExecutionRecord() {
        jdbcTemplate.update(
                "insert into yizhuxinxi (order_no, record_id, patient_id, patient_name, doctor_id, doctor_name, order_type, content, status) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "ORDER202606220001",
                9102L,
                9202L,
                "执行患者",
                9302L,
                "医嘱医生",
                "temporary",
                "一次性注射",
                "pending_audit"
        );
        Long orderId = jdbcTemplate.queryForObject(
                "select id from yizhuxinxi where order_no = ?",
                Long.class,
                "ORDER202606220001"
        );

        Map auditResponse = restTemplate.postForObject("/medical-orders/" + orderId + "/audit-result", Map.of(
                "auditResult", "approved",
                "auditOpinion", "可以执行"
        ), Map.class);
        Map executeResponse = restTemplate.postForObject("/medical-orders/" + orderId + "/execute", Map.of(
                "nurseId", 9401,
                "nurseName", "执行护士",
                "executionResult", "执行完成",
                "remark", "已回访"
        ), Map.class);

        Integer executionCount = jdbcTemplate.queryForObject(
                "select count(*) from yizhuzhixingjilu where order_id = ? and nurse_id = ?",
                Integer.class,
                orderId,
                9401L
        );
        String orderStatus = jdbcTemplate.queryForObject(
                "select status from yizhuxinxi where id = ?",
                String.class,
                orderId
        );

        assertThat(auditResponse).containsEntry("code", 0);
        assertThat(executeResponse).containsEntry("code", 0);
        assertThat(executionCount).isEqualTo(1);
        assertThat(orderStatus).isEqualTo("executed");
    }

    @Test
    void prescriptionDeleteSoftCancelsStatus() {
        jdbcTemplate.update(
                "insert into kaifang (prescription_no, record_id, patient_id, patient_name, doctor_id, doctor_name, medicine_name, quantity, usage_text, status) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "PRES202606220001",
                9103L,
                9203L,
                "处方患者",
                9303L,
                "处方医生",
                "阿莫西林",
                "2盒",
                "每日两次",
                "created"
        );
        Long prescriptionId = jdbcTemplate.queryForObject(
                "select id from kaifang where prescription_no = ?",
                Long.class,
                "PRES202606220001"
        );

        Map response = restTemplate.exchange(
                "/prescriptions/" + prescriptionId,
                org.springframework.http.HttpMethod.DELETE,
                org.springframework.http.HttpEntity.EMPTY,
                Map.class
        ).getBody();

        String status = jdbcTemplate.queryForObject(
                "select status from kaifang where id = ?",
                String.class,
                prescriptionId
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(status).isEqualTo("cancelled");
    }

    @Test
    void testRequestAuditAndFinishPersistStatus() {
        Map createResponse = restTemplate.postForObject("/test-requests", Map.of(
                "recordId", 9104,
                "patientId", 9204,
                "patientName", "检查患者",
                "doctorId", 9304,
                "doctorName", "检查医生",
                "testItem", "血常规",
                "testReason", "复查"
        ), Map.class);
        Number testId = (Number) ((Map<?, ?>) createResponse.get("data")).get("id");

        Map auditResponse = restTemplate.postForObject("/test-requests/" + testId.longValue() + "/audit-result", Map.of(
                "auditResult", "approved",
                "auditOpinion", "同意检查"
        ), Map.class);
        restTemplate.put("/test-requests/" + testId.longValue(), Map.of(
                "resultContent", "检查结果正常"
        ));

        Map detailResponse = restTemplate.getForObject("/test-requests/" + testId.longValue(), Map.class);

        String status = jdbcTemplate.queryForObject(
                "select status from jianchaxiang where id = ?",
                String.class,
                testId.longValue()
        );
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select result_content from jianchaxiang where id = ?",
                testId.longValue()
        );

        assertThat(auditResponse).containsEntry("code", 0);
        assertThat(detailResponse).containsEntry("code", 0);
        assertThat(status).isEqualTo("finished");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("result_content")).isEqualTo("检查结果正常");
    }

    @Test
    void nurseCanExecuteMedicalOrderWithTrustedHeadersFallback() {
        jdbcTemplate.update(
                "insert into yizhuxinxi (order_no, record_id, patient_id, patient_name, doctor_id, doctor_name, order_type, content, status) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "ORDER202606220099",
                9901L,
                9902L,
                "头信息执行患者",
                9903L,
                "头信息医生",
                "temporary",
                "头信息执行医嘱",
                "approved"
        );
        Long orderId = jdbcTemplate.queryForObject(
                "select id from yizhuxinxi where order_no = ?",
                Long.class,
                "ORDER202606220099"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "9904");
        headers.add("X-Username", "nurse_demo");
        headers.add("X-Role-Code", "nurse");
        headers.add("X-User-Table", "users");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/medical-orders/" + orderId + "/execute",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("executionResult", "头信息执行完成"), headers),
                Map.class
        );

        Map row = jdbcTemplate.queryForMap(
                "select nurse_id, nurse_name, execution_result from yizhuzhixingjilu where order_id = ?",
                orderId
        );

        assertThat(response.getBody()).containsEntry("code", 0);
        assertThat(row).containsEntry("nurse_id", 9904L);
        assertThat(row).containsEntry("nurse_name", "nurse_demo");
        assertThat(row).containsEntry("execution_result", "头信息执行完成");
    }
}
