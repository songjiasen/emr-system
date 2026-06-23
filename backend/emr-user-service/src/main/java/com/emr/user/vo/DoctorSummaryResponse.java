package com.emr.user.vo;

/**
 * 医生列表和详情响应数据。
 * 包含预约挂号页面必须展示的科室、擅长领域和个人简介。
 */
public record DoctorSummaryResponse(
        Long id,
        String username,
        String name,
        String gender,
        String phone,
        Long departmentId,
        String departmentName,
        String specialty,
        String profile
) {
}

