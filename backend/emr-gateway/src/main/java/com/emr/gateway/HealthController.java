package com.emr.gateway;

import com.emr.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 网关健康检查接口。
 * 用于确认统一入口服务已启动，后续也可作为部署检查和演示检查入口。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("service", "emr-gateway"));
    }
}

