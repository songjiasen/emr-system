package com.emr.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.system.entity.CarouselEntity;

/**
 * 轮播图 Mapper。
 * 轮播图维护是后台系统配置的一部分，因此和其他系统内容资源共用同一服务数据库。
 */
public interface CarouselMapper extends BaseMapper<CarouselEntity> {
}
