package com.emr.record.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.record.entity.ArchiveApplicationEntity;
import com.emr.record.entity.MedicalRecordArchiveEntity;
import com.emr.record.entity.MedicalRecordEntity;
import com.emr.record.entity.MedicalRecordTemplateEntity;
import com.emr.record.mapper.ArchiveApplicationMapper;
import com.emr.record.mapper.MedicalRecordArchiveMapper;
import com.emr.record.mapper.MedicalRecordMapper;
import com.emr.record.mapper.MedicalRecordTemplateMapper;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 病历模板和归档接口。
 * 覆盖模板管理、使用模板、归档申请、归档审核和归档列表。
 */
@RestController
public class TemplateArchiveController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ARCHIVE_STATUS_NOT_SUBMITTED = "not_submitted";
    private static final String ARCHIVE_STATUS_PENDING = "pending";
    private static final String ARCHIVE_STATUS_ARCHIVED = "archived";
    private static final String ARCHIVE_STATUS_REJECTED = "rejected";

    private final MedicalRecordTemplateMapper templateMapper;
    private final ArchiveApplicationMapper archiveApplicationMapper;
    private final MedicalRecordArchiveMapper archiveMapper;
    private final MedicalRecordMapper medicalRecordMapper;

    public TemplateArchiveController(
            MedicalRecordTemplateMapper templateMapper,
            ArchiveApplicationMapper archiveApplicationMapper,
            MedicalRecordArchiveMapper archiveMapper,
            MedicalRecordMapper medicalRecordMapper
    ) {
        this.templateMapper = templateMapper;
        this.archiveApplicationMapper = archiveApplicationMapper;
        this.archiveMapper = archiveMapper;
        this.medicalRecordMapper = medicalRecordMapper;
    }

    @PostMapping("/medical-record-templates")
    public ApiResponse<Map<String, Object>> createTemplate(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        MedicalRecordTemplateEntity entity = new MedicalRecordTemplateEntity();
        entity.setTemplateName(requireText(payload.get("templateName"), "模板名称不能为空"));
        entity.setTemplateType(stringValue(payload.get("templateType")));
        entity.setContent(requireText(payload.get("content"), "模板内容不能为空"));
        entity.setCreatorId(longValue(payload.get("creatorId")));
        entity.setCreatorName(stringValue(payload.get("creatorName")));
        entity.setStatus(1);
        templateMapper.insert(entity);
        return ApiResponse.success(toTemplateRow(entity));
    }

    @GetMapping("/medical-record-templates")
    public ApiResponse<PageResult<Map<String, Object>>> listTemplates(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> rows = templateMapper.selectList(new LambdaQueryWrapper<MedicalRecordTemplateEntity>()
                        .orderByDesc(MedicalRecordTemplateEntity::getId))
                .stream()
                .map(this::toTemplateRow)
                .toList();
        return ApiResponse.success(PageResult.of(rows, page, limit));
    }

    @GetMapping("/medical-record-templates/{id}")
    public ApiResponse<Map<String, Object>> getTemplate(@PathVariable("id") Long id) {
        return ApiResponse.success(toTemplateRow(requireTemplate(id)));
    }

    @PutMapping("/medical-record-templates/{id}")
    public ApiResponse<Map<String, Object>> updateTemplate(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> request) {
        MedicalRecordTemplateEntity entity = requireTemplate(id);
        Map<String, Object> payload = safePayload(request);
        if (payload.containsKey("templateName")) {
            entity.setTemplateName(requireText(payload.get("templateName"), "模板名称不能为空"));
        }
        if (payload.containsKey("templateType")) {
            entity.setTemplateType(stringValue(payload.get("templateType")));
        }
        if (payload.containsKey("content")) {
            entity.setContent(requireText(payload.get("content"), "模板内容不能为空"));
        }
        if (payload.containsKey("creatorId")) {
            entity.setCreatorId(longValue(payload.get("creatorId")));
        }
        if (payload.containsKey("creatorName")) {
            entity.setCreatorName(stringValue(payload.get("creatorName")));
        }
        if (payload.containsKey("status")) {
            entity.setStatus(intValue(payload.get("status"), entity.getStatus()));
        }
        templateMapper.updateById(entity);
        return ApiResponse.success(toTemplateRow(entity));
    }

    @DeleteMapping("/medical-record-templates/{id}")
    public ApiResponse<Map<String, Object>> deleteTemplate(@PathVariable("id") Long id) {
        MedicalRecordTemplateEntity entity = requireTemplate(id);
        entity.setStatus(0);
        templateMapper.updateById(entity);
        return ApiResponse.success(toTemplateRow(entity));
    }

    @PostMapping("/medical-record-archives/applications")
    public ApiResponse<Map<String, Object>> createArchiveApplication(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        MedicalRecordEntity record = requireMedicalRecord(requireId(payload.get("recordId"), "病历ID不能为空"));
        if (ARCHIVE_STATUS_PENDING.equals(record.getArchiveStatus()) || ARCHIVE_STATUS_ARCHIVED.equals(record.getArchiveStatus())) {
            throw new IllegalArgumentException("当前病历已在归档流程中");
        }

        ArchiveApplicationEntity entity = new ArchiveApplicationEntity();
        entity.setApplicationNo(buildNumber("ARCHAPP"));
        entity.setRecordId(record.getId());
        entity.setRecordNo(record.getRecordNo());
        entity.setPatientId(record.getPatientId());
        entity.setPatientName(record.getPatientName());
        entity.setDoctorId(record.getDoctorId());
        entity.setDoctorName(record.getDoctorName());
        entity.setStatus(ARCHIVE_STATUS_PENDING);
        archiveApplicationMapper.insert(entity);

        record.setArchiveStatus(ARCHIVE_STATUS_PENDING);
        medicalRecordMapper.updateById(record);
        return ApiResponse.success(toApplicationRow(entity));
    }

    @GetMapping("/medical-record-archives/applications")
    public ApiResponse<PageResult<Map<String, Object>>> listArchiveApplications(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> rows = archiveApplicationMapper.selectList(new LambdaQueryWrapper<ArchiveApplicationEntity>()
                        .orderByDesc(ArchiveApplicationEntity::getId))
                .stream()
                .map(this::toApplicationRow)
                .toList();
        return ApiResponse.success(PageResult.of(rows, page, limit));
    }

    @PostMapping("/medical-record-archives/applications/{id}/audit")
    public ApiResponse<Map<String, Object>> auditArchiveApplication(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        ArchiveApplicationEntity application = requireApplication(id);
        MedicalRecordEntity record = requireMedicalRecord(application.getRecordId());
        String result = requireText(payload.get("auditResult"), "审核结果不能为空");
        if (!"approved".equals(result) && !"rejected".equals(result)) {
            throw new IllegalArgumentException("审核结果不能为空");
        }
        application.setStatus(result);
        application.setAuditUserId(longValue(payload.get("auditUserId")));
        application.setAuditUserName(stringValue(payload.get("auditUserName")));
        application.setAuditOpinion(stringValue(payload.get("auditOpinion")));
        application.setAuditTime(LocalDateTime.now());
        archiveApplicationMapper.updateById(application);

        if ("approved".equals(result)) {
            MedicalRecordArchiveEntity archive = new MedicalRecordArchiveEntity();
            archive.setArchiveNo(buildNumber("ARCH"));
            archive.setApplicationId(application.getId());
            archive.setRecordId(record.getId());
            archive.setRecordNo(record.getRecordNo());
            archive.setPatientId(record.getPatientId());
            archive.setPatientName(record.getPatientName());
            archive.setArchiveContent(resolveArchiveContent(payload.get("archiveContent"), record));
            archive.setArchivedAt(LocalDateTime.now());
            archiveMapper.insert(archive);
            record.setArchiveStatus(ARCHIVE_STATUS_ARCHIVED);
        } else {
            record.setArchiveStatus(ARCHIVE_STATUS_REJECTED);
        }
        medicalRecordMapper.updateById(record);
        return ApiResponse.success(toApplicationRow(application));
    }

    @GetMapping("/medical-record-archives")
    public ApiResponse<PageResult<Map<String, Object>>> listArchives(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> rows = archiveMapper.selectList(new LambdaQueryWrapper<MedicalRecordArchiveEntity>()
                        .orderByDesc(MedicalRecordArchiveEntity::getId))
                .stream()
                .map(this::toArchiveRow)
                .toList();
        return ApiResponse.success(PageResult.of(rows, page, limit));
    }

    private Map<String, Object> toTemplateRow(MedicalRecordTemplateEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("templateName", entity.getTemplateName());
        row.put("templateType", entity.getTemplateType());
        row.put("content", entity.getContent());
        row.put("creatorId", entity.getCreatorId());
        row.put("creatorName", entity.getCreatorName());
        row.put("status", entity.getStatus());
        return row;
    }

    private Map<String, Object> toApplicationRow(ArchiveApplicationEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("applicationNo", entity.getApplicationNo());
        row.put("recordId", entity.getRecordId());
        row.put("recordNo", entity.getRecordNo());
        row.put("patientId", entity.getPatientId());
        row.put("patientName", entity.getPatientName());
        row.put("doctorId", entity.getDoctorId());
        row.put("doctorName", entity.getDoctorName());
        row.put("status", entity.getStatus());
        row.put("auditUserId", entity.getAuditUserId());
        row.put("auditUserName", entity.getAuditUserName());
        row.put("auditOpinion", entity.getAuditOpinion());
        row.put("auditTime", entity.getAuditTime());
        return row;
    }

    private Map<String, Object> toArchiveRow(MedicalRecordArchiveEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("archiveNo", entity.getArchiveNo());
        row.put("applicationId", entity.getApplicationId());
        row.put("recordId", entity.getRecordId());
        row.put("recordNo", entity.getRecordNo());
        row.put("patientId", entity.getPatientId());
        row.put("patientName", entity.getPatientName());
        row.put("archiveContent", entity.getArchiveContent());
        row.put("archivedAt", entity.getArchivedAt());
        return row;
    }

    private MedicalRecordTemplateEntity requireTemplate(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        MedicalRecordTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        return entity;
    }

    private ArchiveApplicationEntity requireApplication(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("归档申请不存在");
        }
        ArchiveApplicationEntity entity = archiveApplicationMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("归档申请不存在");
        }
        return entity;
    }

    private MedicalRecordEntity requireMedicalRecord(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("病历记录不存在");
        }
        MedicalRecordEntity entity = medicalRecordMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("病历记录不存在");
        }
        return entity;
    }

    /**
     * Map 请求体统一兜底，保证缺参数时返回字段级错误提示。
     */
    private Map<String, Object> safePayload(Map<String, Object> request) {
        return request == null ? Map.of() : request;
    }

    private Long requireId(Object value, String message) {
        if (!(value instanceof Number number) || number.longValue() <= 0) {
            throw new IllegalArgumentException(message);
        }
        return number.longValue();
    }

    private String requireText(Object value, String message) {
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.toString().trim();
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = stringValue(value);
        return text == null ? null : Long.parseLong(text);
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = stringValue(value);
        return text == null ? defaultValue : Integer.parseInt(text);
    }

    private String buildNumber(String prefix) {
        return prefix + LocalDate.now().format(DATE_FORMATTER) + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String resolveArchiveContent(Object archiveContent, MedicalRecordEntity record) {
        String content = stringValue(archiveContent);
        if (content != null) {
            return content;
        }
        return "recordNo=" + record.getRecordNo()
                + ", diagnosis=" + stringValue(record.getDiagnosis())
                + ", treatmentAdvice=" + stringValue(record.getTreatmentAdvice());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }
}
