package com.emr.auth.vo;

/**
 * 登录响应数据。
 * token 是后续前端访问网关和业务服务时携带的认证凭证。
 */
public record LoginResponse(
        Long userId,
        String username,
        String roleCode,
        String tableName,
        Long departmentId,
        String departmentName,
        String token,
        String expireAt
) {
}
