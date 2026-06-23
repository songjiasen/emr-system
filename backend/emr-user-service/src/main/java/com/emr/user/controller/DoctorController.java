package com.emr.user.controller;

import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.user.service.DoctorService;
import com.emr.user.vo.DoctorSummaryResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医生资料接口。
 * 支撑患者端选择医生预约挂号，以及后台管理端查看医生详情。
 */
@RestController
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * 医生列表接口。
     * page/limit 默认遵循需求文档的每页 10 条规则。
     */
    @GetMapping("/doctors")
    public ApiResponse<PageResult<DoctorSummaryResponse>> listDoctors(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ApiResponse.success(doctorService.listDoctors(page, limit));
    }

    /**
     * 医生详情接口。
     * 预约挂号前可用它展示医生简介和擅长领域。
     */
    @GetMapping("/doctors/{id}")
    public ApiResponse<DoctorSummaryResponse> getDoctor(@PathVariable("id") Long id) {
        return ApiResponse.success(doctorService.getDoctor(id));
    }

    /**
     * 用户服务参数异常处理。
     * 把 ID 缺失、医生不存在等问题直接反馈给前端。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }
}
