package com.emr.auth.service;

import com.emr.auth.dto.LoginRequest;
import com.emr.auth.dto.PatientRegisterRequest;
import com.emr.auth.vo.LoginResponse;
import com.emr.auth.vo.PatientRegisterResponse;

/**
 * 认证服务接口。
 * 先定义登录和患者注册两个 P0 能力，后续再扩展修改密码、忘记密码和 Token 校验。
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    PatientRegisterResponse registerPatient(PatientRegisterRequest request);

    LoginResponse validateToken(String token);

    String changePassword(String username, String oldPassword, String newPassword);

    String resetPassword(String username, String phone, String newPassword);

    String logout(String token);
}
