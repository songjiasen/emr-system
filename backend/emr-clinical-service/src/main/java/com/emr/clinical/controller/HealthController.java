package com.emr.clinical.controller;

import com.emr.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 诊疗服务健康检查接口。
 * 用于确认医嘱、处方、检查申请和执行记录服务已启动。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("service", "emr-clinical-service"));
    }
}

