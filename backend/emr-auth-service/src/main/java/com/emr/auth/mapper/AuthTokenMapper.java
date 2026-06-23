package com.emr.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.auth.entity.AuthTokenEntity;

/**
 * Token 表 Mapper。
 * 负责登录态持久化的基础 CRUD，复杂鉴权口径仍由服务层统一收敛。
 */
public interface AuthTokenMapper extends BaseMapper<AuthTokenEntity> {
}
