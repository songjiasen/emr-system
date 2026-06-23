package com.emr.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SystemContentPersistenceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createNewsPersistsCategoryAndPublishStatus() {
        Map response = restTemplate.postForObject("/news", Map.of(
                "title", "慢病宣教",
                "category", "健康资讯",
                "content", "按时复诊",
                "publishStatus", "published"
        ), Map.class);
        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from news where title = ? and category = ? and publish_status = ?",
                Integer.class,
                "慢病宣教",
                "健康资讯",
                "published"
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void replyMessagePersistsReplyColumns() {
        Map created = restTemplate.postForObject("/messages", Map.of(
                "userId", 12,
                "username", "patient_demo",
                "roleCode", "patient",
                "title", "用药咨询",
                "content", "饭前还是饭后服用"
        ), Map.class);
        Number messageId = (Number) ((Map<?, ?>) created.get("data")).get("id");
        Map reply = restTemplate.postForObject("/messages/" + messageId.longValue() + "/reply", Map.of(
                "replyContent", "饭后服用",
                "replyUserId", 3,
                "replyUserName", "王医生"
        ), Map.class);
        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from messages where id = ? and title = ? and reply_content = ? and reply_user_name = ?",
                Integer.class,
                messageId.longValue(),
                "用药咨询",
                "饭后服用",
                "王医生"
        );

        assertThat(reply).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void deleteCarouselAndCreateSyslogPersistRows() {
        Map carousel = restTemplate.postForObject("/carousels", Map.of(
                "title", "首页轮播",
                "imageUrl", "/uploads/banner.png",
                "linkUrl", "/news/1",
                "sortNo", 1
        ), Map.class);
        Number carouselId = (Number) ((Map<?, ?>) carousel.get("data")).get("id");
        restTemplate.delete("/carousels/" + carouselId.longValue());
        Integer carouselCount = jdbcTemplate.queryForObject(
                "select count(*) from carousels where id = ? and status = 0",
                Integer.class,
                carouselId.longValue()
        );

        Map logResponse = restTemplate.postForObject("/syslogs", Map.of(
                "userId", 1,
                "username", "admin",
                "roleCode", "admin",
                "operation", "POST /cl584734139/news",
                "requestMethod", "POST",
                "requestUri", "/cl584734139/news",
                "requestParams", "category=notice",
                "ipAddress", "127.0.0.1",
                "costMillis", 18
        ), Map.class);
        Integer logCount = jdbcTemplate.queryForObject(
                "select count(*) from syslog where username = ? and request_method = ? and ip = ? and cost_millis = ?",
                Integer.class,
                "admin",
                "POST",
                "127.0.0.1",
                18
        );

        assertThat(logResponse).containsEntry("code", 0);
        assertThat(carouselCount).isEqualTo(1);
        assertThat(logCount).isEqualTo(1);
    }
}
