package com.emr.visit.service;

import com.emr.common.PageResult;

import java.util.Map;

/**
 * 分诊建档与住院管理服务。
 * 对外保持控制器现有返回结构不变，内部统一承接分诊、入院、出院的业务校验和持久化。
 */
public interface InpatientService {

    /**
     * 新增分诊建档记录。
     * 需要保证患者和护士主键信息完整，避免生成无归属的分诊数据。
     */
    Map<String, Object> createTriage(Map<String, Object> request);

    /**
     * 查询分诊建档列表。
     * 支持按患者、护士、状态和分诊级别过滤，兼容前端分页表格读取。
     */
    PageResult<Map<String, Object>> listTriage(Long patientId, Long nurseId, String status, String triageLevel, int page, int limit);

    /**
     * 新增入院登记。
     * 若请求中同时传入病房号和床位号，需要校验该床位当前没有住院中的占用记录。
     */
    Map<String, Object> createAdmission(Map<String, Object> request);

    /**
     * 查询入院列表。
     * 支持按患者、状态、病房和床位过滤，供护士站和后台住院列表共用。
     */
    PageResult<Map<String, Object>> listAdmissions(Long patientId, String status, String wardNo, String bedNo, int page, int limit);

    /**
     * 办理出院。
     * 必须基于有效的住院中记录执行，并确保同一入院记录不会重复生成第二条出院记录。
     */
    Map<String, Object> discharge(Long admissionId, Map<String, Object> request);

    /**
     * 查询出院列表。
     * 支持按患者和入院记录过滤，兼容住院结算和归档后的结果查询。
     */
    PageResult<Map<String, Object>> listDischarges(Long patientId, Long admissionId, int page, int limit);
}
