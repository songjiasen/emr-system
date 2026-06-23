package com.emr.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.common.PageResult;
import com.emr.visit.entity.AdmissionEntity;
import com.emr.visit.entity.DischargeEntity;
import com.emr.visit.entity.TriageRecordEntity;
import com.emr.visit.mapper.AdmissionMapper;
import com.emr.visit.mapper.DischargeMapper;
import com.emr.visit.mapper.TriageRecordMapper;
import com.emr.visit.service.InpatientService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据库版分诊建档与住院管理服务。
 * 取代原有内存 Map 实现，保证分诊、入院、出院在服务重启后仍可追溯，并统一承接关键业务校验。
 */
@Service
public class DatabaseInpatientService implements InpatientService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TriageRecordMapper triageRecordMapper;
    private final AdmissionMapper admissionMapper;
    private final DischargeMapper dischargeMapper;

    public DatabaseInpatientService(
            TriageRecordMapper triageRecordMapper,
            AdmissionMapper admissionMapper,
            DischargeMapper dischargeMapper
    ) {
        this.triageRecordMapper = triageRecordMapper;
        this.admissionMapper = admissionMapper;
        this.dischargeMapper = dischargeMapper;
    }

    /**
     * 新增分诊建档记录。
     * 当前没有网关透传的护士身份兜底，所以这里显式要求请求体带上护士主键和姓名。
     */
    @Override
    public Map<String, Object> createTriage(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);

        TriageRecordEntity entity = new TriageRecordEntity();
        entity.setTriageNo(buildBusinessNo("TRIAGE"));
        entity.setAppointmentId(optionalId(payload.get("appointmentId")));
        entity.setPatientId(requireId(payload.get("patientId"), "患者ID不能为空"));
        entity.setPatientName(requireText(payload.get("patientName"), "患者姓名不能为空"));
        entity.setNurseId(requireId(payload.get("nurseId"), "护士ID不能为空"));
        entity.setNurseName(requireText(payload.get("nurseName"), "护士姓名不能为空"));
        entity.setChiefComplaint(blankToNull(payload.get("chiefComplaint")));
        entity.setTriageLevel(defaultText(payload.get("triageLevel"), "normal"));
        entity.setStatus("created");
        triageRecordMapper.insert(entity);
        return toTriageRow(entity);
    }

    /**
     * 查询分诊建档列表。
     * 结果按主键倒序返回，兼容现有列表页默认看最新记录的体验。
     */
    @Override
    public PageResult<Map<String, Object>> listTriage(Long patientId, Long nurseId, String status, String triageLevel, int page, int limit) {
        LambdaQueryWrapper<TriageRecordEntity> wrapper = new LambdaQueryWrapper<TriageRecordEntity>()
                .orderByDesc(TriageRecordEntity::getId);
        if (patientId != null) {
            wrapper.eq(TriageRecordEntity::getPatientId, patientId);
        }
        if (nurseId != null) {
            wrapper.eq(TriageRecordEntity::getNurseId, nurseId);
        }
        if (blankToNull(status) != null) {
            wrapper.eq(TriageRecordEntity::getStatus, status.trim());
        }
        if (blankToNull(triageLevel) != null) {
            wrapper.eq(TriageRecordEntity::getTriageLevel, triageLevel.trim());
        }
        List<Map<String, Object>> rows = triageRecordMapper.selectList(wrapper).stream()
                .map(this::toTriageRow)
                .toList();
        return PageResult.of(rows, page, limit);
    }

    /**
     * 新增入院登记。
     * 当病房和床位同时传入时，需要先拦住已被住院记录占用的床位，避免一床多住院。
     */
    @Override
    public Map<String, Object> createAdmission(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        String wardNo = blankToNull(payload.get("wardNo"));
        String bedNo = blankToNull(payload.get("bedNo"));
        if (wardNo != null && bedNo != null) {
            long occupiedCount = admissionMapper.selectCount(new LambdaQueryWrapper<AdmissionEntity>()
                    .eq(AdmissionEntity::getWardNo, wardNo)
                    .eq(AdmissionEntity::getBedNo, bedNo)
                    .eq(AdmissionEntity::getStatus, "in_hospital"));
            if (occupiedCount > 0) {
                throw new IllegalArgumentException("当前床位已被占用");
            }
        }

        AdmissionEntity entity = new AdmissionEntity();
        entity.setAdmissionNo(buildBusinessNo("ADM"));
        entity.setPatientId(requireId(payload.get("patientId"), "患者ID不能为空"));
        entity.setPatientName(requireText(payload.get("patientName"), "患者姓名不能为空"));
        entity.setDoctorId(optionalId(payload.get("doctorId")));
        entity.setDoctorName(blankToNull(payload.get("doctorName")));
        entity.setNurseId(optionalId(payload.get("nurseId")));
        entity.setNurseName(blankToNull(payload.get("nurseName")));
        entity.setWardNo(wardNo);
        entity.setBedNo(bedNo);
        entity.setAdmissionTime(parseDateTime(payload.get("admissionTime"), "入院时间不能为空", "入院时间格式不正确"));
        entity.setReason(blankToNull(payload.get("reason")));
        entity.setStatus("in_hospital");
        admissionMapper.insert(entity);
        return toAdmissionRow(entity);
    }

    /**
     * 查询入院列表。
     * 支持以患者、状态、病房和床位作为最小过滤集合，便于护士站快速定位住院记录。
     */
    @Override
    public PageResult<Map<String, Object>> listAdmissions(Long patientId, String status, String wardNo, String bedNo, int page, int limit) {
        LambdaQueryWrapper<AdmissionEntity> wrapper = new LambdaQueryWrapper<AdmissionEntity>()
                .orderByDesc(AdmissionEntity::getId);
        if (patientId != null) {
            wrapper.eq(AdmissionEntity::getPatientId, patientId);
        }
        if (blankToNull(status) != null) {
            wrapper.eq(AdmissionEntity::getStatus, status.trim());
        }
        if (blankToNull(wardNo) != null) {
            wrapper.eq(AdmissionEntity::getWardNo, wardNo.trim());
        }
        if (blankToNull(bedNo) != null) {
            wrapper.eq(AdmissionEntity::getBedNo, bedNo.trim());
        }
        List<Map<String, Object>> rows = admissionMapper.selectList(wrapper).stream()
                .map(this::toAdmissionRow)
                .toList();
        return PageResult.of(rows, page, limit);
    }

    /**
     * 办理出院。
     * 成功后会同步把入院记录标记为已出院，保证住院表和出院表口径一致。
     */
    @Override
    public Map<String, Object> discharge(Long admissionId, Map<String, Object> request) {
        AdmissionEntity admission = requireAdmission(admissionId);
        if (dischargeMapper.selectCount(new LambdaQueryWrapper<DischargeEntity>()
                .eq(DischargeEntity::getAdmissionId, admission.getId())) > 0) {
            throw new IllegalArgumentException("当前入院记录已办理出院");
        }
        if (!"in_hospital".equals(admission.getStatus())) {
            throw new IllegalArgumentException("当前入院记录不在住院中");
        }

        Map<String, Object> payload = safePayload(request);
        DischargeEntity entity = new DischargeEntity();
        entity.setDischargeNo(buildBusinessNo("DIS"));
        entity.setAdmissionId(admission.getId());
        entity.setPatientId(admission.getPatientId());
        entity.setPatientName(admission.getPatientName());
        entity.setDischargeTime(parseDateTime(payload.get("dischargeTime"), "出院时间不能为空", "出院时间格式不正确"));
        entity.setDischargeReason(blankToNull(payload.get("dischargeReason")));
        entity.setDischargeSummary(blankToNull(payload.get("dischargeSummary")));
        entity.setOperatorId(optionalId(payload.get("operatorId")));
        entity.setOperatorName(blankToNull(payload.get("operatorName")));
        dischargeMapper.insert(entity);

        admission.setStatus("discharged");
        admissionMapper.updateById(admission);
        return toDischargeRow(entity);
    }

    /**
     * 查询出院列表。
     * 返回结构补齐 `businessNo` 和 `status`，让现有前端无需区分底层是否有独立出院表。
     */
    @Override
    public PageResult<Map<String, Object>> listDischarges(Long patientId, Long admissionId, int page, int limit) {
        LambdaQueryWrapper<DischargeEntity> wrapper = new LambdaQueryWrapper<DischargeEntity>()
                .orderByDesc(DischargeEntity::getId);
        if (patientId != null) {
            wrapper.eq(DischargeEntity::getPatientId, patientId);
        }
        if (admissionId != null) {
            wrapper.eq(DischargeEntity::getAdmissionId, admissionId);
        }
        List<Map<String, Object>> rows = dischargeMapper.selectList(wrapper).stream()
                .map(this::toDischargeRow)
                .toList();
        return PageResult.of(rows, page, limit);
    }

    private AdmissionEntity requireAdmission(Long admissionId) {
        Long validAdmissionId = requireId(admissionId, "入院记录ID不能为空");
        AdmissionEntity admission = admissionMapper.selectById(validAdmissionId);
        if (admission == null) {
            throw new IllegalArgumentException("入院记录不存在");
        }
        return admission;
    }

    private Map<String, Object> toTriageRow(TriageRecordEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("businessNo", entity.getTriageNo());
        row.put("appointmentId", entity.getAppointmentId());
        row.put("patientId", entity.getPatientId());
        row.put("patientName", entity.getPatientName());
        row.put("nurseId", entity.getNurseId());
        row.put("nurseName", entity.getNurseName());
        row.put("chiefComplaint", entity.getChiefComplaint());
        row.put("triageLevel", entity.getTriageLevel());
        row.put("status", entity.getStatus());
        return row;
    }

    private Map<String, Object> toAdmissionRow(AdmissionEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("businessNo", entity.getAdmissionNo());
        row.put("patientId", entity.getPatientId());
        row.put("patientName", entity.getPatientName());
        row.put("doctorId", entity.getDoctorId());
        row.put("doctorName", entity.getDoctorName());
        row.put("nurseId", entity.getNurseId());
        row.put("nurseName", entity.getNurseName());
        row.put("wardNo", entity.getWardNo());
        row.put("bedNo", entity.getBedNo());
        row.put("admissionTime", formatDateTime(entity.getAdmissionTime()));
        row.put("reason", entity.getReason());
        row.put("status", entity.getStatus());
        return row;
    }

    private Map<String, Object> toDischargeRow(DischargeEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("businessNo", entity.getDischargeNo());
        row.put("admissionId", entity.getAdmissionId());
        row.put("patientId", entity.getPatientId());
        row.put("patientName", entity.getPatientName());
        row.put("dischargeTime", formatDateTime(entity.getDischargeTime()));
        row.put("dischargeReason", entity.getDischargeReason());
        row.put("dischargeSummary", entity.getDischargeSummary());
        row.put("operatorId", entity.getOperatorId());
        row.put("operatorName", entity.getOperatorName());
        row.put("status", "discharged");
        return row;
    }

    private String buildBusinessNo(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + LocalDate.now().format(DATE_FORMATTER) + suffix;
    }

    private LocalDateTime parseDateTime(Object value, String emptyMessage, String formatMessage) {
        String text = requireText(value, emptyMessage);
        try {
            return LocalDateTime.parse(text, DATETIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(formatMessage);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATETIME_FORMATTER);
    }

    /**
     * Map 请求体为空时返回空 Map。
     * 这样后续所有必填校验都能稳定走到统一的业务异常分支。
     */
    private Map<String, Object> safePayload(Map<String, Object> request) {
        return request == null ? Map.of() : request;
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

    private String defaultText(Object value, String defaultValue) {
        String text = blankToNull(value);
        return text == null ? defaultValue : text;
    }

    private String blankToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
