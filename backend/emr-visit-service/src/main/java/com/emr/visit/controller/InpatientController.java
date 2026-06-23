package com.emr.visit.controller;

import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.common.TrustedUserContext;
import com.emr.visit.service.InpatientService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分诊、入院、出院接口。
 * 覆盖护士端分诊建档、入院登记、病房分配和出院办理流程。
 */
@RestController
public class InpatientController {

    private final InpatientService inpatientService;

    public InpatientController(InpatientService inpatientService) {
        this.inpatientService = inpatientService;
    }

    @PostMapping("/triage-records")
    public ApiResponse<Map<String, Object>> createTriage(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        return ApiResponse.success(inpatientService.createTriage(normalizeNursePayload(context, request, "当前登录角色无权创建分诊记录", "当前登录护士只能以本人身份建分诊档案")));
    }

    @GetMapping("/triage-records")
    public ApiResponse<PageResult<Map<String, Object>>> listTriage(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "nurseId", required = false) Long nurseId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "triageLevel", required = false) String triageLevel,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Long scopedNurseId = nurseId;
        if (context.isNurse()) {
            if (nurseId != null && !context.matchesUserId(nurseId)) {
                throw new IllegalArgumentException("当前登录护士只能查询自己的分诊记录");
            }
            scopedNurseId = context.userId();
        }
        return ApiResponse.success(inpatientService.listTriage(patientId, scopedNurseId, status, triageLevel, page, limit));
    }

    @PostMapping("/inpatients/admissions")
    public ApiResponse<Map<String, Object>> createAdmission(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        return ApiResponse.success(inpatientService.createAdmission(normalizeNursePayload(context, request, "当前登录角色无权办理入院", "当前登录护士只能以本人身份办理入院")));
    }

    @GetMapping("/inpatients/admissions")
    public ApiResponse<PageResult<Map<String, Object>>> listAdmissions(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "wardNo", required = false) String wardNo,
            @RequestParam(name = "bedNo", required = false) String bedNo,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ApiResponse.success(inpatientService.listAdmissions(patientId, status, wardNo, bedNo, page, limit));
    }

    @PostMapping("/inpatients/admissions/{id}/discharge")
    public ApiResponse<Map<String, Object>> discharge(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        return ApiResponse.success(inpatientService.discharge(id, normalizeOperatorPayload(context, request)));
    }

    @GetMapping("/inpatients/discharges")
    public ApiResponse<PageResult<Map<String, Object>>> listDischarges(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "admissionId", required = false) Long admissionId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ApiResponse.success(inpatientService.listDischarges(patientId, admissionId, page, limit));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    private Map<String, Object> normalizeNursePayload(TrustedUserContext context, Map<String, Object> request, String roleMessage, String mismatchMessage) {
        if (!context.authenticated()) {
            return request;
        }
        if (!context.isNurse() && !context.isAdminLike()) {
            throw new IllegalArgumentException(roleMessage);
        }
        Map<String, Object> payload = copyPayload(request);
        if (context.isNurse()) {
            if (!payload.containsKey("nurseId") || payload.get("nurseId") == null) {
                payload.put("nurseId", context.userId());
            } else {
                Long nurseId = parseLong(payload.get("nurseId"));
                if (nurseId == null || !context.matchesUserId(nurseId)) {
                    throw new IllegalArgumentException(mismatchMessage);
                }
            }
            if (!payload.containsKey("nurseName") || payload.get("nurseName") == null || payload.get("nurseName").toString().trim().isEmpty()) {
                payload.put("nurseName", context.username());
            }
        }
        return payload;
    }

    private Map<String, Object> normalizeOperatorPayload(TrustedUserContext context, Map<String, Object> request) {
        if (!context.authenticated()) {
            return request;
        }
        if (!context.isNurse() && !context.isAdminLike()) {
            throw new IllegalArgumentException("当前登录角色无权办理出院");
        }
        Map<String, Object> payload = copyPayload(request);
        if (context.isNurse()) {
            if (!payload.containsKey("operatorId") || payload.get("operatorId") == null) {
                payload.put("operatorId", context.userId());
            }
            if (!payload.containsKey("operatorName") || payload.get("operatorName") == null || payload.get("operatorName").toString().trim().isEmpty()) {
                payload.put("operatorName", context.username());
            }
        }
        return payload;
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
