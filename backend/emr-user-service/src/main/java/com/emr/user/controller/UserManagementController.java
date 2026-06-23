package com.emr.user.controller;

import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.user.service.UserDirectoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户管理接口。
 * 通过 type 路由到不同用户表，统一对外保留管理员端的用户维护接口形状。
 */
@RestController
public class UserManagementController {

    private final UserDirectoryService userDirectoryService;

    public UserManagementController(UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }

    /**
     * 创建用户。
     * 未传密码时会补默认初始密码，兼容现有管理端“先建账号再通知改密”的流程。
     */
    @PostMapping("/user-management/{type}")
    public ApiResponse<Map<String, Object>> createUser(@PathVariable("type") String type, @RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(userDirectoryService.createUser(type, request));
    }

    @GetMapping("/user-management/{type}")
    public ApiResponse<PageResult<Map<String, Object>>> listUsers(
            @PathVariable("type") String type,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ApiResponse.success(userDirectoryService.listUsers(type, page, limit));
    }

    @GetMapping("/user-management/{type}/{id}")
    public ApiResponse<Map<String, Object>> getUser(@PathVariable("type") String type, @PathVariable("id") Long id) {
        return ApiResponse.success(userDirectoryService.getUser(type, id));
    }

    @PutMapping("/user-management/{type}/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable("type") String type, @PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(userDirectoryService.updateUser(type, id, request));
    }

    @DeleteMapping("/user-management/{type}/{id}")
    public ApiResponse<Map<String, Object>> deleteUser(@PathVariable("type") String type, @PathVariable("id") Long id) {
        return ApiResponse.success(userDirectoryService.deleteUser(type, id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }
}
