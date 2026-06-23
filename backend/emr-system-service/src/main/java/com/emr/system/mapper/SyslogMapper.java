package com.emr.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.system.entity.SyslogEntity;

/**
 * 操作日志 Mapper。
 * 审计日志写入和后台日志查询共用这个基础访问层。
 */
public interface SyslogMapper extends BaseMapper<SyslogEntity> {
}
