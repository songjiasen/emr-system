package com.emr.system.controller;

import com.emr.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统内容服务健康检查接口。
 * 用于确认资讯、留言、菜单、配置和日志服务已启动。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("service", "emr-system-service"));
    }
}

