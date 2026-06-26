package com.emr.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.common.PageResult;
import com.emr.workflow.entity.WorkflowAuditRecordEntity;
import com.emr.workflow.entity.WorkflowTaskEntity;
import com.emr.workflow.mapper.WorkflowAuditRecordMapper;
import com.emr.workflow.mapper.WorkflowTaskMapper;
import com.emr.workflow.service.BusinessAuditCallbackClient;
import com.emr.workflow.service.WorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 数据库版工作流审核服务。
 * 用 `shenherenwu`、`shenhejilu` 替代原有内存 Map，并在审核成功时同步回写业务服务状态。
 */
@Service
public class DatabaseWorkflowService implements WorkflowService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> SUPPORTED_BUSINESS_TYPES = Set.of("medical_order", "test_request", "record_archive", "medical_record", "prescription");
    private static final Set<String> SUPPORTED_ASSIGNEE_ROLES = Set.of("director", "admin");
    private static final Set<String> SUPPORTED_AUDIT_RESULTS = Set.of("approved", "rejected");

    private final WorkflowTaskMapper workflowTaskMapper;
    private final WorkflowAuditRecordMapper workflowAuditRecordMapper;
    private final BusinessAuditCallbackClient businessAuditCallbackClient;

    public DatabaseWorkflowService(
            WorkflowTaskMapper workflowTaskMapper,
            WorkflowAuditRecordMapper workflowAuditRecordMapper,
            BusinessAuditCallbackClient businessAuditCallbackClient
    ) {
        this.workflowTaskMapper = workflowTaskMapper;
        this.workflowAuditRecordMapper = workflowAuditRecordMapper;
        this.businessAuditCallbackClient = businessAuditCallbackClient;
    }

    /**
     * 创建审核任务。
     * 当前统一拦截重复 pending 待办，避免同一个业务在主任工作台出现多条未处理任务。
     */
    @Override
    public Map<String, Object> createTask(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        String businessType = requireSupportedBusinessType(payload.get("businessType"));
        Long businessId = requireId(payload.get("businessId"), "业务ID不能为空");
        long pendingCount = workflowTaskMapper.selectCount(new LambdaQueryWrapper<WorkflowTaskEntity>()
                .eq(WorkflowTaskEntity::getBusinessType, businessType)
                .eq(WorkflowTaskEntity::getBusinessId, businessId)
                .eq(WorkflowTaskEntity::getStatus, "pending"));
        if (pendingCount > 0) {
            throw new IllegalArgumentException("当前业务已有待审核任务");
        }

        WorkflowTaskEntity entity = new WorkflowTaskEntity();
        entity.setTaskNo(buildTaskNo());
        entity.setBusinessType(businessType);
        entity.setBusinessId(businessId);
        entity.setBusinessNo(blankToNull(payload.get("businessNo")));
        entity.setApplicantId(requireId(payload.get("applicantId"), "申请人ID不能为空"));
        entity.setApplicantName(requireText(payload.get("applicantName"), "申请人姓名不能为空"));
        entity.setAssigneeRole(requireAssigneeRole(payload.get("assigneeRole")));
        entity.setStatus("pending");
        workflowTaskMapper.insert(entity);
        return toTaskRow(entity, null);
    }

    /**
     * 查询审核任务列表。
     * 过滤项完全按设计文档开放，方便主任、管理员从不同角度筛选待办和历史结果。
     */
    @Override
    public PageResult<Map<String, Object>> listTasks(String businessType, Long businessId, String status, String assigneeRole, Long applicantId, int page, int limit) {
        LambdaQueryWrapper<WorkflowTaskEntity> wrapper = new LambdaQueryWrapper<WorkflowTaskEntity>()
                .orderByDesc(WorkflowTaskEntity::getId);
        if (blankToNull(businessType) != null) {
            wrapper.eq(WorkflowTaskEntity::getBusinessType, businessType.trim());
        }
        if (businessId != null) {
            wrapper.eq(WorkflowTaskEntity::getBusinessId, businessId);
        }
        if (blankToNull(status) != null) {
            wrapper.eq(WorkflowTaskEntity::getStatus, status.trim());
        }
        if (blankToNull(assigneeRole) != null) {
            wrapper.eq(WorkflowTaskEntity::getAssigneeRole, assigneeRole.trim());
        }
        if (applicantId != null) {
            wrapper.eq(WorkflowTaskEntity::getApplicantId, applicantId);
        }
        List<Map<String, Object>> rows = workflowTaskMapper.selectList(wrapper).stream()
                .map(entity -> toTaskRow(entity, loadLastAuditRecordId(entity.getId())))
                .toList();
        return PageResult.of(rows, page, limit);
    }

    /**
     * 审核任务。
     * 先完成下游业务状态回写，再落库任务和审核记录，确保跨服务状态不会出现“任务已通过但业务没同步”的假成功。
     */
    @Override
    @Transactional
    public Map<String, Object> auditTask(Long taskId, Map<String, Object> request) {
        WorkflowTaskEntity task = requireTask(taskId);
        if (!"pending".equals(task.getStatus())) {
            throw new IllegalArgumentException("当前审核任务已处理");
        }

        Map<String, Object> payload = safePayload(request);
        Long auditorId = requireId(payload.get("auditorId"), "审核人ID不能为空");
        String auditorName = requireText(payload.get("auditorName"), "审核人姓名不能为空");
        String auditResult = requireAuditResult(payload.get("auditResult"));
        String auditOpinion = blankToNull(payload.get("auditOpinion"));

        businessAuditCallbackClient.syncAuditResult(
                task.getBusinessType(),
                task.getBusinessId(),
                auditorId,
                auditorName,
                auditResult,
                auditOpinion
        );

        WorkflowAuditRecordEntity record = new WorkflowAuditRecordEntity();
        record.setTaskId(task.getId());
        record.setTaskNo(task.getTaskNo());
        record.setAuditorId(auditorId);
        record.setAuditorName(auditorName);
        record.setAuditResult(auditResult);
        record.setAuditOpinion(auditOpinion);
        record.setAuditTime(LocalDateTime.now());
        workflowAuditRecordMapper.insert(record);

        task.setStatus(auditResult);
        workflowTaskMapper.updateById(task);
        return toTaskRow(task, record.getId());
    }

    /**
     * 查询审核记录列表。
     * 保留任务、审核人、审核结果三个过滤项，满足后台按任务追踪审核历史的需求。
     */
    @Override
    public PageResult<Map<String, Object>> listAuditRecords(Long taskId, Long auditorId, String auditResult, int page, int limit) {
        LambdaQueryWrapper<WorkflowAuditRecordEntity> wrapper = new LambdaQueryWrapper<WorkflowAuditRecordEntity>()
                .orderByDesc(WorkflowAuditRecordEntity::getId);
        if (taskId != null) {
            wrapper.eq(WorkflowAuditRecordEntity::getTaskId, taskId);
        }
        if (auditorId != null) {
            wrapper.eq(WorkflowAuditRecordEntity::getAuditorId, auditorId);
        }
        if (blankToNull(auditResult) != null) {
            wrapper.eq(WorkflowAuditRecordEntity::getAuditResult, auditResult.trim());
        }
        List<Map<String, Object>> rows = workflowAuditRecordMapper.selectList(wrapper).stream()
                .map(this::toAuditRecordRow)
                .toList();
        return PageResult.of(rows, page, limit);
    }

    private WorkflowTaskEntity requireTask(Long taskId) {
        Long validTaskId = requireId(taskId, "审核任务ID不能为空");
        WorkflowTaskEntity task = workflowTaskMapper.selectById(validTaskId);
        if (task == null) {
            throw new IllegalArgumentException("审核任务不存在");
        }
        return task;
    }

    private Long loadLastAuditRecordId(Long taskId) {
        WorkflowAuditRecordEntity latestRecord = workflowAuditRecordMapper.selectOne(new LambdaQueryWrapper<WorkflowAuditRecordEntity>()
                .eq(WorkflowAuditRecordEntity::getTaskId, taskId)
                .orderByDesc(WorkflowAuditRecordEntity::getId)
                .last("limit 1"));
        return latestRecord == null ? null : latestRecord.getId();
    }

    private Map<String, Object> toTaskRow(WorkflowTaskEntity entity, Long lastAuditRecordId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("taskNo", entity.getTaskNo());
        row.put("businessType", entity.getBusinessType());
        row.put("businessId", entity.getBusinessId());
        row.put("businessNo", entity.getBusinessNo());
        row.put("applicantId", entity.getApplicantId());
        row.put("applicantName", entity.getApplicantName());
        row.put("assigneeRole", entity.getAssigneeRole());
        row.put("status", entity.getStatus());
        row.put("lastAuditRecordId", lastAuditRecordId);
        return row;
    }

    private Map<String, Object> toAuditRecordRow(WorkflowAuditRecordEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("taskId", entity.getTaskId());
        row.put("taskNo", entity.getTaskNo());
        row.put("auditorId", entity.getAuditorId());
        row.put("auditorName", entity.getAuditorName());
        row.put("auditResult", entity.getAuditResult());
        row.put("auditOpinion", entity.getAuditOpinion());
        row.put("auditTime", entity.getAuditTime());
        return row;
    }

    private String buildTaskNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "WF" + LocalDate.now().format(DATE_FORMATTER) + suffix;
    }

    /**
     * Map 请求体为空时返回空 Map。
     * 这样所有必填校验都能稳定走统一的业务异常提示。
     */
    private Map<String, Object> safePayload(Map<String, Object> request) {
        return request == null ? Map.of() : request;
    }

    private String requireSupportedBusinessType(Object value) {
        String businessType = requireText(value, "业务类型不能为空");
        if (!SUPPORTED_BUSINESS_TYPES.contains(businessType)) {
            throw new IllegalArgumentException("暂不支持的业务类型");
        }
        return businessType;
    }

    private String requireAssigneeRole(Object value) {
        String assigneeRole = requireText(value, "审核角色不能为空");
        if (!SUPPORTED_ASSIGNEE_ROLES.contains(assigneeRole)) {
            throw new IllegalArgumentException("审核角色只允许director或admin");
        }
        return assigneeRole;
    }

    private String requireAuditResult(Object value) {
        String auditResult = requireText(value, "审核结果不能为空");
        if (!SUPPORTED_AUDIT_RESULTS.contains(auditResult)) {
            throw new IllegalArgumentException("审核结果只能是approved或rejected");
        }
        return auditResult;
    }

    private Long requireId(Object value, String message) {
        Long id = optionalId(value);
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
        return id;
    }

    private Long optionalId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ID格式不正确");
        }
    }

    private String requireText(Object value, String message) {
        String text = blankToNull(value);
        if (text == null) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    private String blankToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
