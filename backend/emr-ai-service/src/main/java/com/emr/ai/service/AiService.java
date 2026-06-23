package com.emr.ai.service;

import java.util.Map;

/**
 * AI 智能服务。
 * 承接 OCR、荐药、处方审核、智能检索以及对应的调用日志落库。
 */
public interface AiService {
    Map<String, Object> ocr(Map<String, Object> request);
    Map<String, Object> recommendMedicine(Map<String, Object> request);
    Map<String, Object> prescriptionAudit(Map<String, Object> request);
    Map<String, Object> smartSearch(Map<String, Object> request);
}
