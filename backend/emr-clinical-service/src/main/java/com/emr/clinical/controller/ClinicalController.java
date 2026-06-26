package com.emr.clinical.controller;

import com.emr.clinical.service.ClinicalService;
import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.common.TrustedUserContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 诊疗业务接口。
 * 覆盖医嘱、处方、检查申请和医嘱执行记录，并统一委托给数据库版 service 处理业务规则。
 */
@RestController
public class ClinicalController {

    private final ClinicalService clinicalService;

    public ClinicalController(ClinicalService clinicalService) {
        this.clinicalService = clinicalService;
    }

    @PostMapping("/medical-orders")
    public ApiResponse<Map<String, Object>> createMedicalOrder(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        return ApiResponse.success(clinicalService.createMedicalOrder(normalizeDoctorPayload(context, request, "当前登录医生只能为自己录入医嘱")));
    }

    @GetMapping("/medical-orders")
    public ApiResponse<PageResult<Map<String, Object>>> listMedicalOrders(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "recordId", required = false) Long recordId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Long scopedDoctorId = doctorId;
        if (context.isDoctor()) {
            if (doctorId != null && !context.matchesUserId(doctorId)) {
                throw new IllegalArgumentException("当前登录医生只能查询自己的医嘱");
            }
            scopedDoctorId = context.userId();
        }
        return ApiResponse.success(clinicalService.listMedicalOrders(patientId, scopedDoctorId, status, recordId, page, limit));
    }

    @GetMapping("/medical-orders/{id}")
    public ApiResponse<Map<String, Object>> getMedicalOrder(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Map<String, Object> row = clinicalService.getMedicalOrder(id);
        ensureDoctorOwnedRow(context, row, "doctorId", "当前登录医生无权查看该医嘱");
        return ApiResponse.success(row);
    }

    @PutMapping("/medical-orders/{id}")
    public ApiResponse<Map<String, Object>> updateMedicalOrder(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        ensureDoctorOwnedRow(context, clinicalService.getMedicalOrder(id), "doctorId", "当前登录医生无权维护该医嘱");
        return ApiResponse.success(clinicalService.updateMedicalOrder(id, normalizeDoctorPayload(context, request, "当前登录医生只能维护自己的医嘱")));
    }

    @DeleteMapping("/medical-orders/{id}")
    public ApiResponse<Map<String, Object>> deleteMedicalOrder(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        ensureDoctorOwnedRow(context, clinicalService.getMedicalOrder(id), "doctorId", "当前登录医生无权维护该医嘱");
        return ApiResponse.success(clinicalService.deleteMedicalOrder(id));
    }

    @PostMapping("/medical-orders/{id}/audit-result")
    public ApiResponse<Map<String, Object>> updateMedicalOrderAuditResult(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        ensureAuditRole(TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName));
        return ApiResponse.success(clinicalService.updateMedicalOrderAuditResult(id, request));
    }

    @PostMapping("/medical-orders/{id}/execute")
    public ApiResponse<Map<String, Object>> executeMedicalOrder(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        return ApiResponse.success(clinicalService.executeMedicalOrder(id, normalizeExecutionPayload(context, request)));
    }

    @GetMapping("/medical-orders/{id}/executions")
    public ApiResponse<PageResult<Map<String, Object>>> listExecutions(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ApiResponse.success(clinicalService.listExecutions(id, page, limit));
    }

    @PostMapping("/prescriptions")
    public ApiResponse<Map<String, Object>> createPrescription(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        return ApiResponse.success(clinicalService.createPrescription(normalizeDoctorPayload(context, request, "当前登录医生只能为自己开具处方")));
    }

    @GetMapping("/prescriptions")
    public ApiResponse<PageResult<Map<String, Object>>> listPrescriptions(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "recordId", required = false) Long recordId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Long scopedPatientId = patientId;
        Long scopedDoctorId = doctorId;
        if (context.isPatient()) {
            if (patientId != null && !context.matchesUserId(patientId)) {
                throw new IllegalArgumentException("当前登录患者只能查询自己的处方");
            }
            scopedPatientId = context.userId();
        }
        if (context.isDoctor()) {
            if (doctorId != null && !context.matchesUserId(doctorId)) {
                throw new IllegalArgumentException("当前登录医生只能查询自己的处方");
            }
            scopedDoctorId = context.userId();
        }
        return ApiResponse.success(clinicalService.listPrescriptions(scopedPatientId, scopedDoctorId, status, recordId, page, limit));
    }

    @GetMapping("/prescriptions/{id}")
    public ApiResponse<Map<String, Object>> getPrescription(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Map<String, Object> row = clinicalService.getPrescription(id);
        ensurePatientOrDoctorOwnedRow(context, row, "patientId", "doctorId", "处方");
        return ApiResponse.success(row);
    }

    @PutMapping("/prescriptions/{id}")
    public ApiResponse<Map<String, Object>> updatePrescription(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        ensureDoctorOwnedRow(context, clinicalService.getPrescription(id), "doctorId", "当前登录医生无权维护该处方");
        return ApiResponse.success(clinicalService.updatePrescription(id, normalizeDoctorPayload(context, request, "当前登录医生只能维护自己的处方")));
    }

    @DeleteMapping("/prescriptions/{id}")
    public ApiResponse<Map<String, Object>> deletePrescription(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        ensureDoctorOwnedRow(context, clinicalService.getPrescription(id), "doctorId", "当前登录医生无权维护该处方");
        return ApiResponse.success(clinicalService.deletePrescription(id));
    }

    @PostMapping("/prescriptions/{id}/audit-result")
    public ApiResponse<Map<String, Object>> updatePrescriptionAuditResult(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        ensureAuditRole(TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName));
        return ApiResponse.success(clinicalService.updatePrescriptionAuditResult(id, request));
    }

    @PostMapping("/prescriptions/{id}/pay")
    public ApiResponse<Map<String, Object>> payPrescription(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        if (!context.isPatient()) {
            throw new IllegalArgumentException("仅患者可支付处方");
        }
        Map<String, Object> row = clinicalService.getPrescription(id);
        if (!(row.get("patientId") instanceof Number num) || !context.matchesUserId(num.longValue())) {
            throw new IllegalArgumentException("当前登录患者无权支付该处方");
        }
        return ApiResponse.success(clinicalService.payPrescription(id));
    }

    @GetMapping("/medicines")
    public ApiResponse<List<Map<String, Object>>> listMedicines() {
        return ApiResponse.success(clinicalService.listMedicines());
    }

    @GetMapping("/test-items")
    public ApiResponse<List<Map<String, Object>>> listTestItems() {
        return ApiResponse.success(clinicalService.listTestItems());
    }

    @PostMapping("/test-items")
    public ApiResponse<Map<String, Object>> createTestItem(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(clinicalService.createTestItem(request));
    }

    @PutMapping("/test-items/{id}")
    public ApiResponse<Map<String, Object>> updateTestItem(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(clinicalService.updateTestItem(id, request));
    }

    @DeleteMapping("/test-items/{id}")
    public ApiResponse<Map<String, Object>> deleteTestItem(@PathVariable("id") Long id) {
        return ApiResponse.success(clinicalService.deleteTestItem(id));
    }

    @PostMapping("/test-requests")
    public ApiResponse<Map<String, Object>> createTestRequest(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        return ApiResponse.success(clinicalService.createTestRequest(normalizeDoctorPayload(context, request, "当前登录医生只能为自己提交检查申请")));
    }

    @GetMapping("/test-requests")
    public ApiResponse<PageResult<Map<String, Object>>> listTestRequests(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "recordId", required = false) Long recordId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Long scopedPatientId = patientId;
        Long scopedDoctorId = doctorId;
        if (context.isPatient()) {
            if (patientId != null && !context.matchesUserId(patientId)) {
                throw new IllegalArgumentException("当前登录患者只能查询自己的检查申请");
            }
            scopedPatientId = context.userId();
        }
        if (context.isDoctor()) {
            if (doctorId != null && !context.matchesUserId(doctorId)) {
                throw new IllegalArgumentException("当前登录医生只能查询自己的检查申请");
            }
            scopedDoctorId = context.userId();
        }
        return ApiResponse.success(clinicalService.listTestRequests(scopedPatientId, scopedDoctorId, status, recordId, page, limit));
    }

    @GetMapping("/test-requests/{id}")
    public ApiResponse<Map<String, Object>> getTestRequest(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Map<String, Object> row = clinicalService.getTestRequest(id);
        ensurePatientOrDoctorOwnedRow(context, row, "patientId", "doctorId", "检查申请");
        return ApiResponse.success(row);
    }

    @PutMapping("/test-requests/{id}")
    public ApiResponse<Map<String, Object>> updateTestRequest(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        ensureDoctorOwnedRow(context, clinicalService.getTestRequest(id), "doctorId", "当前登录医生无权维护该检查申请");
        return ApiResponse.success(clinicalService.updateTestRequest(id, normalizeDoctorPayload(context, request, "当前登录医生只能维护自己的检查申请")));
    }

    @DeleteMapping("/test-requests/{id}")
    public ApiResponse<Map<String, Object>> deleteTestRequest(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        ensureDoctorOwnedRow(context, clinicalService.getTestRequest(id), "doctorId", "当前登录医生无权维护该检查申请");
        return ApiResponse.success(clinicalService.deleteTestRequest(id));
    }

    @PostMapping("/test-requests/{id}/audit-result")
    public ApiResponse<Map<String, Object>> updateTestAuditResult(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        ensureAuditRole(TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName));
        return ApiResponse.success(clinicalService.updateTestAuditResult(id, request));
    }

    @PostMapping("/test-requests/{id}/pay")
    public ApiResponse<Map<String, Object>> payTestRequest(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        if (!context.isPatient()) {
            throw new IllegalArgumentException("仅患者可支付检查费用");
        }
        Map<String, Object> row = clinicalService.getTestRequest(id);
        if (!(row.get("patientId") instanceof Number num) || !context.matchesUserId(num.longValue())) {
            throw new IllegalArgumentException("当前登录患者无权支付该检查");
        }
        return ApiResponse.success(clinicalService.payTestRequest(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    private Map<String, Object> normalizeDoctorPayload(TrustedUserContext context, Map<String, Object> request, String mismatchMessage) {
        if (!context.isDoctor()) {
            return request;
        }
        Map<String, Object> payload = copyPayload(request);
        if (payload.containsKey("doctorId") && payload.get("doctorId") != null) {
            Long doctorId = parseLong(payload.get("doctorId"));
            if (doctorId == null || !context.matchesUserId(doctorId)) {
                throw new IllegalArgumentException(mismatchMessage);
            }
        } else {
            payload.put("doctorId", context.userId());
        }
        if (!payload.containsKey("doctorName") || payload.get("doctorName") == null || payload.get("doctorName").toString().trim().isEmpty()) {
            payload.put("doctorName", context.username());
        }
        return payload;
    }

    private Map<String, Object> normalizeExecutionPayload(TrustedUserContext context, Map<String, Object> request) {
        if (!context.authenticated()) {
            return request;
        }
        if (!context.isNurse() && !context.isAdminLike()) {
            throw new IllegalArgumentException("当前登录角色无权执行医嘱");
        }
        Map<String, Object> payload = copyPayload(request);
        if (context.isNurse()) {
            if (!payload.containsKey("nurseId") || payload.get("nurseId") == null) {
                payload.put("nurseId", context.userId());
            } else {
                Long nurseId = parseLong(payload.get("nurseId"));
                if (nurseId == null || !context.matchesUserId(nurseId)) {
                    throw new IllegalArgumentException("当前登录护士只能以本人身份执行医嘱");
                }
            }
            if (!payload.containsKey("nurseName") || payload.get("nurseName") == null || payload.get("nurseName").toString().trim().isEmpty()) {
                payload.put("nurseName", context.username());
            }
        }
        return payload;
    }

    private void ensureAuditRole(TrustedUserContext context) {
        if (!context.authenticated()) {
            return;
        }
        if (context.isDirector() || context.isAdminLike()) {
            return;
        }
        throw new IllegalArgumentException("当前登录角色无权执行审核");
    }

    private void ensureDoctorOwnedRow(TrustedUserContext context, Map<String, Object> row, String key, String message) {
        if (!context.isDoctor()) {
            return;
        }
        Long ownerId = parseLong(row.get(key));
        if (ownerId == null || !context.matchesUserId(ownerId)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void ensurePatientOrDoctorOwnedRow(TrustedUserContext context, Map<String, Object> row, String patientKey, String doctorKey, String label) {
        if (context.isPatient()) {
            Long patientId = parseLong(row.get(patientKey));
            if (patientId == null || !context.matchesUserId(patientId)) {
                throw new IllegalArgumentException("当前登录患者无权查看该" + label);
            }
        }
        if (context.isDoctor()) {
            Long doctorId = parseLong(row.get(doctorKey));
            if (doctorId == null || !context.matchesUserId(doctorId)) {
                throw new IllegalArgumentException("当前登录医生无权查看该" + label);
            }
        }
    }

    private Map<String, Object> copyPayload(Map<String, Object> request) {
        return request == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
