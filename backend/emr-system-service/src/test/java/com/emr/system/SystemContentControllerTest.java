package com.emr.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SystemContentControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createNewsAndMessageReply() {
        Map news = restTemplate.postForObject("/news", Map.of("title", "健康知识", "content", "按时复诊"), Map.class);
        Map message = restTemplate.postForObject("/messages", Map.of("userId", 1001, "username", "patient", "content", "如何预约复诊"), Map.class);
        Number messageId = (Number) ((Map<?, ?>) message.get("data")).get("id");
        Map reply = restTemplate.postForObject("/messages/" + messageId.longValue() + "/reply", Map.of("replyContent", "请在医生列表选择医生预约"), Map.class);
        Map replyData = (Map) reply.get("data");

        assertThat(news).containsEntry("code", 0);
        assertThat(message).containsEntry("code", 0);
        assertThat(reply).containsEntry("code", 0);
        assertThat(replyData).containsEntry("replyContent", "请在医生列表选择医生预约");
    }

    @Test
    void createAndDisableCarousel() {
        Map carousel = restTemplate.postForObject("/carousels", Map.of(
                "title", "医院公告",
                "imageUrl", "/uploads/banner.png",
                "linkUrl", "/news/1"
        ), Map.class);
        Number carouselId = (Number) ((Map<?, ?>) carousel.get("data")).get("id");

        restTemplate.delete("/carousels/" + carouselId.longValue());
        Map list = restTemplate.getForObject("/carousels", Map.class);
        Map carouselData = (Map) carousel.get("data");

        assertThat(carousel).containsEntry("code", 0);
        assertThat(carouselData).containsEntry("title", "医院公告");
        assertThat(list).containsEntry("code", 0);
    }

    @Test
    void createSyslogPersistsAuditFields() {
        Map created = restTemplate.postForObject("/syslogs", Map.of(
                "userId", 1,
                "username", "admin",
                "roleCode", "admin",
                "operation", "POST /cl584734139/news",
                "requestUri", "/cl584734139/news?category=notice",
                "requestMethod", "POST",
                "requestParams", "category=notice",
                "ipAddress", "127.0.0.1",
                "costMillis", 28
        ), Map.class);
        Map createdData = (Map) created.get("data");
        Number logId = (Number) createdData.get("id");
        Map list = restTemplate.getForObject("/syslogs", Map.class);
        Map pageData = (Map) list.get("data");
        java.util.List rows = (java.util.List) pageData.get("rows");
        Map firstRow = null;
        for (Object item : rows) {
            if (!(item instanceof Map row)) {
                continue;
            }
            if (logId.equals(row.get("id"))) {
                firstRow = row;
                break;
            }
        }

        assertThat(created).containsEntry("code", 0);
        assertThat(createdData).containsEntry("roleCode", "admin");
        assertThat(createdData).containsEntry("requestMethod", "POST");
        assertThat(createdData).containsEntry("requestParams", "category=notice");
        assertThat(createdData).containsEntry("ipAddress", "127.0.0.1");
        assertThat(createdData).containsEntry("costMillis", 28);
        assertThat(createdData).containsKey("createdAt");
        assertThat(firstRow).isNotNull();
        assertThat(firstRow).containsEntry("operation", "POST /cl584734139/news");
    }

    @Test
    void patientCannotReplyMessageDirectly() {
        Map message = restTemplate.postForObject("/messages", Map.of(
                "userId", 2001,
                "username", "patient_demo",
                "content", "我想咨询药物"
        ), Map.class);
        Number messageId = (Number) ((Map<?, ?>) message.get("data")).get("id");

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "2001");
        headers.add("X-Username", "patient_demo");
        headers.add("X-Role-Code", "patient");
        headers.add("X-User-Table", "huanzhe");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/messages/" + messageId.longValue() + "/reply",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("replyContent", "患者自己回复"), headers),
                Map.class
        );

        assertThat(response.getBody()).containsEntry("code", 400);
        assertThat(response.getBody()).containsEntry("message", "当前登录角色无权回复留言");
    }
}
