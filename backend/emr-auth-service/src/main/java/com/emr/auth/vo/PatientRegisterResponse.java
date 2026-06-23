package com.emr.auth.vo;

/**
 * 患者注册响应数据。
 * 注册成功后固定返回 patient 角色，便于前端直接进入患者端流程。
 */
public record PatientRegisterResponse(
        Long patientId,
        String username,
        String name,
        String gender,
        String phone,
        String roleCode
) {
}

