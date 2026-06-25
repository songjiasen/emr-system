package com.emr.visit.controller;

import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.common.TrustedUserContext;
import com.emr.visit.dto.AppointmentCancelRequest;
import com.emr.visit.dto.AppointmentCreateRequest;
import com.emr.visit.service.AppointmentService;
import com.emr.visit.vo.AppointmentResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预约挂号接口。
 * 支撑患者端预约挂号、我的预约，以及后台端预约管理的基础能力。
 */
@RestController
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * 新增预约挂号。
     * 创建成功后返回系统自动生成的预约编号，后续医生接诊和病历创建可引用该编号。
     */
    @PostMapping("/appointments")
    public ApiResponse<AppointmentResponse> createAppointment(
            @RequestBody AppointmentCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        if (context.isPatient() && request != null && request.patientId() != null && !context.matchesUserId(request.patientId())) {
            throw new IllegalArgumentException("当前登录患者只能为自己预约");
        }
        return ApiResponse.success(appointmentService.createAppointment(request));
    }

    /**
     * 查询预约列表。
     * 支持按患者、医生、状态过滤，默认每页 10 条。
     */
    @GetMapping("/appointments")
    public ApiResponse<PageResult<AppointmentResponse>> listAppointments(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName,
            @RequestHeader(value = "X-Department-Id", required = false) String departmentIdHeader,
            @RequestHeader(value = "X-Department-Name", required = false) String departmentName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName, departmentIdHeader, departmentName);
        Long scopedPatientId = patientId;
        Long scopedDoctorId = doctorId;
        Long scopedDepartmentId = departmentId;
        if (context.isPatient()) {
            if (patientId != null && !context.matchesUserId(patientId)) {
                throw new IllegalArgumentException("当前登录患者只能查询自己的预约");
            }
            scopedPatientId = context.userId();
        }
        if (context.isDoctor()) {
            if (doctorId != null && !context.matchesUserId(doctorId)) {
                throw new IllegalArgumentException("当前登录医生只能查询自己的预约");
            }
            scopedDoctorId = context.userId();
        }
        if (context.isNurse()) {
            if (context.departmentId() == null) {
                throw new IllegalArgumentException("当前登录护士未绑定科室");
            }
            scopedDepartmentId = context.departmentId();
        }
        return ApiResponse.success(appointmentService.listAppointments(scopedPatientId, scopedDoctorId, scopedDepartmentId, status, page, limit));
    }

    /**
     * 查询预约详情。
     * 用于患者查看预约结果和后台查看预约记录。
     */
    @GetMapping("/appointments/{id}")
    public ApiResponse<AppointmentResponse> getAppointment(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        AppointmentResponse response = appointmentService.getAppointment(id);
        ensureAppointmentAccess(context, response, "查看");
        return ApiResponse.success(response);
    }

    /**
     * 取消预约。
     * 当前由患者端触发，后续可结合 Token 校验限制只能取消自己的预约。
     */
    @PostMapping("/appointments/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancelAppointment(
            @PathVariable("id") Long id,
            @RequestBody(required = false) AppointmentCancelRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        AppointmentResponse current = appointmentService.getAppointment(id);
        ensureAppointmentAccess(context, current, "操作");
        return ApiResponse.success(appointmentService.cancelAppointment(id, request));
    }

    /**
     * 预约模块参数异常处理。
     * 统一返回 code=400，前端可以直接展示 message。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    private void ensureAppointmentAccess(TrustedUserContext context, AppointmentResponse response, String action) {
        if (context.isPatient() && !context.matchesUserId(response.patientId())) {
            throw new IllegalArgumentException("当前登录患者无权" + action + "该预约");
        }
        if (context.isDoctor() && !context.matchesUserId(response.doctorId())) {
            throw new IllegalArgumentException("当前登录医生无权" + action + "该预约");
        }
    }
}
