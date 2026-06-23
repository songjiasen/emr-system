package com.emr.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.auth.entity.PatientAccountEntity;

/**
 * 患者账号 Mapper。
 * 负责患者注册、登录查询和密码改写所需的表访问。
 */
public interface PatientAccountMapper extends BaseMapper<PatientAccountEntity> {
}
