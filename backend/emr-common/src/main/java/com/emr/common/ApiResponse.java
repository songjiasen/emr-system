package com.emr.common;

/**
 * 统一接口响应对象。
 * 各服务先使用同一层响应结构，保证前端和网关拿到的 code、message、data 语义一致。
 */
public record ApiResponse<T>(Integer code, String message, T data) {

    /**
     * 成功响应的统一入口。
     * 这里固定 code=0，后续失败码再从公共错误码中扩展，避免各服务自行定义。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    /**
     * 失败响应的统一入口。
     * 当前先满足接口原型阶段的参数校验，后续再沉淀业务错误码枚举。
     */
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

