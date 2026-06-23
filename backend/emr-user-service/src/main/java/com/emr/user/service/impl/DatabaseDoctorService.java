package com.emr.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.common.PageResult;
import com.emr.user.entity.DepartmentEntity;
import com.emr.user.entity.DoctorEntity;
import com.emr.user.mapper.DepartmentMapper;
import com.emr.user.mapper.DoctorMapper;
import com.emr.user.service.DoctorService;
import com.emr.user.vo.DoctorSummaryResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据库版医生资料服务。
 * 使用 `yisheng` 和 `keshileixing` 表替代原有内存演示数据，同时保留默认演示医生以兼容旧测试和前端默认入口。
 */
@Service
public class DatabaseDoctorService implements DoctorService {

    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    public DatabaseDoctorService(DoctorMapper doctorMapper, DepartmentMapper departmentMapper) {
        this.doctorMapper = doctorMapper;
        this.departmentMapper = departmentMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 初始化演示科室和演示医生。
     * 只在对应账号不存在时补种数据，保证空库启动后 `/doctors/1` 依然可用，但不会覆盖用户自己维护的数据。
     */
    @PostConstruct
    public void initializeDemoDoctors() {
        DepartmentEntity heartDepartment = ensureDepartment("心内科", 10);
        DepartmentEntity childDepartment = ensureDepartment("儿科", 20);
        ensureDoctor(
                "doctor_heart",
                "123456",
                "王医生",
                "男",
                "13900000001",
                heartDepartment.getId(),
                heartDepartment.getName(),
                "高血压、冠心病",
                "从事心内科临床工作多年，擅长常见心血管疾病诊疗。"
        );
        ensureDoctor(
                "doctor_child",
                "123456",
                "李医生",
                "女",
                "13900000002",
                childDepartment.getId(),
                childDepartment.getName(),
                "儿童呼吸道疾病",
                "擅长儿童常见病、多发病诊疗。"
        );
    }

    /**
     * 查询启用中的医生列表。
     * 患者端只展示启用状态医生，防止后台已停用账号继续出现在预约挂号入口。
     */
    @Override
    public PageResult<DoctorSummaryResponse> listDoctors(int page, int limit) {
        List<DoctorSummaryResponse> doctors = doctorMapper.selectList(new LambdaQueryWrapper<DoctorEntity>()
                        .eq(DoctorEntity::getStatus, 1)
                        .orderByAsc(DoctorEntity::getId))
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(doctors, page, limit);
    }

    /**
     * 查询单个医生详情。
     * 详情接口同样只允许读取启用医生，避免前端拿到已停用医生档案造成误预约。
     */
    @Override
    public DoctorSummaryResponse getDoctor(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("医生ID不能为空");
        }
        DoctorEntity entity = doctorMapper.selectById(id);
        if (entity == null || entity.getStatus() == null || entity.getStatus() != 1) {
            throw new IllegalArgumentException("医生不存在");
        }
        return toResponse(entity);
    }

    private DepartmentEntity ensureDepartment(String name, int sortNo) {
        DepartmentEntity existing = departmentMapper.selectOne(new LambdaQueryWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getName, name)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }

        DepartmentEntity entity = new DepartmentEntity();
        entity.setName(name);
        entity.setSortNo(sortNo);
        entity.setStatus(1);
        departmentMapper.insert(entity);
        return entity;
    }

    private void ensureDoctor(
            String username,
            String password,
            String name,
            String gender,
            String phone,
            Long departmentId,
            String departmentName,
            String specialty,
            String profile
    ) {
        DoctorEntity existing = doctorMapper.selectOne(new LambdaQueryWrapper<DoctorEntity>()
                .eq(DoctorEntity::getUsername, username)
                .last("limit 1"));
        if (existing != null) {
            return;
        }

        DoctorEntity entity = new DoctorEntity();
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(password));
        entity.setName(name);
        entity.setGender(gender);
        entity.setPhone(phone);
        entity.setDepartmentId(departmentId);
        entity.setDepartmentName(departmentName);
        entity.setSpecialty(specialty);
        entity.setProfile(profile);
        entity.setStatus(1);
        doctorMapper.insert(entity);
    }

    private DoctorSummaryResponse toResponse(DoctorEntity entity) {
        return new DoctorSummaryResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getName(),
                entity.getGender(),
                entity.getPhone(),
                entity.getDepartmentId(),
                entity.getDepartmentName(),
                entity.getSpecialty(),
                entity.getProfile()
        );
    }
}
