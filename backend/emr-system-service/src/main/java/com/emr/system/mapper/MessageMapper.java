package com.emr.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.system.entity.MessageEntity;

/**
 * 留言 Mapper。
 * 当前承载留言与回复同表更新，避免拆分子表增加本期实现复杂度。
 */
public interface MessageMapper extends BaseMapper<MessageEntity> {
}
