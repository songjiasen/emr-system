package com.emr.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.common.PageResult;
import com.emr.user.entity.DepartmentEntity;
import com.emr.user.entity.DoctorEntity;
import com.emr.user.entity.PatientEntity;
import com.emr.user.entity.SystemUserEntity;
import com.emr.user.mapper.DepartmentMapper;
import com.emr.user.mapper.DoctorMapper;
import com.emr.user.mapper.PatientMapper;
import com.emr.user.mapper.SystemUserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户目录服务。
 * 负责按用户类型路由到不同数据表，同时对外保持统一的用户管理接口结构。
 */
@Service
public class UserDirectoryService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SystemUserMapper systemUserMapper;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    public UserDirectoryService(
            SystemUserMapper systemUserMapper,
            DoctorMapper doctorMapper,
            PatientMapper patientMapper,
            DepartmentMapper departmentMapper
    ) {
        this.systemUserMapper = systemUserMapper;
        this.doctorMapper = doctorMapper;
        this.patientMapper = patientMapper;
        this.departmentMapper = departmentMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 创建用户并写入对应表。
     * 为了兼容当前管理端最简建档流程，密码允许缺省并自动回退到统一初始密码。
     */
    public Map<String, Object> createUser(String type, Map<String, Object> request) {
        UserTypeConfig config = resolveType(type);
        Map<String, Object> payload = safePayload(request);
        String username = requireText(payload.get("username"), "账号不能为空");
        String name = requireText(payload.get("name"), "姓名不能为空");
        ensureUsernameAvailable(username, config.storageKind(), null);
        return switch (config.storageKind()) {
            case DOCTOR -> createDoctor(config, payload, username, name);
            case PATIENT -> createPatient(config, payload, username, name);
            case SYSTEM -> createSystemUser(config, payload, username, name);
        };
    }

    /**
     * 按类型列出用户。
     * 继续复用通用分页对象，避免前端列表页切换到数据库后再改字段解析逻辑。
     */
    public PageResult<Map<String, Object>> listUsers(String type, int page, int limit) {
        UserTypeConfig config = resolveType(type);
        List<Map<String, Object>> rows = switch (config.storageKind()) {
            case DOCTOR -> doctorMapper.selectList(new LambdaQueryWrapper<DoctorEntity>()
                            .orderByAsc(DoctorEntity::getId))
                    .stream()
                    .map(entity -> toDoctorRow(config.type(), entity))
                    .toList();
            case PATIENT -> patientMapper.selectList(new LambdaQueryWrapper<PatientEntity>()
                            .orderByAsc(PatientEntity::getId))
                    .stream()
                    .map(entity -> toPatientRow(config.type(), entity))
                    .toList();
            case SYSTEM -> systemUserMapper.selectList(new LambdaQueryWrapper<SystemUserEntity>()
                            .eq(SystemUserEntity::getRoleCode, config.roleCode())
                            .orderByAsc(SystemUserEntity::getId))
                    .stream()
                    .map(entity -> toSystemUserRow(config.type(), entity))
                    .toList();
        };
        return PageResult.of(rows, page, limit);
    }

    /**
     * 读取单个用户详情。
     * 这里显式校验用户类型和主键是否匹配，避免不同角色路由串用同一条记录。
     */
    public Map<String, Object> getUser(String type, Long id) {
        UserTypeConfig config = resolveType(type);
        return switch (config.storageKind()) {
            case DOCTOR -> toDoctorRow(config.type(), requireDoctor(id));
            case PATIENT -> toPatientRow(config.type(), requirePatient(id));
            case SYSTEM -> toSystemUserRow(config.type(), requireSystemUser(id, config.roleCode()));
        };
    }

    /**
     * 更新用户资料。
     * 保持 `roleCode/type/id` 不允许被请求体直接篡改，避免管理端误提交把账号切到错误表里。
     */
    public Map<String, Object> updateUser(String type, Long id, Map<String, Object> request) {
        UserTypeConfig config = resolveType(type);
        Map<String, Object> payload = safePayload(request);
        return switch (config.storageKind()) {
            case DOCTOR -> updateDoctor(config, id, payload);
            case PATIENT -> updatePatient(config, id, payload);
            case SYSTEM -> updateSystemUser(config, id, payload);
        };
    }

    /**
     * 软删除用户。
     * 删除行为只改状态位，兼容现有前后端对“停用账号”而非物理删除的口径。
     */
    public Map<String, Object> deleteUser(String type, Long id) {
        UserTypeConfig config = resolveType(type);
        return switch (config.storageKind()) {
            case DOCTOR -> {
                DoctorEntity entity = requireDoctor(id);
                entity.setStatus(0);
                doctorMapper.updateById(entity);
                yield toDoctorRow(config.type(), entity);
            }
            case PATIENT -> {
                PatientEntity entity = requirePatient(id);
                entity.setStatus(0);
                patientMapper.updateById(entity);
                yield toPatientRow(config.type(), entity);
            }
            case SYSTEM -> {
                SystemUserEntity entity = requireSystemUser(id, config.roleCode());
                entity.setStatus(0);
                systemUserMapper.updateById(entity);
                yield toSystemUserRow(config.type(), entity);
            }
        };
    }

    private Map<String, Object> createSystemUser(UserTypeConfig config, Map<String, Object> payload, String username, String name) {
        SystemUserEntity entity = new SystemUserEntity();
        entity.setUsername(username);
        entity.setPassword(resolvePasswordHash(payload.get("password")));
        entity.setRealName(name);
        entity.setRoleCode(config.roleCode());
        entity.setGender(stringValue(payload.get("gender")));
        entity.setPhone(stringValue(payload.get("phone")));
        entity.setAvatar(stringValue(payload.get("avatar")));
        entity.setStatus(1);
        systemUserMapper.insert(entity);
        return toSystemUserRow(config.type(), entity);
    }

    private Map<String, Object> createDoctor(UserTypeConfig config, Map<String, Object> payload, String username, String name) {
        DoctorEntity entity = new DoctorEntity();
        entity.setUsername(username);
        entity.setPassword(resolvePasswordHash(payload.get("password")));
        entity.setName(name);
        entity.setGender(stringValue(payload.get("gender")));
        entity.setPhone(stringValue(payload.get("phone")));
        entity.setDepartmentId(longValue(payload.get("departmentId")));
        entity.setDepartmentName(resolveDepartmentName(entity.getDepartmentId(), stringValue(payload.get("departmentName"))));
        entity.setSpecialty(stringValue(payload.get("specialty")));
        entity.setProfile(stringValue(payload.get("profile")));
        entity.setAvatar(stringValue(payload.get("avatar")));
        entity.setStatus(1);
        doctorMapper.insert(entity);
        return toDoctorRow(config.type(), entity);
    }

    private Map<String, Object> createPatient(UserTypeConfig config, Map<String, Object> payload, String username, String name) {
        PatientEntity entity = new PatientEntity();
        entity.setUsername(username);
        entity.setPassword(resolvePasswordHash(payload.get("password")));
        entity.setName(name);
        entity.setGender(stringValue(payload.get("gender")));
        entity.setPhone(stringValue(payload.get("phone")));
        entity.setIdCard(stringValue(payload.get("idCard")));
        entity.setBirthDate(localDateValue(payload.get("birthDate")));
        entity.setAddress(stringValue(payload.get("address")));
        entity.setEmergencyContact(stringValue(payload.get("emergencyContact")));
        entity.setEmergencyPhone(stringValue(payload.get("emergencyPhone")));
        entity.setStatus(1);
        patientMapper.insert(entity);
        return toPatientRow(config.type(), entity);
    }

    private Map<String, Object> updateSystemUser(UserTypeConfig config, Long id, Map<String, Object> payload) {
        SystemUserEntity entity = requireSystemUser(id, config.roleCode());
        if (payload.containsKey("username")) {
            String username = requireText(payload.get("username"), "账号不能为空");
            ensureUsernameAvailable(username, StorageKind.SYSTEM, entity.getId());
            entity.setUsername(username);
        }
        if (payload.containsKey("password")) {
            entity.setPassword(resolvePasswordHash(payload.get("password")));
        }
        if (payload.containsKey("name")) {
            entity.setRealName(requireText(payload.get("name"), "姓名不能为空"));
        }
        if (payload.containsKey("gender")) {
            entity.setGender(stringValue(payload.get("gender")));
        }
        if (payload.containsKey("phone")) {
            entity.setPhone(stringValue(payload.get("phone")));
        }
        if (payload.containsKey("avatar")) {
            entity.setAvatar(stringValue(payload.get("avatar")));
        }
        if (payload.containsKey("status")) {
            entity.setStatus(intValue(payload.get("status"), entity.getStatus()));
        }
        systemUserMapper.updateById(entity);
        return toSystemUserRow(config.type(), entity);
    }

    private Map<String, Object> updateDoctor(UserTypeConfig config, Long id, Map<String, Object> payload) {
        DoctorEntity entity = requireDoctor(id);
        if (payload.containsKey("username")) {
            String username = requireText(payload.get("username"), "账号不能为空");
            ensureUsernameAvailable(username, StorageKind.DOCTOR, entity.getId());
            entity.setUsername(username);
        }
        if (payload.containsKey("password")) {
            entity.setPassword(resolvePasswordHash(payload.get("password")));
        }
        if (payload.containsKey("name")) {
            entity.setName(requireText(payload.get("name"), "姓名不能为空"));
        }
        if (payload.containsKey("gender")) {
            entity.setGender(stringValue(payload.get("gender")));
        }
        if (payload.containsKey("phone")) {
            entity.setPhone(stringValue(payload.get("phone")));
        }
        if (payload.containsKey("departmentId") || payload.containsKey("departmentName")) {
            Long departmentId = payload.containsKey("departmentId") ? longValue(payload.get("departmentId")) : entity.getDepartmentId();
            String departmentName = payload.containsKey("departmentName") ? stringValue(payload.get("departmentName")) : entity.getDepartmentName();
            entity.setDepartmentId(departmentId);
            entity.setDepartmentName(resolveDepartmentName(departmentId, departmentName));
        }
        if (payload.containsKey("specialty")) {
            entity.setSpecialty(stringValue(payload.get("specialty")));
        }
        if (payload.containsKey("profile")) {
            entity.setProfile(stringValue(payload.get("profile")));
        }
        if (payload.containsKey("avatar")) {
            entity.setAvatar(stringValue(payload.get("avatar")));
        }
        if (payload.containsKey("status")) {
            entity.setStatus(intValue(payload.get("status"), entity.getStatus()));
        }
        doctorMapper.updateById(entity);
        return toDoctorRow(config.type(), entity);
    }

    private Map<String, Object> updatePatient(UserTypeConfig config, Long id, Map<String, Object> payload) {
        PatientEntity entity = requirePatient(id);
        if (payload.containsKey("username")) {
            String username = requireText(payload.get("username"), "账号不能为空");
            ensureUsernameAvailable(username, StorageKind.PATIENT, entity.getId());
            entity.setUsername(username);
        }
        if (payload.containsKey("password")) {
            entity.setPassword(resolvePasswordHash(payload.get("password")));
        }
        if (payload.containsKey("name")) {
            entity.setName(requireText(payload.get("name"), "姓名不能为空"));
        }
        if (payload.containsKey("gender")) {
            entity.setGender(stringValue(payload.get("gender")));
        }
        if (payload.containsKey("phone")) {
            entity.setPhone(stringValue(payload.get("phone")));
        }
        if (payload.containsKey("idCard")) {
            entity.setIdCard(stringValue(payload.get("idCard")));
        }
        if (payload.containsKey("birthDate")) {
            entity.setBirthDate(localDateValue(payload.get("birthDate")));
        }
        if (payload.containsKey("address")) {
            entity.setAddress(stringValue(payload.get("address")));
        }
        if (payload.containsKey("emergencyContact")) {
            entity.setEmergencyContact(stringValue(payload.get("emergencyContact")));
        }
        if (payload.containsKey("emergencyPhone")) {
            entity.setEmergencyPhone(stringValue(payload.get("emergencyPhone")));
        }
        if (payload.containsKey("status")) {
            entity.setStatus(intValue(payload.get("status"), entity.getStatus()));
        }
        patientMapper.updateById(entity);
        return toPatientRow(config.type(), entity);
    }

    private void ensureUsernameAvailable(String username, StorageKind selfKind, Long selfId) {
        SystemUserEntity systemUser = systemUserMapper.selectOne(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getUsername, username)
                .last("limit 1"));
        if (systemUser != null && (selfKind != StorageKind.SYSTEM || !systemUser.getId().equals(selfId))) {
            throw new IllegalArgumentException("账号已存在");
        }

        DoctorEntity doctor = doctorMapper.selectOne(new LambdaQueryWrapper<DoctorEntity>()
                .eq(DoctorEntity::getUsername, username)
                .last("limit 1"));
        if (doctor != null && (selfKind != StorageKind.DOCTOR || !doctor.getId().equals(selfId))) {
            throw new IllegalArgumentException("账号已存在");
        }

        PatientEntity patient = patientMapper.selectOne(new LambdaQueryWrapper<PatientEntity>()
                .eq(PatientEntity::getUsername, username)
                .last("limit 1"));
        if (patient != null && (selfKind != StorageKind.PATIENT || !patient.getId().equals(selfId))) {
            throw new IllegalArgumentException("账号已存在");
        }
    }

    private SystemUserEntity requireSystemUser(Long id, String roleCode) {
        if (id == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        SystemUserEntity entity = systemUserMapper.selectById(id);
        if (entity == null || !roleCode.equals(entity.getRoleCode())) {
            throw new IllegalArgumentException("用户类型不匹配");
        }
        return entity;
    }

    private DoctorEntity requireDoctor(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        DoctorEntity entity = doctorMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return entity;
    }

    private PatientEntity requirePatient(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        PatientEntity entity = patientMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return entity;
    }

    private Map<String, Object> toSystemUserRow(String type, SystemUserEntity entity) {
        Map<String, Object> row = baseRow(type, entity.getId(), entity.getUsername(), entity.getRealName(), entity.getRoleCode(), entity.getPhone(), entity.getStatus());
        row.put("gender", entity.getGender());
        row.put("avatar", entity.getAvatar());
        row.put("departmentId", null);
        row.put("departmentName", null);
        row.put("specialty", null);
        return row;
    }

    private Map<String, Object> toDoctorRow(String type, DoctorEntity entity) {
        Map<String, Object> row = baseRow(type, entity.getId(), entity.getUsername(), entity.getName(), "doctor", entity.getPhone(), entity.getStatus());
        row.put("gender", entity.getGender());
        row.put("avatar", entity.getAvatar());
        row.put("departmentId", entity.getDepartmentId());
        row.put("departmentName", entity.getDepartmentName());
        row.put("specialty", entity.getSpecialty());
        row.put("profile", entity.getProfile());
        return row;
    }

    private Map<String, Object> toPatientRow(String type, PatientEntity entity) {
        Map<String, Object> row = baseRow(type, entity.getId(), entity.getUsername(), entity.getName(), "patient", entity.getPhone(), entity.getStatus());
        row.put("gender", entity.getGender());
        row.put("departmentId", null);
        row.put("departmentName", null);
        row.put("specialty", null);
        row.put("idCard", entity.getIdCard());
        row.put("birthDate", entity.getBirthDate());
        row.put("address", entity.getAddress());
        row.put("emergencyContact", entity.getEmergencyContact());
        row.put("emergencyPhone", entity.getEmergencyPhone());
        return row;
    }

    private Map<String, Object> baseRow(String type, Long id, String username, String name, String roleCode, String phone, Integer status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("type", type);
        row.put("userType", type);
        row.put("username", username);
        row.put("name", name);
        row.put("roleCode", roleCode);
        row.put("phone", phone);
        row.put("status", status);
        return row;
    }

    private UserTypeConfig resolveType(String type) {
        String value = requireText(type, "用户类型不能为空");
        return switch (value) {
            case "doctors" -> new UserTypeConfig(value, "doctor", StorageKind.DOCTOR);
            case "patients" -> new UserTypeConfig(value, "patient", StorageKind.PATIENT);
            case "admins" -> new UserTypeConfig(value, "admin", StorageKind.SYSTEM);
            case "nurses" -> new UserTypeConfig(value, "nurse", StorageKind.SYSTEM);
            case "directors" -> new UserTypeConfig(value, "director", StorageKind.SYSTEM);
            default -> new UserTypeConfig(value, value, StorageKind.SYSTEM);
        };
    }

    private String resolvePasswordHash(Object passwordValue) {
        if (passwordValue == null) {
            return passwordEncoder.encode(DEFAULT_PASSWORD);
        }
        return passwordEncoder.encode(requireText(passwordValue, "密码不能为空"));
    }

    /**
     * 冗余科室名称优先取请求值，否则按科室 ID 回表查询。
     * 这样即使只传 `departmentId`，医生列表里仍能直接展示科室名称。
     */
    private String resolveDepartmentName(Long departmentId, String departmentName) {
        if (departmentName != null && !departmentName.isBlank()) {
            return departmentName;
        }
        if (departmentId == null) {
            return null;
        }
        DepartmentEntity entity = departmentMapper.selectById(departmentId);
        return entity == null ? null : entity.getName();
    }

    private Map<String, Object> safePayload(Map<String, Object> request) {
        return request == null ? Map.of() : request;
    }

    private String requireText(Object value, String message) {
        String text = stringValue(value);
        if (text == null) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = stringValue(value);
        return text == null ? defaultValue : Integer.parseInt(text);
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = stringValue(value);
        return text == null ? null : Long.parseLong(text);
    }

    private LocalDate localDateValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        String text = stringValue(value);
        return text == null ? null : LocalDate.parse(text);
    }

    private enum StorageKind {
        SYSTEM,
        DOCTOR,
        PATIENT
    }

    private record UserTypeConfig(String type, String roleCode, StorageKind storageKind) {
    }
}
