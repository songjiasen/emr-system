package com.emr.common;

/**
 * 网关透传的可信登录上下文。
 * 业务服务只在请求头完整时启用角色和数据范围约束；无头场景继续兼容单服务测试和内部回调。
 */
public record TrustedUserContext(Long userId, String username, String roleCode, String tableName, Long departmentId, String departmentName) {

    public TrustedUserContext(Long userId, String username, String roleCode, String tableName) {
        this(userId, username, roleCode, tableName, null, null);
    }

    public static TrustedUserContext fromHeaders(String userIdHeader, String username, String roleCode, String tableName) {
        Long userId = parseLong(userIdHeader);
        return new TrustedUserContext(userId, blankToNull(username), blankToNull(roleCode), blankToNull(tableName), null, null);
    }

    public static TrustedUserContext fromHeaders(String userIdHeader, String username, String roleCode, String tableName, String departmentIdHeader, String departmentName) {
        Long userId = parseLong(userIdHeader);
        Long departmentId = parseLong(departmentIdHeader);
        return new TrustedUserContext(userId, blankToNull(username), blankToNull(roleCode), blankToNull(tableName), departmentId, blankToNull(departmentName));
    }

    public boolean authenticated() {
        return userId != null && roleCode != null;
    }

    public boolean isPatient() {
        return "patient".equals(roleCode);
    }

    public boolean isDoctor() {
        return "doctor".equals(roleCode);
    }

    public boolean isNurse() {
        return "nurse".equals(roleCode);
    }

    public boolean isDirector() {
        return "director".equals(roleCode);
    }

    public boolean isAdminLike() {
        return "admin".equals(roleCode) || "super_admin".equals(roleCode);
    }

    public boolean matchesUserId(Long candidate) {
        return userId != null && candidate != null && userId.equals(candidate);
    }

    private static Long parseLong(String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
