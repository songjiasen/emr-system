package com.emr.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.emr.auth.dto.LoginRequest;
import com.emr.auth.dto.PatientRegisterRequest;
import com.emr.auth.entity.AuthTokenEntity;
import com.emr.auth.entity.DoctorAccountEntity;
import com.emr.auth.entity.PatientAccountEntity;
import com.emr.auth.entity.SystemUserEntity;
import com.emr.auth.mapper.AuthTokenMapper;
import com.emr.auth.mapper.DoctorAccountMapper;
import com.emr.auth.mapper.PatientAccountMapper;
import com.emr.auth.mapper.SystemUserMapper;
import com.emr.auth.service.AuthService;
import com.emr.auth.vo.LoginResponse;
import com.emr.auth.vo.PatientRegisterResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * MySQL 持久化认证服务。
 * 账号来源统一读取 `emr_user` 库，Token 落到 `emr_auth.token`，保持接口契约不变的同时补齐真实持久化链路。
 */
@Service
public class DatabaseAuthService implements AuthService {

    private static final String USERS_TABLE = "users";
    private static final String DOCTOR_TABLE = "yisheng";
    private static final String PATIENT_TABLE = "huanzhe";

    private final SystemUserMapper systemUserMapper;
    private final DoctorAccountMapper doctorAccountMapper;
    private final PatientAccountMapper patientAccountMapper;
    private final AuthTokenMapper authTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Duration tokenTtl;

    public DatabaseAuthService(
            SystemUserMapper systemUserMapper,
            DoctorAccountMapper doctorAccountMapper,
            PatientAccountMapper patientAccountMapper,
            AuthTokenMapper authTokenMapper,
            @Value("${emr.auth.token-ttl:PT2H}") Duration tokenTtl
    ) {
        this.systemUserMapper = systemUserMapper;
        this.doctorAccountMapper = doctorAccountMapper;
        this.patientAccountMapper = patientAccountMapper;
        this.authTokenMapper = authTokenMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.clock = Clock.systemUTC();
        this.tokenTtl = tokenTtl == null || tokenTtl.isNegative() ? Duration.ofHours(2) : tokenTtl;
    }

    /**
     * 初始化演示账号。
     * 只在对应表里不存在时补种数据，避免每次启动都覆盖用户已经改过的密码或资料。
     */
    @PostConstruct
    public void initializeDemoAccounts() {
        ensureSystemUser("admin", "admin123", "系统管理员", "13800000000", "admin");
        ensureSystemUser("super_admin", "admin123", "超级管理员", "13800000001", "super_admin");
        ensureDoctor("doctor", "123456", "演示医生", "13800000002");
        ensureSystemUser("nurse", "123456", "演示护士", "13800000003", "nurse");
        ensureSystemUser("director", "123456", "演示主任", "13800000004", "director");
        ensurePatient("patient_demo", "123456", "患者演示", "13800000005", "男");
    }

    /**
     * 登录并写入 Token 表。
     * 登录认证明确校验账号、密码、角色三者一致，写库成功后再把有效期返回给前端和网关。
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String username = requireText(request == null ? null : request.username(), "账号不能为空");
        String password = requireText(request == null ? null : request.password(), "密码不能为空");
        String roleCode = requireText(request == null ? null : request.roleCode(), "角色不能为空");

        AccountSnapshot account = requireAccount(username);
        if (!roleCode.equals(account.roleCode()) || !passwordEncoder.matches(password, account.passwordHash())) {
            throw new IllegalArgumentException("账号、密码或角色不正确");
        }

        Instant expireAt = Instant.now(clock).plus(tokenTtl);
        String token = UUID.randomUUID().toString().replace("-", "");
        AuthTokenEntity entity = new AuthTokenEntity();
        entity.setUserId(account.userId());
        entity.setUsername(account.username());
        entity.setRoleCode(account.roleCode());
        entity.setTableName(account.tableName());
        entity.setToken(token);
        entity.setExpireAt(LocalDateTime.ofInstant(expireAt, ZoneOffset.UTC));
        authTokenMapper.insert(entity);

        return new LoginResponse(
                account.userId(),
                account.username(),
                account.roleCode(),
                account.tableName(),
                token,
                expireAt.toString()
        );
    }

    /**
     * 注册患者并落到 `huanzhe` 表。
     * 密码以 BCrypt 哈希存储，保证后续登录、改密和重置密码都复用同一套安全口径。
     */
    @Override
    public PatientRegisterResponse registerPatient(PatientRegisterRequest request) {
        String username = requireText(request == null ? null : request.username(), "账号不能为空");
        String password = requireText(request == null ? null : request.password(), "密码不能为空");
        String name = requireText(request == null ? null : request.name(), "姓名不能为空");
        String gender = request == null ? null : request.gender();
        String phone = request == null ? null : request.phone();
        if (usernameExists(username)) {
            throw new IllegalArgumentException("账号已存在");
        }

        PatientAccountEntity entity = new PatientAccountEntity();
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(password));
        entity.setName(name);
        entity.setGender(gender);
        entity.setPhone(phone);
        entity.setStatus(1);
        patientAccountMapper.insert(entity);
        return new PatientRegisterResponse(entity.getId(), username, name, gender, phone, "patient");
    }

    /**
     * 校验 Token 并返回登录上下文。
     * 过期 Token 会在校验时顺手删掉，避免表里累积无效会话。
     */
    @Override
    public LoginResponse validateToken(String token) {
        String value = requireText(token, "Token不能为空");
        AuthTokenEntity entity = authTokenMapper.selectOne(new LambdaQueryWrapper<AuthTokenEntity>()
                .eq(AuthTokenEntity::getToken, value)
                .last("limit 1"));
        if (entity == null) {
            throw new IllegalArgumentException("Token无效或已过期");
        }

        Instant expireAt = entity.getExpireAt().toInstant(ZoneOffset.UTC);
        if (!expireAt.isAfter(Instant.now(clock))) {
            authTokenMapper.deleteById(entity.getId());
            throw new IllegalArgumentException("Token无效或已过期");
        }
        return new LoginResponse(
                entity.getUserId(),
                entity.getUsername(),
                entity.getRoleCode(),
                entity.getTableName(),
                entity.getToken(),
                expireAt.toString()
        );
    }

    /**
     * 修改密码并清掉该账号现存 Token。
     * 先校验旧密码，再按账号来源表更新哈希值，最后统一剔除历史会话。
     */
    @Override
    public String changePassword(String username, String oldPassword, String newPassword) {
        String accountName = requireText(username, "账号不能为空");
        String oldValue = requireText(oldPassword, "旧密码不能为空");
        String newValue = requireText(newPassword, "新密码不能为空");
        AccountSnapshot account = requireAccount(accountName);
        if (!passwordEncoder.matches(oldValue, account.passwordHash())) {
            throw new IllegalArgumentException("旧密码不正确");
        }

        updatePassword(account, passwordEncoder.encode(newValue));
        removeUserTokens(accountName);
        return "密码修改成功";
    }

    /**
     * 通过手机号重置密码并清理旧 Token。
     * 当前仍然沿用文档里的“账号 + 手机号”找回口径，后续再接短信验证码不会影响此处表结构。
     */
    @Override
    public String resetPassword(String username, String phone, String newPassword) {
        String accountName = requireText(username, "账号不能为空");
        String phoneValue = requireText(phone, "手机号不能为空");
        String newValue = requireText(newPassword, "新密码不能为空");
        AccountSnapshot account = requireAccount(accountName);
        if (account.phone() == null || !account.phone().equals(phoneValue)) {
            throw new IllegalArgumentException("账号或手机号不正确");
        }

        updatePassword(account, passwordEncoder.encode(newValue));
        removeUserTokens(accountName);
        return "密码重置成功";
    }

    /**
     * 注销当前 Token。
     * 与登录校验共享同一张 Token 表，注销只删除当前令牌，不影响同账号其他并发会话。
     */
    @Override
    public String logout(String token) {
        String value = requireText(token, "Token不能为空");
        authTokenMapper.delete(new LambdaQueryWrapper<AuthTokenEntity>().eq(AuthTokenEntity::getToken, value));
        return "注销成功";
    }

    /**
     * 读取账号快照。
     * 账号来源按 `users -> yisheng -> huanzhe` 的顺序查找，且只接受启用状态账号参与认证。
     */
    private AccountSnapshot requireAccount(String username) {
        SystemUserEntity systemUser = systemUserMapper.selectOne(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getUsername, username)
                .eq(SystemUserEntity::getStatus, 1)
                .last("limit 1"));
        if (systemUser != null) {
            return new AccountSnapshot(
                    systemUser.getId(),
                    systemUser.getUsername(),
                    systemUser.getPassword(),
                    systemUser.getRealName(),
                    systemUser.getPhone(),
                    systemUser.getRoleCode(),
                    USERS_TABLE
            );
        }

        DoctorAccountEntity doctor = doctorAccountMapper.selectOne(new LambdaQueryWrapper<DoctorAccountEntity>()
                .eq(DoctorAccountEntity::getUsername, username)
                .eq(DoctorAccountEntity::getStatus, 1)
                .last("limit 1"));
        if (doctor != null) {
            return new AccountSnapshot(
                    doctor.getId(),
                    doctor.getUsername(),
                    doctor.getPassword(),
                    doctor.getName(),
                    doctor.getPhone(),
                    "doctor",
                    DOCTOR_TABLE
            );
        }

        PatientAccountEntity patient = patientAccountMapper.selectOne(new LambdaQueryWrapper<PatientAccountEntity>()
                .eq(PatientAccountEntity::getUsername, username)
                .eq(PatientAccountEntity::getStatus, 1)
                .last("limit 1"));
        if (patient != null) {
            return new AccountSnapshot(
                    patient.getId(),
                    patient.getUsername(),
                    patient.getPassword(),
                    patient.getName(),
                    patient.getPhone(),
                    "patient",
                    PATIENT_TABLE
            );
        }
        throw new IllegalArgumentException("账号不存在");
    }

    /**
     * 更新不同来源表中的密码哈希。
     * 这里按表来源显式分支，避免误把患者密码更新到系统用户表之类的跨表错误。
     */
    private void updatePassword(AccountSnapshot account, String newPasswordHash) {
        switch (account.tableName()) {
            case USERS_TABLE -> systemUserMapper.update(null, new LambdaUpdateWrapper<SystemUserEntity>()
                    .eq(SystemUserEntity::getId, account.userId())
                    .set(SystemUserEntity::getPassword, newPasswordHash));
            case DOCTOR_TABLE -> doctorAccountMapper.update(null, new LambdaUpdateWrapper<DoctorAccountEntity>()
                    .eq(DoctorAccountEntity::getId, account.userId())
                    .set(DoctorAccountEntity::getPassword, newPasswordHash));
            case PATIENT_TABLE -> patientAccountMapper.update(null, new LambdaUpdateWrapper<PatientAccountEntity>()
                    .eq(PatientAccountEntity::getId, account.userId())
                    .set(PatientAccountEntity::getPassword, newPasswordHash));
            default -> throw new IllegalArgumentException("未知账号来源");
        }
    }

    /**
     * 清理指定账号的所有 Token。
     * 改密和找回密码后都要调用这里，防止旧登录态继续可用。
     */
    private void removeUserTokens(String username) {
        authTokenMapper.delete(new LambdaQueryWrapper<AuthTokenEntity>().eq(AuthTokenEntity::getUsername, username));
    }

    /**
     * 判断用户名是否已被占用。
     * 唯一性跨三类账号表统一校验，避免同一个 username 同时出现在患者和管理员表。
     */
    private boolean usernameExists(String username) {
        return systemUserMapper.selectCount(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getUsername, username)) > 0
                || doctorAccountMapper.selectCount(new LambdaQueryWrapper<DoctorAccountEntity>()
                .eq(DoctorAccountEntity::getUsername, username)) > 0
                || patientAccountMapper.selectCount(new LambdaQueryWrapper<PatientAccountEntity>()
                .eq(PatientAccountEntity::getUsername, username)) > 0;
    }

    /**
     * 兜底初始化系统用户账号。
     * 只补全缺失账号，不覆盖已存在的真实数据。
     */
    private void ensureSystemUser(String username, String password, String realName, String phone, String roleCode) {
        if (systemUserMapper.selectCount(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getUsername, username)) > 0) {
            return;
        }
        SystemUserEntity entity = new SystemUserEntity();
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(password));
        entity.setRealName(realName);
        entity.setRoleCode(roleCode);
        entity.setPhone(phone);
        entity.setStatus(1);
        systemUserMapper.insert(entity);
    }

    /**
     * 兜底初始化医生演示账号。
     * 医生账号单独落在 `yisheng` 表，便于后续和医生资料查询继续共用一份主数据。
     */
    private void ensureDoctor(String username, String password, String name, String phone) {
        if (doctorAccountMapper.selectCount(new LambdaQueryWrapper<DoctorAccountEntity>()
                .eq(DoctorAccountEntity::getUsername, username)) > 0) {
            return;
        }
        DoctorAccountEntity entity = new DoctorAccountEntity();
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(password));
        entity.setName(name);
        entity.setPhone(phone);
        entity.setStatus(1);
        doctorAccountMapper.insert(entity);
    }

    /**
     * 兜底初始化患者演示账号。
     * 患者注册和演示账号共享同一张 `huanzhe` 表，保证登录来源和数据口径一致。
     */
    private void ensurePatient(String username, String password, String name, String phone, String gender) {
        if (patientAccountMapper.selectCount(new LambdaQueryWrapper<PatientAccountEntity>()
                .eq(PatientAccountEntity::getUsername, username)) > 0) {
            return;
        }
        PatientAccountEntity entity = new PatientAccountEntity();
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(password));
        entity.setName(name);
        entity.setGender(gender);
        entity.setPhone(phone);
        entity.setStatus(1);
        patientAccountMapper.insert(entity);
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 认证过程统一使用的账号快照。
     * 这里显式区分 `tableName`，保证后续改密和 Token 返回都能追溯到正确的数据来源表。
     */
    private record AccountSnapshot(
            Long userId,
            String username,
            String passwordHash,
            String name,
            String phone,
            String roleCode,
            String tableName
    ) {
    }
}
