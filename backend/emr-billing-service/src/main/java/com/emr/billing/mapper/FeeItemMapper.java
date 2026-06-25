package com.emr.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.billing.entity.FeeItemEntity;

/**
 * 费用项目配置 Mapper。
 * 负责读取可用于新增费用的标准收费项目。
 */
public interface FeeItemMapper extends BaseMapper<FeeItemEntity> {
}
