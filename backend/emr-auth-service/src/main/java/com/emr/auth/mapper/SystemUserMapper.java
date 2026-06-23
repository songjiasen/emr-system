package com.emr.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.auth.entity.SystemUserEntity;

/**
 * 系统用户 Mapper。
 * 认证服务通过它读取管理员、护士、主任等系统账号的登录资料。
 */
public interface SystemUserMapper extends BaseMapper<SystemUserEntity> {
}
