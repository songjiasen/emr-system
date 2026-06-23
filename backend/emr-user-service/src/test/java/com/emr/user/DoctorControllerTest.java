package com.emr.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DoctorControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void doctorListReturnsBasicDoctorFields() {
        Map response = restTemplate.getForObject("/doctors", Map.class);
        Map data = (Map) response.get("data");
        List<?> rows = (List<?>) data.get("rows");
        Map first = (Map) rows.get(0);

        assertThat(response).containsEntry("code", 0);
        assertThat(rows).isNotEmpty();
        assertThat(first).containsKeys("id", "name", "departmentName", "specialty");
    }

    @Test
    void doctorDetailReturnsDoctorProfile() {
        Map response = restTemplate.getForObject("/doctors/1", Map.class);
        Map data = (Map) response.get("data");

        assertThat(response).containsEntry("code", 0);
        assertThat(((Number) data.get("id")).longValue()).isEqualTo(1L);
        assertThat(data).containsKey("profile");
    }
}
