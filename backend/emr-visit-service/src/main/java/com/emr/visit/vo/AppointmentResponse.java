package com.emr.visit.vo;

/**
 * 预约挂号响应数据。
 * 既服务患者端“我的预约”，也服务后台端预约管理列表。
 */
public record AppointmentResponse(
        Long id,
        String appointmentNo,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        Long departmentId,
        String departmentName,
        String appointmentTime,
        String status,
        String cancelReason,
        String remark
) {
}

