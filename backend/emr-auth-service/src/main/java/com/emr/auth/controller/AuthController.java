package com.emr.auth.controller;

import com.emr.auth.dto.LoginRequest;
import com.emr.auth.dto.PatientRegisterRequest;
import com.emr.auth.service.AuthService;
import com.emr.auth.vo.LoginResponse;
import com.emr.auth.vo.PatientRegisterResponse;
import com.emr.common.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口。
 * 对外提供登录和患者注册入口，路径先按模块语义固定为 /auth/*。
 */
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录接口。
     * 登录成功后返回 Token、角色和来源表，前端据此保存登录态并跳转对应端。
     */
    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 患者注册接口。
     * 注册成功后默认创建 patient 角色账号，并写入 `emr_user.huanzhe` 表。
     */
    @PostMapping("/auth/patient/register")
    public ApiResponse<PatientRegisterResponse> registerPatient(@RequestBody PatientRegisterRequest request) {
        return ApiResponse.success(authService.registerPatient(request));
    }

    /**
     * Token 校验接口。
     * 网关和前端可用它确认当前登录态是否仍有效。
     */
    @PostMapping("/auth/token/validate")
    public ApiResponse<LoginResponse> validateToken(@RequestHeader(value = "Token", required = false) String token) {
        return ApiResponse.success(authService.validateToken(token));
    }

    /**
     * 修改密码接口。
     * 通过账号、旧密码和新密码更新对应账号表密码，同时清理该账号历史 Token。
     */
    @PostMapping("/auth/password/change")
    public ApiResponse<String> changePassword(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = request == null ? Map.of() : request;
        return ApiResponse.success(authService.changePassword(
                valueOf(payload.get("username")),
                valueOf(payload.get("oldPassword")),
                valueOf(payload.get("newPassword"))
        ));
    }

    /**
     * 忘记密码重置接口。
     * 当前以账号和手机号作为找回条件，后续可扩展短信验证码。
     */
    @PostMapping("/auth/password/reset")
    public ApiResponse<String> resetPassword(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = request == null ? Map.of() : request;
        return ApiResponse.success(authService.resetPassword(
                valueOf(payload.get("username")),
                valueOf(payload.get("phone")),
                valueOf(payload.get("newPassword"))
        ));
    }

    /**
     * 注销接口。
     * 当前通过请求头 Token 移除登录态。
     */
    @PostMapping("/auth/logout")
    public ApiResponse<String> logout(@RequestHeader(value = "Token", required = false) String token) {
        return ApiResponse.success(authService.logout(token));
    }

    /**
     * 认证模块参数异常处理。
     * 当前先把必填字段错误返回给前端，避免空参数继续进入主流程。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    private String valueOf(Object value) {
        return value == null ? null : value.toString();
    }
}
