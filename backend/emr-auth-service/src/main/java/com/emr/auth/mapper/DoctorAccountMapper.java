package com.emr.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.auth.entity.DoctorAccountEntity;

/**
 * 医生账号 Mapper。
 * 仅承接认证相关查询和密码更新，不扩展到医生业务信息维护。
 */
public interface DoctorAccountMapper extends BaseMapper<DoctorAccountEntity> {
}
