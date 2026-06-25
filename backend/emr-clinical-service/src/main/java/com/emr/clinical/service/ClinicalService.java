package com.emr.clinical.service;

import com.emr.common.PageResult;

import java.util.Map;

/**
 * 诊疗主流程服务。
 * 承接医嘱、执行记录、处方和检查申请的数据库持久化与状态流校验。
 */
public interface ClinicalService {
    Map<String, Object> createMedicalOrder(Map<String, Object> request);
    PageResult<Map<String, Object>> listMedicalOrders(Long patientId, Long doctorId, String status, Long recordId, int page, int limit);
    Map<String, Object> getMedicalOrder(Long id);
    Map<String, Object> updateMedicalOrder(Long id, Map<String, Object> request);
    Map<String, Object> deleteMedicalOrder(Long id);
    Map<String, Object> updateMedicalOrderAuditResult(Long id, Map<String, Object> request);
    Map<String, Object> executeMedicalOrder(Long id, Map<String, Object> request);
    PageResult<Map<String, Object>> listExecutions(Long orderId, int page, int limit);
    Map<String, Object> createPrescription(Map<String, Object> request);
    PageResult<Map<String, Object>> listPrescriptions(Long patientId, Long doctorId, String status, Long recordId, int page, int limit);
    Map<String, Object> getPrescription(Long id);
    Map<String, Object> updatePrescription(Long id, Map<String, Object> request);
    Map<String, Object> deletePrescription(Long id);
    Map<String, Object> createTestRequest(Map<String, Object> request);
    PageResult<Map<String, Object>> listTestRequests(Long patientId, Long doctorId, String status, Long recordId, int page, int limit);
    Map<String, Object> getTestRequest(Long id);
    Map<String, Object> updateTestRequest(Long id, Map<String, Object> request);
    Map<String, Object> deleteTestRequest(Long id);
    Map<String, Object> updateTestAuditResult(Long id, Map<String, Object> request);
    Map<String, Object> payTestRequest(Long id);
}
