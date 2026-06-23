package com.emr.billing.service;

import com.emr.common.PageResult;

import java.util.Map;

/**
 * 费用管理服务。
 * 承接费用新增、查询、修改、支付和删除的业务规则。
 */
public interface BillingService {

    /**
     * 新增费用记录。
     * 需要保证患者信息、费用项目和金额完整，同时兼容旧前端的 relatedBusiness 字段命名。
     */
    Map<String, Object> createFee(Map<String, Object> request);

    /**
     * 查询费用列表。
     * 支持按患者、支付状态、费用来源类型和来源业务 ID 过滤。
     */
    PageResult<Map<String, Object>> listFees(Long patientId, String payStatus, String businessType, Long businessId, int page, int limit);

    /**
     * 查询单条费用记录。
     * 未找到时返回明确业务错误，避免患者端误展示空数据。
     */
    Map<String, Object> getFee(Long id);

    /**
     * 修改费用记录。
     * 只有未支付费用允许修改，避免支付后账目发生变化。
     */
    Map<String, Object> updateFee(Long id, Map<String, Object> request);

    /**
     * 支付费用。
     * 只允许从 unpaid 流转到 paid，并补齐支付时间。
     */
    Map<String, Object> payFee(Long id);

    /**
     * 删除费用记录。
     * 只允许删除未支付费用，已支付或已退费记录必须保留以便追溯。
     */
    Map<String, Object> deleteFee(Long id);
}
