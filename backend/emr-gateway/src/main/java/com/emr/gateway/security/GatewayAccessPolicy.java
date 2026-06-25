package com.emr.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 网关访问策略。
 * 以文档中的角色职责为边界，在路由转发前完成匿名白名单和角色级粗粒度校验。
 */
@Component
public class GatewayAccessPolicy {

    private static final String PREFIX = "/cl584734139";
    private static final Set<String> ADMIN_ROLES = Set.of("admin", "super_admin");

    /**
     * 判断请求是否允许匿名访问。
     * 只开放登录注册、找回密码、健康检查和患者端首页所需的公开查询。
     */
    public boolean isPublic(HttpMethod method, String path) {
        if (HttpMethod.OPTIONS.equals(method) || "/health".equals(path)) {
            return true;
        }
        if (HttpMethod.POST.equals(method)) {
            return pathEquals(path, "/auth/login")
                    || pathEquals(path, "/auth/patient/register")
                    || pathEquals(path, "/auth/password/reset");
        }
        return HttpMethod.GET.equals(method) && (
                pathStartsWith(path, "/doctors")
                        || pathStartsWith(path, "/departments")
                        || pathStartsWith(path, "/news")
                        || pathStartsWith(path, "/carousels")
        );
    }

    /**
     * 判断登录角色是否可访问目标接口。
     * 管理员拥有全模块权限，其余角色按需求文档中的工作职责分配最小权限。
     */
    public boolean isAllowed(String roleCode, HttpMethod method, String path) {
        if (roleCode == null || roleCode.isBlank() || method == null || path == null) {
            return false;
        }
        if (ADMIN_ROLES.contains(roleCode)) {
            return true;
        }
        if (pathStartsWith(path, "/auth") || pathStartsWith(path, "/messages") || pathStartsWith(path, "/ai")) {
            return true;
        }
        return switch (roleCode) {
            case "patient" -> isPatientAllowed(method, path);
            case "doctor" -> isDoctorAllowed(method, path);
            case "nurse" -> isNurseAllowed(method, path);
            case "director" -> isDirectorAllowed(method, path);
            default -> false;
        };
    }

    private boolean isPatientAllowed(HttpMethod method, String path) {
        if (pathStartsWith(path, "/appointments")) {
            return true;
        }
        if (pathStartsWith(path, "/medical-records")
                || pathStartsWith(path, "/prescriptions")) {
            return HttpMethod.GET.equals(method);
        }
        if (pathStartsWith(path, "/test-requests")) {
            return HttpMethod.GET.equals(method) || (HttpMethod.POST.equals(method) && path.endsWith("/pay"));
        }
        if (pathStartsWith(path, "/fees")) {
            return HttpMethod.GET.equals(method) || HttpMethod.POST.equals(method);
        }
        // 患者端只需要读取余额用于支付页展示；扣款仍由计费服务内部调用用户服务完成。
        if (pathStartsWith(path, "/user-management/patients")
                && HttpMethod.GET.equals(method)
                && path.endsWith("/balance")) {
            return true;
        }
        return false;
    }

    private boolean isDoctorAllowed(HttpMethod method, String path) {
        return pathStartsWith(path, "/appointments")
                || pathStartsWith(path, "/medical-records")
                || pathStartsWith(path, "/medical-record-templates")
                || pathStartsWith(path, "/medical-record-archives")
                || pathStartsWith(path, "/medical-orders")
                || pathStartsWith(path, "/prescriptions")
                || pathStartsWith(path, "/test-requests");
    }

    private boolean isNurseAllowed(HttpMethod method, String path) {
        if (pathStartsWith(path, "/appointments")) {
            return HttpMethod.GET.equals(method);
        }
        if (pathStartsWith(path, "/triage-records")
                || pathStartsWith(path, "/inpatients")) {
            return true;
        }
        if (pathStartsWith(path, "/medical-records")) {
            return HttpMethod.GET.equals(method) || HttpMethod.POST.equals(method);
        }
        if (pathStartsWith(path, "/medical-orders")) {
            return HttpMethod.GET.equals(method) || (HttpMethod.POST.equals(method) && path.endsWith("/execute"));
        }
        return false;
    }

    private boolean isDirectorAllowed(HttpMethod method, String path) {
        if (pathStartsWith(path, "/workflow")
                || pathStartsWith(path, "/medical-record-archives")) {
            return true;
        }
        if (pathStartsWith(path, "/medical-records")) {
            return HttpMethod.GET.equals(method);
        }
        if (pathStartsWith(path, "/medical-orders") || pathStartsWith(path, "/test-requests")) {
            return HttpMethod.GET.equals(method) || (HttpMethod.POST.equals(method) && path.endsWith("/audit-result"));
        }
        return pathStartsWith(path, "/prescriptions") && HttpMethod.GET.equals(method);
    }

    private boolean pathEquals(String path, String endpoint) {
        return (PREFIX + endpoint).equals(path);
    }

    private boolean pathStartsWith(String path, String endpoint) {
        String fullPath = PREFIX + endpoint;
        return fullPath.equals(path) || path.startsWith(fullPath + "/");
    }
}
