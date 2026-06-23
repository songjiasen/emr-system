package com.emr.visit.controller;

import com.emr.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 就诊流程服务健康检查接口。
 * 用于确认预约、分诊、入院、出院等就诊流程服务已启动。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("service", "emr-visit-service"));
    }
}

