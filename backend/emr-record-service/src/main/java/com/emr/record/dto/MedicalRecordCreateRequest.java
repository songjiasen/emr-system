package com.emr.record.dto;

/**
 * 新增病历请求。
 * 字段对齐 binglixinxi 表，appointmentId/appointmentNo 用于串起预约到病历的主流程。
 */
public record MedicalRecordCreateRequest(
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
        String fileUrl
) {
}

