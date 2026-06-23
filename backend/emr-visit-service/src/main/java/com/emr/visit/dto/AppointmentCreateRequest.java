package com.emr.visit.dto;

/**
 * 预约挂号创建请求。
 * 字段对齐 yuyueguahao 表，当前先由前端传入医生和患者快照信息，后续可改为跨服务查询补全。
 */
public record AppointmentCreateRequest(
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        Long departmentId,
        String departmentName,
        String appointmentTime,
        String remark
) {
}

