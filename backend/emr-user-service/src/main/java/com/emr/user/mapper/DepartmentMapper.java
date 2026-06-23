package com.emr.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.user.entity.DepartmentEntity;

/**
 * 科室 Mapper。
 * 用户服务通过它维护科室基础资料和医生关联科室冗余字段。
 */
public interface DepartmentMapper extends BaseMapper<DepartmentEntity> {
}
