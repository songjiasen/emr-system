package com.emr.gateway.security;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志事件。
 * 由网关在受保护写操作成功后生成，统一携带用户上下文和请求元数据，避免业务服务重复拼装审计字段。
 */
public record OperationAuditEvent(
        Long userId,
        String username,
        String roleCode,
        String operation,
        String requestUri,
        String requestMethod,
        String requestParams,
        String ipAddress,
        Long costMillis
) {

    /**
     * 转换为系统服务日志接口可直接接收的请求体。
     * 这里显式构造字段，保证网关与系统服务之间的审计字段口径一致。
     */
    public Map<String, Object> toRequestBody() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("roleCode", roleCode);
        payload.put("operation", operation);
        payload.put("requestUri", requestUri);
        payload.put("requestMethod", requestMethod);
        payload.put("requestParams", requestParams);
        payload.put("ipAddress", ipAddress);
        payload.put("costMillis", costMillis);
        return payload;
    }
}
