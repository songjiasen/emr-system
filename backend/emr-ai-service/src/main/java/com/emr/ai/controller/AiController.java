package com.emr.ai.controller;

import com.emr.ai.service.AiService;
import com.emr.common.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 智能接口。
 * 当前使用 mock provider 保证期末演示不依赖外部百度 AI Key；后续可替换为真实 SDK 调用。
 */
@RestController
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ai/ocr")
    public ApiResponse<Map<String, Object>> ocr(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(aiService.ocr(request));
    }

    @PostMapping("/ai/recommend-medicine")
    public ApiResponse<Map<String, Object>> recommendMedicine(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(aiService.recommendMedicine(request));
    }

    @PostMapping("/ai/prescription-audit")
    public ApiResponse<Map<String, Object>> prescriptionAudit(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(aiService.prescriptionAudit(request));
    }

    @PostMapping("/ai/smart-search")
    public ApiResponse<Map<String, Object>> smartSearch(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(aiService.smartSearch(request));
    }

    /**
     * AI 模块参数异常处理。
     * 统一返回 code=400，前端可以直接展示具体错误原因。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }
}
