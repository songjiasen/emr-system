package com.emr.record.vo;

/**
 * 病历响应数据。
 * 同时服务医生端病历管理和患者端我的病历详情。
 */
public record MedicalRecordResponse(
        Long id,
        String recordNo,
        Long appointmentId,
        String appointmentNo,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        String visitTime,
        String chiefComplaint,
        String presentIllness,
        String pastHistory,
        String diagnosis,
        String treatmentAdvice,
        String fileUrl,
        String archiveStatus
) {
}

