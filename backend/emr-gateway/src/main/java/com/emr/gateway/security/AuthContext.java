package com.emr.gateway.security;

/**
 * 网关认证上下文。
 * 字段来自认证服务的 Token 校验结果，只允许网关生成并向下游透传。
 */
public record AuthContext(Long userId, String username, String roleCode, String tableName) {
}
