package com.emr.auth.dto;

/**
 * 登录请求参数。
 * roleCode 用于区分管理员、医生、患者、护士、主任等不同入口。
 */
public record LoginRequest(String username, String password, String roleCode) {
}

