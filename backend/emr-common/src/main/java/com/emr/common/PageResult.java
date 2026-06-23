package com.emr.common;

import java.util.List;

/**
 * 统一分页结果对象。
 * 文档要求列表接口支持分页，这里先固定 rows/total/page/limit 字段，方便前端列表页复用。
 */
public record PageResult<T>(List<T> rows, long total, int page, int limit) {

    /**
     * 从完整列表构造分页结果。
     * 当前内存版服务用这个方法做演示数据分页，后续接 MyBatis-Plus 时可替换为数据库分页结果。
     */
    public static <T> PageResult<T> of(List<T> rows, int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        int fromIndex = Math.min((safePage - 1) * safeLimit, rows.size());
        int toIndex = Math.min(fromIndex + safeLimit, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), rows.size(), safePage, safeLimit);
    }
}
