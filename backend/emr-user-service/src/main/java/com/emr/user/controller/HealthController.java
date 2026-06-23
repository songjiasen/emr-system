package com.emr.user.controller;

import com.emr.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户科室服务健康检查接口。
 * 用于确认用户资料和科室资料服务已启动。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("service", "emr-user-service"));
    }
}

