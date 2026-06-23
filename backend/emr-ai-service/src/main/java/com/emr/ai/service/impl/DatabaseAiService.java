package com.emr.ai.service.impl;

import com.emr.ai.entity.AiRequestLogEntity;
import com.emr.ai.mapper.AiRequestLogMapper;
import com.emr.ai.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 数据库版 AI 服务。
 * 保留 mock provider 兜底，同时把四类 AI 调用统一记录到 `ai_request_log`，智能检索优先读取病历服务数据。
 */
@Service
public class DatabaseAiService implements AiService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int SUMMARY_MAX_LENGTH = 200;

    private final AiRequestLogMapper aiRequestLogMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String provider;
    private final String recordBaseUrl;

    public DatabaseAiService(
            AiRequestLogMapper aiRequestLogMapper,
            @Value("${EMR_AI_PROVIDER:mock}") String provider,
            @Value("${EMR_RECORD_BASE_URL:http://localhost:8104}") String recordBaseUrl
    ) {
        this.aiRequestLogMapper = aiRequestLogMapper;
        this.provider = provider;
        this.recordBaseUrl = recordBaseUrl;
    }

    /**
     * OCR mock 识别。
     * 至少要求文件地址或图片 base64 其一存在，识别结果按演示口径返回文本与结构化字段。
     */
    @Override
    public Map<String, Object> ocr(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        String fileUrl = blankToNull(payload.get("fileUrl"));
        String imageBase64 = blankToNull(payload.get("imageBase64"));
        String inputSummary = summarize("file=" + defaultText(fileUrl, "base64_image"));
        try {
            if (fileUrl == null && imageBase64 == null) {
                throw new IllegalArgumentException("fileUrl和imageBase64至少传一个");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("provider", provider);
            result.put("fileUrl", fileUrl);
            result.put("recognizedText", "患者主诉胸闷，建议结合心电图检查。");
            result.put("fields", Map.of("chiefComplaint", "胸闷", "suggestion", "心电图检查"));
            saveSuccessLog("ocr", payload, inputSummary, summarize("recognized=胸闷; suggestion=心电图检查"));
            return result;
        } catch (IllegalArgumentException exception) {
            saveFailedLog("ocr", payload, inputSummary, exception.getMessage());
            throw exception;
        }
    }

    /**
     * mock 荐药。
     * 当前根据诊断关键字返回可演示建议，不输出医疗承诺结论。
     */
    @Override
    public Map<String, Object> recommendMedicine(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        String diagnosis = requireText(payload.get("diagnosis"), "诊断不能为空");
        String inputSummary = summarize("diagnosis=" + diagnosis);
        try {
            List<String> recommendations = buildMedicineRecommendations(diagnosis);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("provider", provider);
            result.put("diagnosis", diagnosis);
            result.put("recommendations", recommendations);
            saveSuccessLog("recommend_medicine", payload, inputSummary, summarize(String.join(";", recommendations)));
            return result;
        } catch (IllegalArgumentException exception) {
            saveFailedLog("recommend_medicine", payload, inputSummary, exception.getMessage());
            throw exception;
        }
    }

    /**
     * mock 处方审核。
     * 支持空处方拦截、重复药品识别和基础禁忌关键词提示，便于答辩时展示规则型审核结果。
     */
    @Override
    public Map<String, Object> prescriptionAudit(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        String prescriptionText = blankToNull(payload.get("prescriptionText"));
        List<String> medicineNames = collectMedicineNames(prescriptionText, payload.get("medicines"));
        String inputSummary = summarize("prescription=" + defaultText(prescriptionText, String.join(",", medicineNames)));
        try {
            List<String> warnings = new ArrayList<>();
            List<String> suggestions = new ArrayList<>();
            boolean passed = true;

            if (medicineNames.isEmpty()) {
                warnings.add("处方内容不能为空");
                suggestions.add("请至少录入一种药品");
                passed = false;
            }

            Set<String> seen = new LinkedHashSet<>();
            Set<String> duplicates = new LinkedHashSet<>();
            for (String medicineName : medicineNames) {
                String normalized = medicineName.toLowerCase();
                if (!seen.add(normalized)) {
                    duplicates.add(medicineName);
                }
                if (medicineName.contains("青霉素过敏") || medicineName.contains("禁用")) {
                    warnings.add("检测到潜在禁忌关键词: " + medicineName);
                    passed = false;
                }
            }
            if (!duplicates.isEmpty()) {
                warnings.add("存在重复药品: " + String.join("、", duplicates));
                suggestions.add("请确认是否需要合并重复药品");
                passed = false;
            }
            if (warnings.isEmpty()) {
                suggestions.add("未发现明显冲突，仍需医生最终确认");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("provider", provider);
            result.put("prescriptionText", prescriptionText);
            result.put("passed", passed);
            result.put("warnings", warnings);
            result.put("suggestions", suggestions);
            saveSuccessLog("prescription_audit", payload, inputSummary, summarize("passed=" + passed + ";warnings=" + String.join("|", warnings)));
            return result;
        } catch (IllegalArgumentException exception) {
            saveFailedLog("prescription_audit", payload, inputSummary, exception.getMessage());
            throw exception;
        }
    }

    /**
     * 智能检索。
     * 优先请求病历服务列表接口并在本地做关键词过滤；若病历服务不可用，则返回空结果并记失败日志，不影响当前 AI 服务可用性。
     */
    @Override
    public Map<String, Object> smartSearch(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);
        String keyword = requireText(payload.get("keyword"), "keyword不能为空");
        Long patientId = optionalId(payload.get("patientId"));
        Long doctorId = optionalId(payload.get("doctorId"));
        String inputSummary = summarize("keyword=" + keyword + ";patientId=" + defaultText(patientId == null ? null : patientId.toString(), "-") + ";doctorId=" + defaultText(doctorId == null ? null : doctorId.toString(), "-"));

        List<Map<String, Object>> results;
        boolean degraded = false;
        String errorMessage = null;
        try {
            results = loadSmartSearchResults(keyword, patientId, doctorId);
        } catch (RestClientException | IllegalArgumentException exception) {
            results = List.of();
            degraded = true;
            errorMessage = "病历服务调用失败";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", provider);
        result.put("keyword", keyword);
        result.put("results", results);

        if (degraded) {
            saveFailedLog("smart_search", payload, inputSummary, errorMessage);
        } else {
            saveSuccessLog("smart_search", payload, inputSummary, summarize("resultCount=" + results.size()));
        }
        return result;
    }

    private List<Map<String, Object>> loadSmartSearchResults(String keyword, Long patientId, Long doctorId) {
        StringBuilder url = new StringBuilder(recordBaseUrl)
                .append("/medical-records?page=1&limit=100");
        if (patientId != null) {
            url.append("&patientId=").append(patientId);
        }
        if (doctorId != null) {
            url.append("&doctorId=").append(doctorId);
        }
        ResponseEntity<Map> response = restTemplate.getForEntity(url.toString(), Map.class);
        Map body = response.getBody();
        if (body == null || !Integer.valueOf(0).equals(body.get("code"))) {
            throw new RestClientException("病历服务响应异常");
        }
        Map data = (Map) body.get("data");
        List<Map<String, Object>> rows = data == null ? List.of() : (List<Map<String, Object>>) data.getOrDefault("rows", List.of());
        String normalizedKeyword = keyword.toLowerCase();
        return rows.stream()
                .filter(row -> matchesKeyword(row, normalizedKeyword))
                .map(this::toSearchResult)
                .toList();
    }

    private boolean matchesKeyword(Map<String, Object> row, String keyword) {
        return containsIgnoreCase(row.get("recordNo"), keyword)
                || containsIgnoreCase(row.get("patientName"), keyword)
                || containsIgnoreCase(row.get("diagnosis"), keyword)
                || containsIgnoreCase(row.get("chiefComplaint"), keyword)
                || containsIgnoreCase(row.get("treatmentAdvice"), keyword);
    }

    private Map<String, Object> toSearchResult(Map<String, Object> row) {
        String diagnosis = blankToNull(row.get("diagnosis"));
        String complaint = blankToNull(row.get("chiefComplaint"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordNo", row.get("recordNo"));
        result.put("patientName", row.get("patientName"));
        result.put("diagnosis", diagnosis);
        result.put("summary", summarize(defaultText(diagnosis, complaint)));
        return result;
    }

    private List<String> buildMedicineRecommendations(String diagnosis) {
        String text = diagnosis.toLowerCase();
        if (text.contains("感染")) {
            return List.of("建议结合血常规结果评估是否需要抗感染治疗", "注意补液和休息", "必要时复诊复查");
        }
        if (text.contains("高血压") || text.contains("血压")) {
            return List.of("建议持续监测血压", "遵医嘱调整降压方案", "关注低盐饮食");
        }
        if (text.contains("胃")) {
            return List.of("建议清淡饮食", "按医嘱评估胃黏膜保护用药", "出现加重症状及时复诊");
        }
        return List.of("建议结合检查结果对症治疗", "按医嘱复诊", "必要时完善进一步检查");
    }

    private List<String> collectMedicineNames(String prescriptionText, Object medicinesValue) {
        List<String> result = new ArrayList<>();
        if (prescriptionText != null) {
            for (String part : prescriptionText.split("[,，;；\\n]")) {
                String text = part.trim();
                if (!text.isEmpty()) {
                    result.add(text);
                }
            }
        }
        if (medicinesValue instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object name = map.get("medicineName");
                    if (blankToNull(name) != null) {
                        result.add(blankToNull(name));
                    }
                } else if (blankToNull(item) != null) {
                    result.add(blankToNull(item));
                }
            }
        }
        return result;
    }

    private void saveSuccessLog(String aiType, Map<String, Object> payload, String inputSummary, String resultSummary) {
        AiRequestLogEntity entity = baseLog(aiType, payload, inputSummary);
        entity.setResultSummary(resultSummary);
        entity.setStatus("success");
        aiRequestLogMapper.insert(entity);
    }

    private void saveFailedLog(String aiType, Map<String, Object> payload, String inputSummary, String errorMessage) {
        AiRequestLogEntity entity = baseLog(aiType, payload, inputSummary);
        entity.setStatus("failed");
        entity.setErrorMessage(summarize(errorMessage));
        aiRequestLogMapper.insert(entity);
    }

    private AiRequestLogEntity baseLog(String aiType, Map<String, Object> payload, String inputSummary) {
        AiRequestLogEntity entity = new AiRequestLogEntity();
        entity.setRequestNo(buildRequestNo());
        entity.setAiType(aiType);
        entity.setUserId(optionalId(payload.get("userId")));
        entity.setUsername(blankToNull(payload.get("username")));
        entity.setInputSummary(inputSummary);
        entity.setProvider(provider);
        return entity;
    }

    private String buildRequestNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "AI" + LocalDate.now().format(DATE_FORMATTER) + suffix;
    }

    private String summarize(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= SUMMARY_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, SUMMARY_MAX_LENGTH);
    }

    private boolean containsIgnoreCase(Object value, String keyword) {
        String text = blankToNull(value);
        return text != null && text.toLowerCase().contains(keyword);
    }

    /**
     * Map 请求体为空时返回空 Map。
     * 这样所有参数校验都能统一落到明确业务异常，同时日志也能拿到最基础的调用上下文。
     */
    private Map<String, Object> safePayload(Map<String, Object> request) {
        return request == null ? Map.of() : request;
    }

    private Long optionalId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ID格式不正确");
        }
    }

    private String requireText(Object value, String message) {
        String text = blankToNull(value);
        if (text == null) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String blankToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
