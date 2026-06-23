package com.emr.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.ai.entity.AiRequestLogEntity;

/**
 * AI 调用日志 Mapper。
 * 负责 `ai_request_log` 表的持久化读写。
 */
public interface AiRequestLogMapper extends BaseMapper<AiRequestLogEntity> {
}
