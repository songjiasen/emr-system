package com.emr.billing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BillingControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createAndPayFee() {
        jdbcTemplate.update(
                "insert into fee_items (item_code, item_name, amount, item_category, enabled, sort_order) values (?, ?, ?, ?, ?, ?)",
                "controller_registration",
                "挂号费",
                new BigDecimal("20.00"),
                "门诊",
                true,
                1
        );

        Map fee = restTemplate.postForObject("/fees", Map.of(
                "patientId", 1001,
                "patientName", "测试患者",
                "feeItemCode", "controller_registration"
        ), Map.class);
        Number feeId = (Number) ((Map<?, ?>) fee.get("data")).get("id");
        Map feeData = (Map) fee.get("data");

        Map paid = restTemplate.postForObject("/fees/" + feeId.longValue() + "/pay", Map.of(), Map.class);
        Map paidData = (Map) paid.get("data");

        assertThat(fee).containsEntry("code", 0);
        assertThat(feeData).containsEntry("payStatus", "unpaid");
        assertThat(paid).containsEntry("code", 0);
        assertThat(paidData).containsEntry("payStatus", "paid");
    }
}
