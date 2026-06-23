package com.emr.workflow;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkflowPersistenceTest {

    private static final CallbackStubServer CLINICAL_SERVER = new CallbackStubServer();
    private static final CallbackStubServer RECORD_SERVER = new CallbackStubServer();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("EMR_CLINICAL_BASE_URL", CLINICAL_SERVER::baseUrl);
        registry.add("EMR_RECORD_BASE_URL", RECORD_SERVER::baseUrl);
    }

    @BeforeEach
    void resetServers() {
        CLINICAL_SERVER.reset();
        RECORD_SERVER.reset();
    }

    @AfterAll
    static void shutdownServers() {
        CLINICAL_SERVER.close();
        RECORD_SERVER.close();
    }

    @Test
    void createTaskPersistsToWorkflowTable() {
        Map response = restTemplate.postForObject("/workflow/tasks", Map.of(
                "businessType", "medical_order",
                "businessId", 7101,
                "businessNo", "ORDER202606220001",
                "applicantId", 801,
                "applicantName", "申请医生",
                "assigneeRole", "director"
        ), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from shenherenwu where business_type = ? and business_id = ? and assignee_role = ? and status = ?",
                Integer.class,
                "medical_order",
                7101L,
                "director",
                "pending"
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void duplicatePendingTaskIsRejected() {
        jdbcTemplate.update(
                "insert into shenherenwu (task_no, business_type, business_id, business_no, applicant_id, applicant_name, assignee_role, status) values (?, ?, ?, ?, ?, ?, ?, ?)",
                "WF202606220001",
                "medical_order",
                7201L,
                "ORDER202606220002",
                901L,
                "已有申请人",
                "director",
                "pending"
        );

        Map response = restTemplate.postForObject("/workflow/tasks", Map.of(
                "businessType", "medical_order",
                "businessId", 7201,
                "applicantId", 902,
                "applicantName", "重复申请人",
                "assigneeRole", "director"
        ), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from shenherenwu where business_type = ? and business_id = ?",
                Integer.class,
                "medical_order",
                7201L
        );

        assertThat(response).containsEntry("code", 400);
        assertThat(response).containsEntry("message", "当前业务已有待审核任务");
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void auditTaskPersistsAuditRecordAndCallsBusinessService() {
        jdbcTemplate.update(
                "insert into shenherenwu (task_no, business_type, business_id, business_no, applicant_id, applicant_name, assignee_role, status) values (?, ?, ?, ?, ?, ?, ?, ?)",
                "WF202606220002",
                "medical_order",
                7301L,
                "ORDER202606220003",
                1001L,
                "医嘱申请人",
                "director",
                "pending"
        );
        Long taskId = jdbcTemplate.queryForObject(
                "select id from shenherenwu where task_no = ?",
                Long.class,
                "WF202606220002"
        );
        CLINICAL_SERVER.respondWithStatus(200);

        Map response = restTemplate.postForObject("/workflow/tasks/" + taskId + "/audit", Map.of(
                "auditorId", 1101,
                "auditorName", "主任甲",
                "auditResult", "approved",
                "auditOpinion", "审核通过"
        ), Map.class);

        Integer recordCount = jdbcTemplate.queryForObject(
                "select count(*) from shenhejilu where task_id = ? and audit_result = ?",
                Integer.class,
                taskId,
                "approved"
        );
        String taskStatus = jdbcTemplate.queryForObject(
                "select status from shenherenwu where id = ?",
                String.class,
                taskId
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(recordCount).isEqualTo(1);
        assertThat(taskStatus).isEqualTo("approved");
        assertThat(CLINICAL_SERVER.lastPath()).isEqualTo("/medical-orders/7301/audit-result");
        assertThat(CLINICAL_SERVER.lastBody()).contains("\"auditResult\":\"approved\"");
        assertThat(CLINICAL_SERVER.lastBody()).contains("\"auditOpinion\":\"审核通过\"");
    }

    @Test
    void auditTaskReturns502WhenBusinessCallbackFails() {
        jdbcTemplate.update(
                "insert into shenherenwu (task_no, business_type, business_id, applicant_id, applicant_name, assignee_role, status) values (?, ?, ?, ?, ?, ?, ?)",
                "WF202606220003",
                "medical_order",
                7401L,
                1201L,
                "失败申请人",
                "director",
                "pending"
        );
        Long taskId = jdbcTemplate.queryForObject(
                "select id from shenherenwu where task_no = ?",
                Long.class,
                "WF202606220003"
        );
        CLINICAL_SERVER.respondWithStatus(500);

        Map response = restTemplate.postForObject("/workflow/tasks/" + taskId + "/audit", Map.of(
                "auditorId", 1202,
                "auditorName", "主任乙",
                "auditResult", "rejected",
                "auditOpinion", "回写失败"
        ), Map.class);

        Integer recordCount = jdbcTemplate.queryForObject(
                "select count(*) from shenhejilu where task_id = ?",
                Integer.class,
                taskId
        );
        String taskStatus = jdbcTemplate.queryForObject(
                "select status from shenherenwu where id = ?",
                String.class,
                taskId
        );

        assertThat(response).containsEntry("code", 502);
        assertThat(response).containsEntry("message", "业务状态回写失败");
        assertThat(recordCount).isEqualTo(0);
        assertThat(taskStatus).isEqualTo("pending");
    }

    /**
     * 用 JDK 内置 HTTP Server 充当业务服务回写替身。
     * 这样测试无需额外依赖，也能校验真实请求路径和请求体。
     */
    private static final class CallbackStubServer {

        private final HttpServer server;
        private final AtomicInteger responseStatus = new AtomicInteger(200);
        private volatile String lastPath;
        private volatile String lastBody;

        private CallbackStubServer() {
            try {
                this.server = HttpServer.create(new InetSocketAddress(0), 0);
            } catch (IOException exception) {
                throw new IllegalStateException("创建回写测试服务失败", exception);
            }
            this.server.createContext("/", this::handle);
            this.server.start();
        }

        private void handle(HttpExchange exchange) throws IOException {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            this.lastPath = exchange.getRequestURI().getPath();
            this.lastBody = new String(requestBytes, StandardCharsets.UTF_8);
            byte[] responseBytes = "{\"code\":0}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus.get(), responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        }

        private void reset() {
            this.lastPath = null;
            this.lastBody = null;
            this.responseStatus.set(200);
        }

        private void respondWithStatus(int status) {
            this.responseStatus.set(status);
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private String lastPath() {
            return lastPath;
        }

        private String lastBody() {
            return lastBody;
        }

        private void close() {
            this.server.stop(0);
        }
    }
}
