package com.emr.billing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.billing.entity.FeeEntity;
import com.emr.billing.mapper.FeeMapper;
import com.emr.billing.service.BillingService;
import com.emr.common.PageResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据库版费用服务。
 * 用 `feiyong` 表替代原有内存 Map，保证费用和支付状态在服务重启后仍可追溯。
 */
@Service
public class DatabaseBillingService implements BillingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FeeMapper feeMapper;

    public DatabaseBillingService(FeeMapper feeMapper) {
        this.feeMapper = feeMapper;
    }

    /**
     * 新增费用记录。
     * 金额收敛为 BigDecimal，并把老前端的 relatedBusiness 字段统一映射到当前业务来源字段。
     */
    @Override
    public Map<String, Object> createFee(Map<String, Object> request) {
        Map<String, Object> payload = safePayload(request);

        FeeEntity entity = new FeeEntity();
        entity.setFeeNo(buildFeeNo());
        entity.setPatientId(requireId(payload.get("patientId"), "患者ID不能为空"));
        entity.setPatientName(requireText(payload.get("patientName"), "患者姓名不能为空"));
        entity.setBusinessType(resolveBusinessType(payload));
        entity.setBusinessId(resolveBusinessId(payload));
        entity.setFeeItem(requireText(firstPresent(payload, "feeItem", "feeType"), "费用项目不能为空"));
        entity.setAmount(requireAmount(payload.get("amount")));
        entity.setPayStatus("unpaid");
        entity.setPayTime(null);
        entity.setRemark(blankToNull(payload.get("remark")));
        feeMapper.insert(entity);
        return toRow(entity);
    }

    /**
     * 查询费用列表。
     * 保持按主键倒序返回，兼容前端默认看最新收费记录的体验。
     */
    @Override
    public PageResult<Map<String, Object>> listFees(Long patientId, String payStatus, String businessType, Long businessId, int page, int limit) {
        LambdaQueryWrapper<FeeEntity> wrapper = new LambdaQueryWrapper<FeeEntity>()
                .orderByDesc(FeeEntity::getId);
        if (patientId != null) {
            wrapper.eq(FeeEntity::getPatientId, patientId);
        }
        if (blankToNull(payStatus) != null) {
            wrapper.eq(FeeEntity::getPayStatus, payStatus.trim());
        }
        if (blankToNull(businessType) != null) {
            wrapper.eq(FeeEntity::getBusinessType, businessType.trim());
        }
        if (businessId != null) {
            wrapper.eq(FeeEntity::getBusinessId, businessId);
        }
        List<Map<String, Object>> rows = feeMapper.selectList(wrapper).stream()
                .map(this::toRow)
                .toList();
        return PageResult.of(rows, page, limit);
    }

    /**
     * 查询单条费用记录。
     * 费用详情和支付结果页都走这条链路，因此这里统一做不存在校验。
     */
    @Override
    public Map<String, Object> getFee(Long id) {
        return toRow(requireFee(id));
    }

    /**
     * 修改费用记录。
     * 已支付或已退费费用不允许修改，避免账目已结算后再发生字段漂移。
     */
    @Override
    public Map<String, Object> updateFee(Long id, Map<String, Object> request) {
        FeeEntity entity = requireFee(id);
        ensureEditable(entity, "已支付或已退费费用不允许修改");

        Map<String, Object> payload = safePayload(request);
        if (payload.containsKey("businessType") || payload.containsKey("relatedBusinessType")) {
            entity.setBusinessType(resolveBusinessType(payload));
        }
        if (payload.containsKey("businessId") || payload.containsKey("relatedBusinessId")) {
            entity.setBusinessId(resolveBusinessId(payload));
        }
        if (payload.containsKey("feeItem") || payload.containsKey("feeType")) {
            entity.setFeeItem(requireText(firstPresent(payload, "feeItem", "feeType"), "费用项目不能为空"));
        }
        if (payload.containsKey("amount")) {
            entity.setAmount(requireAmount(payload.get("amount")));
        }
        if (payload.containsKey("remark")) {
            entity.setRemark(blankToNull(payload.get("remark")));
        }
        feeMapper.updateById(entity);
        return toRow(entity);
    }

    /**
     * 支付费用。
     * 只允许未支付费用执行一次支付，并在落库时同步写入支付时间。
     */
    @Override
    public Map<String, Object> payFee(Long id) {
        FeeEntity entity = requireFee(id);
        if (!"unpaid".equals(entity.getPayStatus())) {
            throw new IllegalArgumentException("当前费用已支付或已退费");
        }
        entity.setPayStatus("paid");
        entity.setPayTime(LocalDateTime.now());
        feeMapper.updateById(entity);
        return toRow(entity);
    }

    /**
     * 删除费用记录。
     * 当前仍采用物理删除，但只对未支付费用开放，和设计文档约束保持一致。
     */
    @Override
    public Map<String, Object> deleteFee(Long id) {
        FeeEntity entity = requireFee(id);
        ensureEditable(entity, "已支付或已退费费用不允许删除");
        feeMapper.deleteById(entity.getId());
        return toRow(entity);
    }

    private FeeEntity requireFee(Long id) {
        Long feeId = requireId(id, "费用ID不能为空");
        FeeEntity entity = feeMapper.selectById(feeId);
        if (entity == null) {
            throw new IllegalArgumentException("费用记录不存在");
        }
        return entity;
    }

    private void ensureEditable(FeeEntity entity, String message) {
        if (!"unpaid".equals(entity.getPayStatus())) {
            throw new IllegalArgumentException(message);
        }
    }

    private Map<String, Object> toRow(FeeEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("feeNo", entity.getFeeNo());
        row.put("patientId", entity.getPatientId());
        row.put("patientName", entity.getPatientName());
        row.put("businessType", entity.getBusinessType());
        row.put("businessId", entity.getBusinessId());
        row.put("feeItem", entity.getFeeItem());
        row.put("amount", entity.getAmount());
        row.put("payStatus", entity.getPayStatus());
        row.put("status", entity.getPayStatus());
        row.put("payTime", formatDateTime(entity.getPayTime()));
        row.put("remark", entity.getRemark());
        return row;
    }

    private String buildFeeNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "FEE" + LocalDate.now().format(DATE_FORMATTER) + suffix;
    }

    private String resolveBusinessType(Map<String, Object> payload) {
        String businessType = blankToNull(firstPresent(payload, "businessType", "relatedBusinessType"));
        return businessType == null ? "manual" : businessType;
    }

    private Long resolveBusinessId(Map<String, Object> payload) {
        return optionalId(firstPresent(payload, "businessId", "relatedBusinessId"));
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATETIME_FORMATTER);
    }

    /**
     * Map 请求体为空时返回空 Map。
     * 这样缺失字段会统一走显式业务提示，而不是在后续字段读取里触发空指针。
     */
    private Map<String, Object> safePayload(Map<String, Object> request) {
        return request == null ? Map.of() : request;
    }

    private Object firstPresent(Map<String, Object> payload, String firstKey, String secondKey) {
        if (payload.containsKey(firstKey)) {
            return payload.get(firstKey);
        }
        return payload.get(secondKey);
    }

    private Long requireId(Object value, String message) {
        Long id = optionalId(value);
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
        return id;
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

    private BigDecimal requireAmount(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("费用金额不能为空");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(value.toString().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("费用金额格式不正确");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("费用金额不能为负数");
        }
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private String blankToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
