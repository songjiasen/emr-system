package com.emr.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emr.system.entity.MenuEntity;

/**
 * 菜单 Mapper。
 * 角色菜单当前用一张表存 JSON 配置，避免把菜单树结构展开到本批次之外。
 */
public interface MenuMapper extends BaseMapper<MenuEntity> {
}
