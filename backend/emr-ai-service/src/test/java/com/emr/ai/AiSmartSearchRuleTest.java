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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiSmartSearchRuleTest {

    private static final RecordStubServer RECORD_SERVER = new RecordStubServer();

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("EMR_RECORD_BASE_URL", RECORD_SERVER::baseUrl);
    }

    @BeforeEach
    void resetRecordServer() {
        RECORD_SERVER.reset();
    }

    @AfterAll
    static void shutdownServers() {
        RECORD_SERVER.close();
    }

    @Test
    void ordinaryPrescriptionPassesAudit() {
        Map response = restTemplate.postForObject("/ai/prescription-audit", Map.of(
                "prescriptionText", "阿莫西林,维生素C"
        ), Map.class);
        Map data = (Map) response.get("data");

        assertThat(response).containsEntry("code", 0);
        assertThat(data).containsEntry("passed", true);
        assertThat((List<?>) data.get("warnings")).isEmpty();
    }

    @Test
    void duplicateMedicinePrescriptionFailsAudit() {
        Map response = restTemplate.postForObject("/ai/prescription-audit", Map.of(
                "prescriptionText", "阿莫西林,阿莫西林"
        ), Map.class);
        Map data = (Map) response.get("data");

        assertThat(response).containsEntry("code", 0);
        assertThat(data).containsEntry("passed", false);
        assertThat((List<?>) data.get("warnings")).isNotEmpty();
    }

    @Test
    void smartSearchReturnsRecordServiceMatches() {
        Map response = restTemplate.postForObject("/ai/smart-search", Map.of(
                "keyword", "感染"
        ), Map.class);
        Map data = (Map) response.get("data");
        List<?> results = (List<?>) data.get("results");

        assertThat(response).containsEntry("code", 0);
        assertThat(results).hasSize(1);
        Map first = (Map) results.get(0);
        assertThat(first).containsEntry("recordNo", "MR-DEMO-001");
        assertThat(first).containsEntry("patientName", "检索患者");
    }

    private static final class RecordStubServer {

        private final HttpServer server;
        private volatile String body = """
                {"code":0,"data":{"rows":[
                  {"recordNo":"MR-DEMO-001","patientName":"检索患者","diagnosis":"上呼吸道感染","chiefComplaint":"发热咳嗽","treatmentAdvice":"多喝水"},
                  {"recordNo":"MR-DEMO-002","patientName":"普通患者","diagnosis":"胃肠炎","chiefComplaint":"腹痛","treatmentAdvice":"清淡饮食"}
                ],"total":2,"page":1,"limit":100}}
                """;

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
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private void reset() {
            this.body = """
                    {"code":0,"data":{"rows":[
                      {"recordNo":"MR-DEMO-001","patientName":"检索患者","diagnosis":"上呼吸道感染","chiefComplaint":"发热咳嗽","treatmentAdvice":"多喝水"},
                      {"recordNo":"MR-DEMO-002","patientName":"普通患者","diagnosis":"胃肠炎","chiefComplaint":"腹痛","treatmentAdvice":"清淡饮食"}
                    ],"total":2,"page":1,"limit":100}}
                    """;
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void close() {
            this.server.stop(0);
        }
    }
}
