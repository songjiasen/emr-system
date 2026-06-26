package com.emr.workflow.service.impl;

import com.emr.workflow.service.BusinessAuditCallbackClient;
import com.emr.workflow.service.BusinessCallbackException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 HTTP 的业务状态回写客户端。
 * 保持微服务边界清晰，通过下游公开接口同步审核结果，而不是直接跨库修改业务表。
 */
@Component
public class HttpBusinessAuditCallbackClient implements BusinessAuditCallbackClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String clinicalBaseUrl;
    private final String recordBaseUrl;

    public HttpBusinessAuditCallbackClient(
            @Value("${EMR_CLINICAL_BASE_URL:http://localhost:8105}") String clinicalBaseUrl,
            @Value("${EMR_RECORD_BASE_URL:http://localhost:8104}") String recordBaseUrl
    ) {
        this.clinicalBaseUrl = clinicalBaseUrl;
        this.recordBaseUrl = recordBaseUrl;
    }

    /**
     * 回写审核结果。
     * `medical_record` 目前只有工作流留痕，不做下游状态回写；其余已接入类型按约定路径发 HTTP 请求。
     */
    @Override
    public void syncAuditResult(String businessType, Long businessId, Long auditorId, String auditorName, String auditResult, String auditOpinion) {
        String path = resolvePath(businessType, businessId);
        if (path == null) {
            return;
        }
        Map<String, Object> payload = buildPayload(businessType, auditorId, auditorName, auditResult, auditOpinion);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.postForEntity(path, new HttpEntity<>(payload, headers), String.class);
        } catch (RestClientException exception) {
            throw new BusinessCallbackException("业务状态回写失败", exception);
        }
    }

    private String resolvePath(String businessType, Long businessId) {
        return switch (businessType) {
            case "medical_order" -> clinicalBaseUrl + "/medical-orders/" + businessId + "/audit-result";
            case "test_request" -> clinicalBaseUrl + "/test-requests/" + businessId + "/audit-result";
            case "prescription" -> clinicalBaseUrl + "/prescriptions/" + businessId + "/audit-result";
            case "record_archive" -> recordBaseUrl + "/medical-record-archives/applications/" + businessId + "/audit";
            case "medical_record" -> null;
            default -> throw new IllegalArgumentException("暂不支持的业务类型");
        };
    }

    private Map<String, Object> buildPayload(String businessType, Long auditorId, String auditorName, String auditResult, String auditOpinion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("auditResult", auditResult);
        payload.put("auditOpinion", auditOpinion);
        if ("record_archive".equals(businessType)) {
            payload.put("auditUserId", auditorId);
            payload.put("auditUserName", auditorName);
        }
        return payload;
    }
}
