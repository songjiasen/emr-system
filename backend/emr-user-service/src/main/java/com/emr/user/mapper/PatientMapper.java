package com.emr.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.user.entity.PatientEntity;

/**
 * 患者 Mapper。
 * 管理端创建患者和后续患者档案扩展都通过该表读写。
 */
public interface PatientMapper extends BaseMapper<PatientEntity> {
}
