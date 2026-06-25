package com.emr.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.common.PageResult;
import com.emr.visit.dto.AppointmentCancelRequest;
import com.emr.visit.dto.AppointmentCreateRequest;
import com.emr.visit.entity.AppointmentEntity;
import com.emr.visit.mapper.AppointmentMapper;
import com.emr.visit.service.AppointmentService;
import com.emr.visit.vo.AppointmentResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * 数据库版预约挂号服务。
 * 用 `yuyueguahao` 表替代原有内存实现，保证预约创建、查询和取消在服务重启后仍可追溯。
 */
@Service
public class DatabaseAppointmentService implements AppointmentService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppointmentMapper appointmentMapper;

    public DatabaseAppointmentService(AppointmentMapper appointmentMapper) {
        this.appointmentMapper = appointmentMapper;
    }

    /**
     * 创建预约并持久化。
     * 预约状态保持 `pending` 初始值，兼容后续确认、取消和完成等流程流转。
     */
    @Override
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        Long patientId = requireId(request == null ? null : request.patientId(), "患者ID不能为空");
        String patientName = requireText(request == null ? null : request.patientName(), "患者姓名不能为空");
        Long doctorId = requireId(request == null ? null : request.doctorId(), "医生ID不能为空");
        String doctorName = requireText(request == null ? null : request.doctorName(), "医生姓名不能为空");
        LocalDateTime appointmentTime = parseAppointmentTime(request == null ? null : request.appointmentTime());

        AppointmentEntity entity = new AppointmentEntity();
        entity.setAppointmentNo(buildAppointmentNo());
        entity.setPatientId(patientId);
        entity.setPatientName(patientName);
        entity.setDoctorId(doctorId);
        entity.setDoctorName(doctorName);
        entity.setDepartmentId(request == null ? null : request.departmentId());
        entity.setDepartmentName(request == null ? null : blankToNull(request.departmentName()));
        entity.setAppointmentTime(appointmentTime);
        entity.setStatus("pending");
        entity.setCancelReason(null);
        entity.setRemark(request == null ? null : blankToNull(request.remark()));
        appointmentMapper.insert(entity);
        return toResponse(entity);
    }

    /**
     * 查询预约列表。
     * 保持患者、医生、状态三个可选过滤条件，并按创建顺序倒序返回，兼容现有列表体验。
     */
    @Override
    public PageResult<AppointmentResponse> listAppointments(Long patientId, Long doctorId, Long departmentId, String status, int page, int limit) {
        LambdaQueryWrapper<AppointmentEntity> wrapper = new LambdaQueryWrapper<AppointmentEntity>()
                .orderByDesc(AppointmentEntity::getId);
        if (patientId != null) {
            wrapper.eq(AppointmentEntity::getPatientId, patientId);
        }
        if (doctorId != null) {
            wrapper.eq(AppointmentEntity::getDoctorId, doctorId);
        }
        if (departmentId != null) {
            wrapper.eq(AppointmentEntity::getDepartmentId, departmentId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AppointmentEntity::getStatus, status.trim());
        }
        List<AppointmentResponse> rows = appointmentMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(rows, page, limit);
    }

    /**
     * 查询预约详情。
     * 未找到预约时显式报错，避免后续病历和收费流程引用不存在的预约记录。
     */
    @Override
    public AppointmentResponse getAppointment(Long id) {
        return toResponse(requireAppointment(id));
    }

    /**
     * 取消预约并落库取消原因。
     * 已完成预约仍然禁止取消，保持原有业务约束不变。
     */
    @Override
    public AppointmentResponse cancelAppointment(Long id, AppointmentCancelRequest request) {
        AppointmentEntity entity = requireAppointment(id);
        if ("finished".equals(entity.getStatus())) {
            throw new IllegalArgumentException("已完成预约不能取消");
        }
        entity.setStatus("cancelled");
        entity.setCancelReason(request == null ? null : blankToNull(request.cancelReason()));
        appointmentMapper.updateById(entity);
        return toResponse(entity);
    }

    private AppointmentEntity requireAppointment(Long id) {
        Long appointmentId = requireId(id, "预约ID不能为空");
        AppointmentEntity entity = appointmentMapper.selectById(appointmentId);
        if (entity == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        return entity;
    }

    private AppointmentResponse toResponse(AppointmentEntity entity) {
        return new AppointmentResponse(
                entity.getId(),
                entity.getAppointmentNo(),
                entity.getPatientId(),
                entity.getPatientName(),
                entity.getDoctorId(),
                entity.getDoctorName(),
                entity.getDepartmentId(),
                entity.getDepartmentName(),
                formatAppointmentTime(entity.getAppointmentTime()),
                entity.getStatus(),
                entity.getCancelReason(),
                entity.getRemark()
        );
    }

    private String buildAppointmentNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "APPT" + LocalDate.now().format(DATE_FORMATTER) + suffix;
    }

    private LocalDateTime parseAppointmentTime(String value) {
        String text = requireText(value, "预约时间不能为空");
        try {
            return LocalDateTime.parse(text, DATETIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("预约时间格式不正确");
        }
    }

    private String formatAppointmentTime(LocalDateTime value) {
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
