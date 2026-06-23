package com.emr.record.controller;

import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.common.TrustedUserContext;
import com.emr.record.dto.MedicalRecordCreateRequest;
import com.emr.record.service.MedicalRecordService;
import com.emr.record.vo.MedicalRecordResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 病历管理接口。
 * 支撑医生录入病历、医生查看病历列表、患者查看自己的病历。
 */
@RestController
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    /**
     * 新增病历。
     * 医生接诊后调用，成功后返回自动生成的病历编号。
     */
    @PostMapping("/medical-records")
    public ApiResponse<MedicalRecordResponse> createRecord(
            @RequestBody MedicalRecordCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        if (context.isDoctor() && request != null && request.doctorId() != null && !context.matchesUserId(request.doctorId())) {
            throw new IllegalArgumentException("当前登录医生只能为自己创建病历");
        }
        return ApiResponse.success(medicalRecordService.createRecord(request));
    }

    /**
     * 查询病历列表。
     * 患者端传 patientId，医生端传 doctorId，后台端可按归档状态筛选。
     */
    @GetMapping("/medical-records")
    public ApiResponse<PageResult<MedicalRecordResponse>> listRecords(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "archiveStatus", required = false) String archiveStatus,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        Long scopedPatientId = patientId;
        Long scopedDoctorId = doctorId;
        if (context.isPatient()) {
            if (patientId != null && !context.matchesUserId(patientId)) {
                throw new IllegalArgumentException("当前登录患者只能查询自己的病历");
            }
            scopedPatientId = context.userId();
        }
        if (context.isDoctor()) {
            if (doctorId != null && !context.matchesUserId(doctorId)) {
                throw new IllegalArgumentException("当前登录医生只能查询自己的病历");
            }
            scopedDoctorId = context.userId();
        }
        return ApiResponse.success(medicalRecordService.listRecords(scopedPatientId, scopedDoctorId, archiveStatus, page, limit));
    }

    /**
     * 查询病历详情。
     * 用于患者查看完整病历内容，也用于医嘱、归档等后续流程读取诊断信息。
     */
    @GetMapping("/medical-records/{id}")
    public ApiResponse<MedicalRecordResponse> getRecord(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        MedicalRecordResponse response = medicalRecordService.getRecord(id);
        ensureRecordAccess(context, response, "查看");
        return ApiResponse.success(response);
    }

    /**
     * 编辑病历。
     * 允许医生补充诊断、既往史和诊疗医嘱等字段。
     */
    @PutMapping("/medical-records/{id}")
    public ApiResponse<MedicalRecordResponse> updateRecord(
            @PathVariable("id") Long id,
            @RequestBody MedicalRecordCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        MedicalRecordResponse current = medicalRecordService.getRecord(id);
        ensureRecordAccess(context, current, "操作");
        if (context.isDoctor() && request != null && request.doctorId() != null && !context.matchesUserId(request.doctorId())) {
            throw new IllegalArgumentException("当前登录医生只能维护自己的病历");
        }
        return ApiResponse.success(medicalRecordService.updateRecord(id, request));
    }

    /**
     * 删除病历。
     * 当前按需求执行物理删除；调用前先按可信身份头校验病历归属。
     */
    @DeleteMapping("/medical-records/{id}")
    public ApiResponse<MedicalRecordResponse> deleteRecord(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        MedicalRecordResponse current = medicalRecordService.getRecord(id);
        ensureRecordAccess(context, current, "操作");
        return ApiResponse.success(medicalRecordService.deleteRecord(id));
    }

    /**
     * 病历文件上传演示接口。
     * 当前返回可展示的文件访问路径，后续接 MultipartFile 后写入 storage/uploads。
     */
    @PostMapping("/medical-records/upload")
    public ApiResponse<String> uploadRecordFile() {
        return ApiResponse.success("/uploads/medical-record-demo.pdf");
    }

    /**
     * 病历模块参数异常处理。
     * 统一返回 code=400，前端可直接展示 message。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    private void ensureRecordAccess(TrustedUserContext context, MedicalRecordResponse response, String action) {
        if (context.isPatient() && !context.matchesUserId(response.patientId())) {
            throw new IllegalArgumentException("当前登录患者无权" + action + "该病历");
        }
        if (context.isDoctor() && !context.matchesUserId(response.doctorId())) {
            throw new IllegalArgumentException("当前登录医生无权" + action + "该病历");
        }
    }
}
