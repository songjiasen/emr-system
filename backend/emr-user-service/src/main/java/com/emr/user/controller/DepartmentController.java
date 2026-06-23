package com.emr.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.user.entity.DepartmentEntity;
import com.emr.user.mapper.DepartmentMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 科室管理接口。
 * 直接读写 `keshileixing` 表，兼容现有科室增删改查接口和分页响应结构。
 */
@RestController
public class DepartmentController {

    private final DepartmentMapper departmentMapper;

    public DepartmentController(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    /**
     * 创建科室。
     * 保持接口只要求 `name`，排序号缺省时仍然回落到 0，避免影响现有前端提交流程。
     */
    @PostMapping("/departments")
    public ApiResponse<Map<String, Object>> createDepartment(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        DepartmentEntity entity = new DepartmentEntity();
        entity.setName(requireText(payload.get("name"), "科室名称不能为空"));
        entity.setSortNo(intValue(payload.get("sortNo"), 0));
        entity.setStatus(1);
        departmentMapper.insert(entity);
        return ApiResponse.success(toRow(entity));
    }

    @GetMapping("/departments")
    public ApiResponse<PageResult<Map<String, Object>>> listDepartments(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> rows = departmentMapper.selectList(new LambdaQueryWrapper<DepartmentEntity>()
                        .orderByAsc(DepartmentEntity::getId))
                .stream()
                .map(this::toRow)
                .toList();
        return ApiResponse.success(PageResult.of(rows, page, limit));
    }

    @PutMapping("/departments/{id}")
    public ApiResponse<Map<String, Object>> updateDepartment(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> request) {
        DepartmentEntity entity = requireDepartment(id);
        Map<String, Object> payload = safePayload(request);
        if (payload.containsKey("name")) {
            entity.setName(requireText(payload.get("name"), "科室名称不能为空"));
        }
        if (payload.containsKey("sortNo")) {
            entity.setSortNo(intValue(payload.get("sortNo"), entity.getSortNo()));
        }
        if (payload.containsKey("status")) {
            entity.setStatus(intValue(payload.get("status"), entity.getStatus()));
        }
        departmentMapper.updateById(entity);
        return ApiResponse.success(toRow(entity));
    }

    @DeleteMapping("/departments/{id}")
    public ApiResponse<Map<String, Object>> deleteDepartment(@PathVariable("id") Long id) {
        DepartmentEntity entity = requireDepartment(id);
        entity.setStatus(0);
        departmentMapper.updateById(entity);
        return ApiResponse.success(toRow(entity));
    }

    /**
     * 把实体统一转成前端既有字段结构。
     * 这里继续输出 `sortNo/status` 命名，避免前端因为数据库字段是下划线而感知变化。
     */
    private Map<String, Object> toRow(DepartmentEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("name", entity.getName());
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        return row;
    }

    private DepartmentEntity requireDepartment(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("科室不存在");
        }
        DepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("科室不存在");
        }
        return entity;
    }

    /**
     * 处理空请求体，避免科室新增/编辑因为空 Map 访问抛出空指针异常。
     */
    private Map<String, Object> safePayload(Map<String, Object> request) {
        return request == null ? Map.of() : request;
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value.toString().trim();
        return text.isEmpty() ? defaultValue : Integer.parseInt(text);
    }

    private String requireText(Object value, String message) {
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.toString().trim();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }
}
