package com.emr.workflow.service;

/**
 * 审核结果回写客户端。
 * 工作流服务通过它把审核结果同步给病历、医嘱、检查等业务服务。
 */
public interface BusinessAuditCallbackClient {

    /**
     * 回写业务审核结果。
     * 不支持回写的业务类型允许直接跳过，回写失败则抛异常阻断当前审核提交。
     */
    void syncAuditResult(String businessType, Long businessId, Long auditorId, String auditorName, String auditResult, String auditOpinion);
}
