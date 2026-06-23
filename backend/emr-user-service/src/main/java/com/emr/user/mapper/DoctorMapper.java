package com.emr.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.user.entity.DoctorEntity;

/**
 * 医生 Mapper。
 * 统一承接医生档案和预约挂号用到的医生查询。
 */
public interface DoctorMapper extends BaseMapper<DoctorEntity> {
}
