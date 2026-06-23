package com.emr.auth.dto;

/**
 * 患者注册请求参数。
 * 对应需求文档中的患者自主注册信息，后续可继续补身份证号、地址等扩展字段。
 */
public record PatientRegisterRequest(
        String username,
        String password,
        String name,
        String gender,
        String phone
) {
}

