package com.emr.workflow.controller;

import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.common.TrustedUserContext;
import com.emr.workflow.service.BusinessCallbackException;
import com.emr.workflow.service.WorkflowService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工作流审核接口。
 * 覆盖病历审核、医嘱审核、检查审核和归档审核的通用任务流。
 */
@RestController
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/workflow/tasks")
    public ApiResponse<Map<String, Object>> createTask(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        ensureWorkflowAccess(TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName));
        return ApiResponse.success(workflowService.createTask(request));
    }

    @GetMapping("/workflow/tasks")
    public ApiResponse<PageResult<Map<String, Object>>> listTasks(
            @RequestParam(name = "businessType", required = false) String businessType,
            @RequestParam(name = "businessId", required = false) Long businessId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "assigneeRole", required = false) String assigneeRole,
            @RequestParam(name = "applicantId", required = false) Long applicantId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        ensureWorkflowAccess(TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName));
        return ApiResponse.success(workflowService.listTasks(businessType, businessId, status, assigneeRole, applicantId, page, limit));
    }

    @PostMapping("/workflow/tasks/{id}/audit")
    public ApiResponse<Map<String, Object>> auditTask(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        ensureWorkflowAccess(TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName));
        return ApiResponse.success(workflowService.auditTask(id, request));
    }

    @GetMapping("/workflow/audit-records")
    public ApiResponse<PageResult<Map<String, Object>>> listAuditRecords(
            @RequestParam(name = "taskId", required = false) Long taskId,
            @RequestParam(name = "auditorId", required = false) Long auditorId,
            @RequestParam(name = "auditResult", required = false) String auditResult,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        ensureWorkflowAccess(TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName));
        return ApiResponse.success(workflowService.listAuditRecords(taskId, auditorId, auditResult, page, limit));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    @ExceptionHandler(BusinessCallbackException.class)
    public ApiResponse<Void> handleBusinessCallback(BusinessCallbackException exception) {
        return ApiResponse.fail(502, exception.getMessage());
    }

    private void ensureWorkflowAccess(TrustedUserContext context) {
        if (!context.authenticated()) {
            return;
        }
        if (context.isAdminLike() || context.isDirector()) {
            return;
        }
        throw new IllegalArgumentException("当前登录角色无权访问审核任务中心");
    }
}
