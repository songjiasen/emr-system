package com.emr.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.user.entity.SystemUserEntity;

/**
 * 系统用户 Mapper。
 * 用于管理员、护士、主任等后台账号的持久化维护。
 */
public interface SystemUserMapper extends BaseMapper<SystemUserEntity> {
}
