package com.emr.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DepartmentControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createAndListDepartment() {
        Map response = restTemplate.postForObject("/departments", Map.of("name", "急诊科"), Map.class);
        Map listResponse = restTemplate.getForObject("/departments", Map.class);
        Map data = (Map) response.get("data");
        Map listData = (Map) listResponse.get("data");
        List<?> rows = (List<?>) listData.get("rows");

        assertThat(response).containsEntry("code", 0);
        assertThat(data).containsEntry("name", "急诊科");
        assertThat(rows).isNotEmpty();
    }
}
