package com.emr.billing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BillingPersistenceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createFeePersistsToBillingTable() {
        Map response = restTemplate.postForObject("/fees", Map.of(
                "patientId", 8101,
                "patientName", "收费患者",
                "feeItem", "检验费",
                "amount", 58.50,
                "remark", "门诊收费"
        ), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from feiyong where patient_id = ? and fee_item = ? and amount = ? and pay_status = ?",
                Integer.class,
                8101L,
                "检验费",
                new BigDecimal("58.50"),
                "unpaid"
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void listFeesReadsPersistedRows() {
        jdbcTemplate.update(
                "insert into feiyong (fee_no, patient_id, patient_name, business_type, business_id, fee_item, amount, pay_status) values (?, ?, ?, ?, ?, ?, ?, ?)",
                "FEE202606220001",
                8201L,
                "列表患者",
                "manual",
                1L,
                "床位费",
                new BigDecimal("120.00"),
                "unpaid"
        );

        Map response = restTemplate.getForObject("/fees?patientId=8201&page=1&limit=10", Map.class);
        List<?> rows = (List<?>) ((Map<?, ?>) response.get("data")).get("rows");

        assertThat(response).containsEntry("code", 0);
        assertThat(rows).anySatisfy(item -> {
            Map row = (Map) item;
            assertThat(row).containsEntry("patientName", "列表患者");
            assertThat(row).containsEntry("payStatus", "unpaid");
        });
    }

    @Test
    void payFeeUpdatesStatusAndPayTime() {
        jdbcTemplate.update(
                "insert into feiyong (fee_no, patient_id, patient_name, fee_item, amount, pay_status) values (?, ?, ?, ?, ?, ?)",
                "FEE202606220002",
                8301L,
                "支付患者",
                "药费",
                new BigDecimal("88.00"),
                "unpaid"
        );
        Long feeId = jdbcTemplate.queryForObject(
                "select id from feiyong where fee_no = ?",
                Long.class,
                "FEE202606220002"
        );

        Map response = restTemplate.postForObject("/fees/" + feeId + "/pay", Map.of(), Map.class);

        String payStatus = jdbcTemplate.queryForObject(
                "select pay_status from feiyong where id = ?",
                String.class,
                feeId
        );
        Object payTime = jdbcTemplate.queryForObject(
                "select pay_time from feiyong where id = ?",
                Object.class,
                feeId
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(payStatus).isEqualTo("paid");
        assertThat(payTime).isNotNull();
    }

    @Test
    void paidFeeCannotBeUpdatedOrDeleted() {
        jdbcTemplate.update(
                "insert into feiyong (fee_no, patient_id, patient_name, fee_item, amount, pay_status, pay_time) values (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP())",
                "FEE202606220003",
                8401L,
                "已支付患者",
                "挂号费",
                new BigDecimal("20.00"),
                "paid"
        );
        Long feeId = jdbcTemplate.queryForObject(
                "select id from feiyong where fee_no = ?",
                Long.class,
                "FEE202606220003"
        );

        Map updatedResponse = restTemplate.getForObject("/fees/" + feeId, Map.class);
        Map deleteResponse = restTemplate.getForObject("/fees/" + feeId, Map.class);
        ResponseEntity<Map> updateAttempt = restTemplate.exchange(
                "/fees/" + feeId,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("amount", 25)),
                Map.class
        );
        ResponseEntity<Map> deleteAttempt = restTemplate.exchange(
                "/fees/" + feeId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Map.class
        );

        assertThat(updateAttempt.getBody()).containsEntry("code", 400);
        assertThat(updateAttempt.getBody()).containsEntry("message", "已支付或已退费费用不允许修改");
        assertThat(deleteAttempt.getBody()).containsEntry("code", 400);
        assertThat(deleteAttempt.getBody()).containsEntry("message", "已支付或已退费费用不允许删除");
        assertThat(updatedResponse).containsEntry("code", 0);
        assertThat(deleteResponse).containsEntry("code", 0);
    }

    @Test
    void patientCannotQueryOrPayOtherPatientsFee() {
        jdbcTemplate.update(
                "insert into feiyong (fee_no, patient_id, patient_name, fee_item, amount, pay_status) values (?, ?, ?, ?, ?, ?)",
                "FEE202606220099",
                8502L,
                "他人费用患者",
                "治疗费",
                new BigDecimal("66.00"),
                "unpaid"
        );
        Long feeId = jdbcTemplate.queryForObject(
                "select id from feiyong where fee_no = ?",
                Long.class,
                "FEE202606220099"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "8501");
        headers.add("X-Username", "patient_demo");
        headers.add("X-Role-Code", "patient");
        headers.add("X-User-Table", "huanzhe");

        ResponseEntity<Map> listResponse = restTemplate.exchange(
                "/fees?patientId=8502",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        ResponseEntity<Map> payResponse = restTemplate.exchange(
                "/fees/" + feeId + "/pay",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                Map.class
        );

        assertThat(listResponse.getBody()).containsEntry("code", 400);
        assertThat(listResponse.getBody()).containsEntry("message", "当前登录患者只能查询自己的费用");
        assertThat(payResponse.getBody()).containsEntry("code", 400);
        assertThat(payResponse.getBody()).containsEntry("message", "当前登录患者无权支付该费用");
    }
}
