package com.emr.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.system.entity.NewsEntity;

/**
 * 资讯 Mapper。
 * 负责资讯内容的基础 CRUD，查询排序和分页口径仍由控制层统一组织。
 */
public interface NewsMapper extends BaseMapper<NewsEntity> {
}
