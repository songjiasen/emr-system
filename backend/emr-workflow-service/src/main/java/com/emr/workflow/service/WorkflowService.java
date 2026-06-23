package com.emr.workflow.service;

import com.emr.common.PageResult;

import java.util.Map;

/**
 * 工作流审核服务。
 * 承接审核任务创建、审核处理、审核记录查询和业务状态回写。
 */
public interface WorkflowService {

    /**
     * 创建审核任务。
     * 需要拦截同一业务的重复 pending 任务，避免主任列表里出现重复待办。
     */
    Map<String, Object> createTask(Map<String, Object> request);

    /**
     * 查询审核任务列表。
     * 支持按业务、状态、审核角色和申请人过滤，兼容后台审核列表分页展示。
     */
    PageResult<Map<String, Object>> listTasks(
            String businessType,
            Long businessId,
            String status,
            String assigneeRole,
            Long applicantId,
            int page,
            int limit
    );

    /**
     * 审核任务。
     * 只有待审核任务允许处理，且回写下游业务成功后才会正式更新任务和审核记录。
     */
    Map<String, Object> auditTask(Long taskId, Map<String, Object> request);

    /**
     * 查询审核记录列表。
     * 支持按任务、审核人和审核结果过滤，用于后台查看审核历史。
     */
    PageResult<Map<String, Object>> listAuditRecords(Long taskId, Long auditorId, String auditResult, int page, int limit);
}
