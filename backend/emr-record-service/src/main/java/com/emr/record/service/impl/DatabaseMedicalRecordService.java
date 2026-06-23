package com.emr.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.common.PageResult;
import com.emr.record.dto.MedicalRecordCreateRequest;
import com.emr.record.entity.MedicalRecordEntity;
import com.emr.record.mapper.MedicalRecordMapper;
import com.emr.record.service.MedicalRecordService;
import com.emr.record.vo.MedicalRecordResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * 数据库版病历服务。
 * 用 `binglixinxi` 表替代原有内存实现，保证病历创建、查询、更新和删除都能跨重启保留。
 */
@Service
public class DatabaseMedicalRecordService implements MedicalRecordService {

    private static final String ARCHIVE_STATUS_NOT_SUBMITTED = "not_submitted";
    private static final String ARCHIVE_STATUS_ARCHIVED = "archived";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MedicalRecordMapper medicalRecordMapper;

    public DatabaseMedicalRecordService(MedicalRecordMapper medicalRecordMapper) {
        this.medicalRecordMapper = medicalRecordMapper;
    }

    /**
     * 创建病历。
     * 新病历默认未提交归档，后续归档申请流程从 `not_submitted` 状态进入。
     */
    @Override
    public MedicalRecordResponse createRecord(MedicalRecordCreateRequest request) {
        Long patientId = requireId(request == null ? null : request.patientId(), "患者ID不能为空");
        String patientName = requireText(request == null ? null : request.patientName(), "患者姓名不能为空");
        Long doctorId = requireId(request == null ? null : request.doctorId(), "医生ID不能为空");
        String doctorName = requireText(request == null ? null : request.doctorName(), "医生姓名不能为空");
        LocalDateTime visitTime = parseDateTime(request == null ? null : request.visitTime(), "就诊时间不能为空", "就诊时间格式不正确");

        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setRecordNo(buildRecordNo());
        entity.setAppointmentId(request == null ? null : request.appointmentId());
        entity.setAppointmentNo(blankToNull(request == null ? null : request.appointmentNo()));
        entity.setPatientId(patientId);
        entity.setPatientName(patientName);
        entity.setDoctorId(doctorId);
        entity.setDoctorName(doctorName);
        entity.setVisitTime(visitTime);
        entity.setChiefComplaint(blankToNull(request == null ? null : request.chiefComplaint()));
        entity.setPresentIllness(blankToNull(request == null ? null : request.presentIllness()));
        entity.setPastHistory(blankToNull(request == null ? null : request.pastHistory()));
        entity.setDiagnosis(blankToNull(request == null ? null : request.diagnosis()));
        entity.setTreatmentAdvice(blankToNull(request == null ? null : request.treatmentAdvice()));
        entity.setFileUrl(blankToNull(request == null ? null : request.fileUrl()));
        entity.setArchiveStatus(ARCHIVE_STATUS_NOT_SUBMITTED);
        medicalRecordMapper.insert(entity);
        return toResponse(entity);
    }

    /**
     * 查询病历列表。
     * 继续支持按患者、医生、归档状态过滤，并按最新病历优先返回。
     */
    @Override
    public PageResult<MedicalRecordResponse> listRecords(Long patientId, Long doctorId, String archiveStatus, int page, int limit) {
        LambdaQueryWrapper<MedicalRecordEntity> wrapper = new LambdaQueryWrapper<MedicalRecordEntity>()
                .orderByDesc(MedicalRecordEntity::getId);
        if (patientId != null) {
            wrapper.eq(MedicalRecordEntity::getPatientId, patientId);
        }
        if (doctorId != null) {
            wrapper.eq(MedicalRecordEntity::getDoctorId, doctorId);
        }
        if (archiveStatus != null && !archiveStatus.isBlank()) {
            wrapper.eq(MedicalRecordEntity::getArchiveStatus, archiveStatus.trim());
        }
        List<MedicalRecordResponse> rows = medicalRecordMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(rows, page, limit);
    }

    /**
     * 查询病历详情。
     * 病历不存在时返回明确业务错误，避免后续诊疗和归档流程拿到空数据。
     */
    @Override
    public MedicalRecordResponse getRecord(Long id) {
        return toResponse(requireRecord(id));
    }

    /**
     * 更新病历。
     * 已归档病历禁止修改，其他状态只覆盖请求里显式传入的正文和预约关联字段。
     */
    @Override
    public MedicalRecordResponse updateRecord(Long id, MedicalRecordCreateRequest request) {
        MedicalRecordEntity entity = requireRecord(id);
        if (ARCHIVE_STATUS_ARCHIVED.equals(entity.getArchiveStatus())) {
            throw new IllegalArgumentException("已归档病历不允许修改");
        }
        if (request == null) {
            return toResponse(entity);
        }

        if (request.appointmentId() != null) {
            entity.setAppointmentId(request.appointmentId());
        }
        if (request.appointmentNo() != null) {
            entity.setAppointmentNo(blankToNull(request.appointmentNo()));
        }
        if (request.patientId() != null) {
            entity.setPatientId(requireId(request.patientId(), "患者ID不能为空"));
        }
        if (request.patientName() != null) {
            entity.setPatientName(requireText(request.patientName(), "患者姓名不能为空"));
        }
        if (request.doctorId() != null) {
            entity.setDoctorId(requireId(request.doctorId(), "医生ID不能为空"));
        }
        if (request.doctorName() != null) {
            entity.setDoctorName(requireText(request.doctorName(), "医生姓名不能为空"));
        }
        if (request.visitTime() != null) {
            entity.setVisitTime(parseDateTime(request.visitTime(), "就诊时间不能为空", "就诊时间格式不正确"));
        }
        if (request.chiefComplaint() != null) {
            entity.setChiefComplaint(blankToNull(request.chiefComplaint()));
        }
        if (request.presentIllness() != null) {
            entity.setPresentIllness(blankToNull(request.presentIllness()));
        }
        if (request.pastHistory() != null) {
            entity.setPastHistory(blankToNull(request.pastHistory()));
        }
        if (request.diagnosis() != null) {
            entity.setDiagnosis(blankToNull(request.diagnosis()));
        }
        if (request.treatmentAdvice() != null) {
            entity.setTreatmentAdvice(blankToNull(request.treatmentAdvice()));
        }
        if (request.fileUrl() != null) {
            entity.setFileUrl(blankToNull(request.fileUrl()));
        }

        medicalRecordMapper.updateById(entity);
        return toResponse(entity);
    }

    /**
     * 删除病历。
     * 已进入归档流程的病历不允许删除，避免归档记录和病历主表脱节。
     */
    @Override
    public MedicalRecordResponse deleteRecord(Long id) {
        MedicalRecordEntity entity = requireRecord(id);
        if (!ARCHIVE_STATUS_NOT_SUBMITTED.equals(entity.getArchiveStatus())) {
            throw new IllegalArgumentException("当前病历已有归档流程，不允许删除");
        }
        medicalRecordMapper.deleteById(entity.getId());
        return toResponse(entity);
    }

    private MedicalRecordEntity requireRecord(Long id) {
        Long recordId = requireId(id, "病历ID不能为空");
        MedicalRecordEntity entity = medicalRecordMapper.selectById(recordId);
        if (entity == null) {
            throw new IllegalArgumentException("病历记录不存在");
        }
        return entity;
    }

    private MedicalRecordResponse toResponse(MedicalRecordEntity entity) {
        return new MedicalRecordResponse(
                entity.getId(),
                entity.getRecordNo(),
                entity.getAppointmentId(),
                entity.getAppointmentNo(),
                entity.getPatientId(),
                entity.getPatientName(),
                entity.getDoctorId(),
                entity.getDoctorName(),
                formatDateTime(entity.getVisitTime()),
                entity.getChiefComplaint(),
                entity.getPresentIllness(),
                entity.getPastHistory(),
                entity.getDiagnosis(),
                entity.getTreatmentAdvice(),
                entity.getFileUrl(),
                entity.getArchiveStatus()
        );
    }

    private String buildRecordNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MR" + LocalDate.now().format(DATE_FORMATTER) + suffix;
    }

    private LocalDateTime parseDateTime(String value, String emptyMessage, String formatMessage) {
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

    private Long requireId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String requireText(String value, String message) {
        String text = blankToNull(value);
        if (text == null) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
