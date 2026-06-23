package com.emr.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void mockAiEndpointsReturnStructuredResults() {
        Map ocr = restTemplate.postForObject("/ai/ocr", Map.of("fileUrl", "/uploads/report.png"), Map.class);
        Map recommend = restTemplate.postForObject("/ai/recommend-medicine", Map.of("diagnosis", "上呼吸道感染"), Map.class);
        Map audit = restTemplate.postForObject("/ai/prescription-audit", Map.of("prescriptionText", "阿莫西林"), Map.class);

        assertThat(ocr).containsEntry("code", 0);
        assertThat(recommend).containsEntry("code", 0);
        assertThat(audit).containsEntry("code", 0);
    }
}
