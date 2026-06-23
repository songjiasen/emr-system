package com.emr.workflow.controller;

import com.emr.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工作流服务健康检查接口。
 * 用于确认病历审核、医嘱审核、检查审核和归档审核服务已启动。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("service", "emr-workflow-service"));
    }
}

