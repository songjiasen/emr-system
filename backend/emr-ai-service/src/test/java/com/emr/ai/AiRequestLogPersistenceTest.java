package com.emr.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiRequestLogPersistenceTest {

    private static final RecordStubServer RECORD_SERVER = new RecordStubServer();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("EMR_RECORD_BASE_URL", RECORD_SERVER::baseUrl);
    }

    @BeforeEach
    void resetRecordServer() {
        RECORD_SERVER.reset();
        jdbcTemplate.update("delete from ai_request_log");
    }

    @AfterAll
    static void shutdownServers() {
        RECORD_SERVER.close();
    }

    @Test
    void allAiEndpointsPersistSuccessLogs() {
        restTemplate.postForObject("/ai/ocr", Map.of("fileUrl", "/uploads/ocr-demo.png", "userId", 1, "username", "doctor_a"), Map.class);
        restTemplate.postForObject("/ai/recommend-medicine", Map.of("diagnosis", "上呼吸道感染", "userId", 2, "username", "doctor_b"), Map.class);
        restTemplate.postForObject("/ai/prescription-audit", Map.of("prescriptionText", "阿莫西林,维生素C", "userId", 3, "username", "director_a"), Map.class);
        restTemplate.postForObject("/ai/smart-search", Map.of("keyword", "感染", "userId", 4, "username", "doctor_c"), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from ai_request_log where status = 'success'",
                Integer.class
        );
        Integer smartSearchCount = jdbcTemplate.queryForObject(
                "select count(*) from ai_request_log where ai_type = 'smart_search' and provider = 'mock'",
                Integer.class
        );

        assertThat(rowCount).isEqualTo(4);
        assertThat(smartSearchCount).isEqualTo(1);
    }

    @Test
    void validationFailurePersistsFailedLog() {
        Map response = restTemplate.postForObject("/ai/prescription-audit", Map.of("prescriptionText", ""), Map.class);

        Integer failedCount = jdbcTemplate.queryForObject(
                "select count(*) from ai_request_log where ai_type = 'prescription_audit' and status = 'success'",
                Integer.class
        );

        assertThat(response).containsEntry("code", 0);
        Map data = (Map) response.get("data");
        assertThat(data).containsEntry("passed", false);
        assertThat(failedCount).isEqualTo(1);
    }

    @Test
    void smartSearchFallsBackToEmptyResultsAndWritesFailedLogWhenRecordServiceUnavailable() {
        RECORD_SERVER.respondWithStatus(500);

        Map response = restTemplate.postForObject("/ai/smart-search", Map.of("keyword", "心电图"), Map.class);
        Map data = (Map) response.get("data");

        Integer failedCount = jdbcTemplate.queryForObject(
                "select count(*) from ai_request_log where ai_type = 'smart_search' and status = 'failed'",
                Integer.class
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(data.get("results")).isEqualTo(java.util.List.of());
        assertThat(failedCount).isEqualTo(1);
    }

    /**
     * 用 JDK 内置 HTTP Server 模拟病历服务。
     * 这样智能检索测试既能验证远程调用，也不用额外引第三方测试服务器依赖。
     */
    private static final class RecordStubServer {

        private final HttpServer server;
        private volatile int status = 200;

        private RecordStubServer() {
            try {
                this.server = HttpServer.create(new InetSocketAddress(0), 0);
            } catch (IOException exception) {
                throw new IllegalStateException("创建病历测试服务失败", exception);
            }
            this.server.createContext("/medical-records", this::handleMedicalRecords);
            this.server.start();
        }

        private void handleMedicalRecords(HttpExchange exchange) throws IOException {
            String body = """
                    {"code":0,"data":{"rows":[
                      {"recordNo":"MR-DEMO-001","patientName":"检索患者","diagnosis":"上呼吸道感染","chiefComplaint":"发热咳嗽","treatmentAdvice":"多喝水"},
                      {"recordNo":"MR-DEMO-002","patientName":"普通患者","diagnosis":"胃肠炎","chiefComplaint":"腹痛","treatmentAdvice":"清淡饮食"}
                    ],"total":2,"page":1,"limit":100}}
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private void reset() {
            this.status = 200;
        }

        private void respondWithStatus(int status) {
            this.status = status;
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void close() {
            this.server.stop(0);
        }
    }
}
