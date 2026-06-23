package com.emr.billing.controller;

import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.common.TrustedUserContext;
import com.emr.billing.service.BillingService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 费用管理接口。
 * 覆盖费用新增、列表、详情、支付和患者查看费用。
 */
@RestController
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/fees")
    public ApiResponse<Map<String, Object>> createFee(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(billingService.createFee(request));
    }

    @GetMapping("/fees")
    public ApiResponse<PageResult<Map<String, Object>>> listFees(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "payStatus", required = false) String payStatus,
            @RequestParam(name = "businessType", required = false) String businessType,
            @RequestParam(name = "businessId", required = false) Long businessId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Long scopedPatientId = patientId;
        if (context.isPatient()) {
            if (patientId != null && !context.matchesUserId(patientId)) {
                throw new IllegalArgumentException("当前登录患者只能查询自己的费用");
            }
            scopedPatientId = context.userId();
        }
        return ApiResponse.success(billingService.listFees(scopedPatientId, payStatus, businessType, businessId, page, limit));
    }

    @GetMapping("/fees/{id}")
    public ApiResponse<Map<String, Object>> getFee(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Map<String, Object> fee = billingService.getFee(id);
        ensureFeeAccess(context, fee, "查看");
        return ApiResponse.success(fee);
    }

    @PutMapping("/fees/{id}")
    public ApiResponse<Map<String, Object>> updateFee(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(billingService.updateFee(id, request));
    }

    @PostMapping("/fees/{id}/pay")
    public ApiResponse<Map<String, Object>> payFee(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Map<String, Object> current = billingService.getFee(id);
        ensureFeeAccess(context, current, "支付");
        return ApiResponse.success(billingService.payFee(id));
    }

    @DeleteMapping("/fees/{id}")
    public ApiResponse<Map<String, Object>> deleteFee(@PathVariable("id") Long id) {
        return ApiResponse.success(billingService.deleteFee(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    private void ensureFeeAccess(TrustedUserContext context, Map<String, Object> fee, String action) {
        if (!context.isPatient()) {
            return;
        }
        Object patientId = fee.get("patientId");
        if (!(patientId instanceof Number number) || !context.matchesUserId(number.longValue())) {
            throw new IllegalArgumentException("当前登录患者无权" + action + "该费用");
        }
    }
}
