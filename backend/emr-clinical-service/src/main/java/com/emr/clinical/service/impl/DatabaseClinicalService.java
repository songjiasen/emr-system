package com.emr.clinical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.clinical.entity.MedicalOrderEntity;
import com.emr.clinical.entity.MedicalOrderExecutionEntity;
import com.emr.clinical.entity.PrescriptionEntity;
import com.emr.clinical.entity.TestRequestEntity;
import com.emr.clinical.mapper.MedicalOrderExecutionMapper;
import com.emr.clinical.mapper.MedicalOrderMapper;
import com.emr.clinical.mapper.PrescriptionMapper;
import com.emr.clinical.mapper.TestRequestMapper;
import com.emr.clinical.service.ClinicalService;
import com.emr.common.PageResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 数据库版诊疗服务。
 * 用临床四张主表替代原有内存 Map，并把审核、执行、取消等关键状态流固定下来。
 */
@Service
public class DatabaseClinicalService implements ClinicalService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> AUDIT_RESULTS = Set.of("approved", "rejected");

    private final MedicalOrderMapper medicalOrderMapper;
    private final MedicalOrderExecutionMapper executionMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final TestRequestMapper testRequestMapper;

    public DatabaseClinicalService(
            MedicalOrderMapper medicalOrderMapper,
            MedicalOrderExecutionMapper executionMapper,
            PrescriptionMapper prescriptionMapper,
            TestRequestMapper testRequestMapper
    ) {
        this.medicalOrderMapper = medicalOrderMapper;
        this.executionMapper = executionMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.testRequestMapper = testRequestMapper;
    }

    @Override
    public Map<String, Object> createMedicalOrder(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        MedicalOrderEntity entity = new MedicalOrderEntity();
        entity.setOrderNo(buildBusinessNo("ORDER"));
        entity.setRecordId(optionalId(payload.get("recordId")));
        entity.setPatientId(requireId(payload.get("patientId"), "患者ID不能为空"));
        entity.setPatientName(requireText(payload.get("patientName"), "患者姓名不能为空"));
        entity.setDoctorId(requireId(payload.get("doctorId"), "医生ID不能为空"));
        entity.setDoctorName(requireText(payload.get("doctorName"), "医生姓名不能为空"));
        entity.setOrderType(defaultText(payload.get("orderType"), "normal"));
        entity.setContent(requireText(payload.get("content"), "医嘱内容不能为空"));
        entity.setStatus("pending_audit");
        medicalOrderMapper.insert(entity);
        return toMedicalOrderRow(entity);
    }

    @Override
    public PageResult<Map<String, Object>> listMedicalOrders(Long patientId, Long doctorId, String status, Long recordId, int page, int limit) {
        LambdaQueryWrapper<MedicalOrderEntity> wrapper = new LambdaQueryWrapper<MedicalOrderEntity>().orderByDesc(MedicalOrderEntity::getId);
        if (patientId != null) wrapper.eq(MedicalOrderEntity::getPatientId, patientId);
        if (doctorId != null) wrapper.eq(MedicalOrderEntity::getDoctorId, doctorId);
        if (blankToNull(status) != null) wrapper.eq(MedicalOrderEntity::getStatus, status.trim());
        if (recordId != null) wrapper.eq(MedicalOrderEntity::getRecordId, recordId);
        return PageResult.of(medicalOrderMapper.selectList(wrapper).stream().map(this::toMedicalOrderRow).toList(), page, limit);
    }

    @Override
    public Map<String, Object> getMedicalOrder(Long id) {
        return toMedicalOrderRow(requireMedicalOrder(id));
    }

    @Override
    public Map<String, Object> updateMedicalOrder(Long id, Map<String, Object> request) {
        MedicalOrderEntity entity = requireMedicalOrder(id);
        if ("executed".equals(entity.getStatus())) {
            throw new IllegalArgumentException("已执行医嘱不允许修改");
        }
        Map<String, Object> payload = safePayload(request);
        boolean contentChanged = false;
        if (payload.containsKey("recordId")) entity.setRecordId(optionalId(payload.get("recordId")));
        if (payload.containsKey("orderType")) entity.setOrderType(defaultText(payload.get("orderType"), "normal"));
        if (payload.containsKey("content")) {
            entity.setContent(requireText(payload.get("content"), "医嘱内容不能为空"));
            contentChanged = true;
        }
        if (contentChanged && "approved".equals(entity.getStatus())) {
            entity.setStatus("pending_audit");
        }
        medicalOrderMapper.updateById(entity);
        return toMedicalOrderRow(entity);
    }

    @Override
    public Map<String, Object> deleteMedicalOrder(Long id) {
        MedicalOrderEntity entity = requireMedicalOrder(id);
        if ("executed".equals(entity.getStatus())) {
            throw new IllegalArgumentException("已执行医嘱不允许删除");
        }
        medicalOrderMapper.deleteById(entity.getId());
        return toMedicalOrderRow(entity);
    }

    @Override
    public Map<String, Object> updateMedicalOrderAuditResult(Long id, Map<String, Object> request) {
        MedicalOrderEntity entity = requireMedicalOrder(id);
        if ("executed".equals(entity.getStatus())) {
            throw new IllegalArgumentException("已执行医嘱不允许审核");
        }
        Map<String, Object> payload = safePayload(request);
        entity.setStatus(requireAuditResult(payload.get("auditResult")));
        entity.setAuditOpinion(blankToNull(payload.get("auditOpinion")));
        medicalOrderMapper.updateById(entity);
        return toMedicalOrderRow(entity);
    }

    @Override
    public Map<String, Object> executeMedicalOrder(Long id, Map<String, Object> request) {
        MedicalOrderEntity order = requireMedicalOrder(id);
        if (!"approved".equals(order.getStatus())) {
            throw new IllegalArgumentException("当前医嘱未审核通过，不能执行");
        }
        Map<String, Object> payload = safePayload(request);
        MedicalOrderExecutionEntity entity = new MedicalOrderExecutionEntity();
        entity.setOrderId(order.getId());
        entity.setOrderNo(order.getOrderNo());
        entity.setNurseId(requireId(payload.get("nurseId"), "护士ID不能为空"));
        entity.setNurseName(requireText(payload.get("nurseName"), "护士姓名不能为空"));
        entity.setExecutionTime(LocalDateTime.now());
        entity.setExecutionResult(requireText(payload.get("executionResult"), "执行结果不能为空"));
        entity.setRemark(blankToNull(payload.get("remark")));
        executionMapper.insert(entity);
        order.setStatus("executed");
        medicalOrderMapper.updateById(order);
        return toExecutionRow(entity);
    }

    @Override
    public PageResult<Map<String, Object>> listExecutions(Long orderId, int page, int limit) {
        requireId(orderId, "医嘱ID不能为空");
        LambdaQueryWrapper<MedicalOrderExecutionEntity> wrapper = new LambdaQueryWrapper<MedicalOrderExecutionEntity>()
                .eq(MedicalOrderExecutionEntity::getOrderId, orderId)
                .orderByDesc(MedicalOrderExecutionEntity::getId);
        return PageResult.of(executionMapper.selectList(wrapper).stream().map(this::toExecutionRow).toList(), page, limit);
    }

    @Override
    public Map<String, Object> createPrescription(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setPrescriptionNo(buildBusinessNo("PRES"));
        entity.setRecordId(optionalId(payload.get("recordId")));
        entity.setPatientId(requireId(payload.get("patientId"), "患者ID不能为空"));
        entity.setPatientName(requireText(payload.get("patientName"), "患者姓名不能为空"));
        entity.setDoctorId(requireId(payload.get("doctorId"), "医生ID不能为空"));
        entity.setDoctorName(requireText(payload.get("doctorName"), "医生姓名不能为空"));
        entity.setMedicineName(requireText(payload.get("medicineName"), "药品名称不能为空"));
        entity.setQuantity(blankToNull(payload.get("quantity")));
        entity.setUsageText(blankToNull(payload.get("usageText")));
        entity.setRemark(blankToNull(payload.get("remark")));
        entity.setStatus("created");
        prescriptionMapper.insert(entity);
        return toPrescriptionRow(entity);
    }

    @Override
    public PageResult<Map<String, Object>> listPrescriptions(Long patientId, Long doctorId, String status, Long recordId, int page, int limit) {
        LambdaQueryWrapper<PrescriptionEntity> wrapper = new LambdaQueryWrapper<PrescriptionEntity>().orderByDesc(PrescriptionEntity::getId);
        if (patientId != null) wrapper.eq(PrescriptionEntity::getPatientId, patientId);
        if (doctorId != null) wrapper.eq(PrescriptionEntity::getDoctorId, doctorId);
        if (blankToNull(status) != null) wrapper.eq(PrescriptionEntity::getStatus, status.trim());
        if (recordId != null) wrapper.eq(PrescriptionEntity::getRecordId, recordId);
        return PageResult.of(prescriptionMapper.selectList(wrapper).stream().map(this::toPrescriptionRow).toList(), page, limit);
    }

    @Override
    public Map<String, Object> getPrescription(Long id) {
        return toPrescriptionRow(requirePrescription(id));
    }

    @Override
    public Map<String, Object> updatePrescription(Long id, Map<String, Object> request) {
        PrescriptionEntity entity = requirePrescription(id);
        if ("cancelled".equals(entity.getStatus())) {
            throw new IllegalArgumentException("已取消处方不允许修改");
        }
        Map<String, Object> payload = safePayload(request);
        if (payload.containsKey("recordId")) entity.setRecordId(optionalId(payload.get("recordId")));
        if (payload.containsKey("medicineName")) entity.setMedicineName(requireText(payload.get("medicineName"), "药品名称不能为空"));
        if (payload.containsKey("quantity")) entity.setQuantity(blankToNull(payload.get("quantity")));
        if (payload.containsKey("usageText")) entity.setUsageText(blankToNull(payload.get("usageText")));
        if (payload.containsKey("remark")) entity.setRemark(blankToNull(payload.get("remark")));
        prescriptionMapper.updateById(entity);
        return toPrescriptionRow(entity);
    }

    @Override
    public Map<String, Object> deletePrescription(Long id) {
        PrescriptionEntity entity = requirePrescription(id);
        entity.setStatus("cancelled");
        prescriptionMapper.updateById(entity);
        return toPrescriptionRow(entity);
    }

    @Override
    public Map<String, Object> createTestRequest(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        TestRequestEntity entity = new TestRequestEntity();
        entity.setTestNo(buildBusinessNo("TEST"));
        entity.setRecordId(optionalId(payload.get("recordId")));
        entity.setPatientId(requireId(payload.get("patientId"), "患者ID不能为空"));
        entity.setPatientName(requireText(payload.get("patientName"), "患者姓名不能为空"));
        entity.setDoctorId(requireId(payload.get("doctorId"), "医生ID不能为空"));
        entity.setDoctorName(requireText(payload.get("doctorName"), "医生姓名不能为空"));
        entity.setTestItem(requireText(payload.get("testItem"), "检查项目不能为空"));
        entity.setTestReason(blankToNull(payload.get("testReason")));
        entity.setStatus("pending_audit");
        testRequestMapper.insert(entity);
        return toTestRequestRow(entity);
    }

    @Override
    public PageResult<Map<String, Object>> listTestRequests(Long patientId, Long doctorId, String status, Long recordId, int page, int limit) {
        LambdaQueryWrapper<TestRequestEntity> wrapper = new LambdaQueryWrapper<TestRequestEntity>().orderByDesc(TestRequestEntity::getId);
        if (patientId != null) wrapper.eq(TestRequestEntity::getPatientId, patientId);
        if (doctorId != null) wrapper.eq(TestRequestEntity::getDoctorId, doctorId);
        if (blankToNull(status) != null) wrapper.eq(TestRequestEntity::getStatus, status.trim());
        if (recordId != null) wrapper.eq(TestRequestEntity::getRecordId, recordId);
        return PageResult.of(testRequestMapper.selectList(wrapper).stream().map(this::toTestRequestRow).toList(), page, limit);
    }

    @Override
    public Map<String, Object> getTestRequest(Long id) {
        return toTestRequestRow(requireTestRequest(id));
    }

    @Override
    public Map<String, Object> updateTestRequest(Long id, Map<String, Object> request) {
        TestRequestEntity entity = requireTestRequest(id);
        if ("finished".equals(entity.getStatus())) {
            throw new IllegalArgumentException("检查申请已完成，不允许修改");
        }
        Map<String, Object> payload = safePayload(request);
        if (payload.containsKey("recordId")) entity.setRecordId(optionalId(payload.get("recordId")));
        if (payload.containsKey("testItem")) entity.setTestItem(requireText(payload.get("testItem"), "检查项目不能为空"));
        if (payload.containsKey("testReason")) entity.setTestReason(blankToNull(payload.get("testReason")));
        if (payload.containsKey("resultContent")) {
            entity.setResultContent(blankToNull(payload.get("resultContent")));
            if ("approved".equals(entity.getStatus()) && blankToNull(payload.get("resultContent")) != null) {
                entity.setStatus("finished");
            }
        }
        testRequestMapper.updateById(entity);
        return toTestRequestRow(entity);
    }

    @Override
    public Map<String, Object> deleteTestRequest(Long id) {
        TestRequestEntity entity = requireTestRequest(id);
        if ("finished".equals(entity.getStatus())) {
            throw new IllegalArgumentException("已完成检查申请不允许删除");
        }
        testRequestMapper.deleteById(entity.getId());
        return toTestRequestRow(entity);
    }

    @Override
    public Map<String, Object> updateTestAuditResult(Long id, Map<String, Object> request) {
        TestRequestEntity entity = requireTestRequest(id);
        if ("finished".equals(entity.getStatus())) {
            throw new IllegalArgumentException("已完成检查申请不允许审核");
        }
        Map<String, Object> payload = safePayload(request);
        entity.setStatus(requireAuditResult(payload.get("auditResult")));
        entity.setAuditOpinion(blankToNull(payload.get("auditOpinion")));
        testRequestMapper.updateById(entity);
        return toTestRequestRow(entity);
    }

    private MedicalOrderEntity requireMedicalOrder(Long id) {
        MedicalOrderEntity entity = medicalOrderMapper.selectById(requireId(id, "医嘱ID不能为空"));
        if (entity == null) throw new IllegalArgumentException("医嘱不存在");
        return entity;
    }

    private PrescriptionEntity requirePrescription(Long id) {
        PrescriptionEntity entity = prescriptionMapper.selectById(requireId(id, "处方ID不能为空"));
        if (entity == null) throw new IllegalArgumentException("处方不存在");
        return entity;
    }

    private TestRequestEntity requireTestRequest(Long id) {
        TestRequestEntity entity = testRequestMapper.selectById(requireId(id, "检查申请ID不能为空"));
        if (entity == null) throw new IllegalArgumentException("检查申请不存在");
        return entity;
    }

    private Map<String, Object> toMedicalOrderRow(MedicalOrderEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("orderNo", entity.getOrderNo());
        row.put("recordId", entity.getRecordId());
        row.put("patientId", entity.getPatientId());
        row.put("patientName", entity.getPatientName());
        row.put("doctorId", entity.getDoctorId());
        row.put("doctorName", entity.getDoctorName());
        row.put("orderType", entity.getOrderType());
        row.put("content", entity.getContent());
        row.put("status", entity.getStatus());
        row.put("auditOpinion", entity.getAuditOpinion());
        return row;
    }

    private Map<String, Object> toExecutionRow(MedicalOrderExecutionEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("orderId", entity.getOrderId());
        row.put("orderNo", entity.getOrderNo());
        row.put("nurseId", entity.getNurseId());
        row.put("nurseName", entity.getNurseName());
        row.put("executionTime", formatDateTime(entity.getExecutionTime()));
        row.put("executionResult", entity.getExecutionResult());
        row.put("remark", entity.getRemark());
        return row;
    }

    private Map<String, Object> toPrescriptionRow(PrescriptionEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("prescriptionNo", entity.getPrescriptionNo());
        row.put("recordId", entity.getRecordId());
        row.put("patientId", entity.getPatientId());
        row.put("patientName", entity.getPatientName());
        row.put("doctorId", entity.getDoctorId());
        row.put("doctorName", entity.getDoctorName());
        row.put("medicineName", entity.getMedicineName());
        row.put("quantity", entity.getQuantity());
        row.put("usageText", entity.getUsageText());
        row.put("remark", entity.getRemark());
        row.put("status", entity.getStatus());
        return row;
    }

    private Map<String, Object> toTestRequestRow(TestRequestEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("testNo", entity.getTestNo());
        row.put("recordId", entity.getRecordId());
        row.put("patientId", entity.getPatientId());
        row.put("patientName", entity.getPatientName());
        row.put("doctorId", entity.getDoctorId());
        row.put("doctorName", entity.getDoctorName());
        row.put("testItem", entity.getTestItem());
        row.put("testReason", entity.getTestReason());
        row.put("status", entity.getStatus());
        row.put("auditOpinion", entity.getAuditOpinion());
        row.put("resultContent", entity.getResultContent());
        return row;
    }

    private String buildBusinessNo(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + LocalDate.now().format(DATE_FORMATTER) + suffix;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATETIME_FORMATTER);
    }

    /**
     * Map 请求体为空时返回空 Map。
     * 这样所有必填校验都能稳定走到统一业务提示。
     */
    private Map<String, Object> safePayload(Map<String, Object> request) {
        return request == null ? Map.of() : request;
    }

    private String requireAuditResult(Object value) {
        String result = requireText(value, "审核结果不能为空");
        if (!AUDIT_RESULTS.contains(result)) {
            throw new IllegalArgumentException("审核结果只能是approved或rejected");
        }
        return result;
    }

    private Long requireId(Object value, String message) {
        Long id = optionalId(value);
        if (id == null || id <= 0) throw new IllegalArgumentException(message);
        return id;
    }

    private Long optionalId(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        String text = blankToNull(value);
        if (text == null) return null;
        try { return Long.parseLong(text); } catch (NumberFormatException exception) { throw new IllegalArgumentException("ID格式不正确"); }
    }

    private String requireText(Object value, String message) {
        String text = blankToNull(value);
        if (text == null) throw new IllegalArgumentException(message);
        return text;
    }

    private String defaultText(Object value, String defaultValue) {
        String text = blankToNull(value);
        return text == null ? defaultValue : text;
    }

    private String blankToNull(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
