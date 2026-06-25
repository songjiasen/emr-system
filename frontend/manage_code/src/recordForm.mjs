/**
 * 规范化病历就诊时间。
 * 后端病历服务按 `yyyy-MM-dd HH:mm:ss` 解析，这里兼容前端常见的分钟级输入，避免提交时被后端拒绝。
 */
export function normalizeRecordVisitTime(value) {
  const text = String(value || '').trim().replace('T', ' ');
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(text)) {
    return `${text}:00`;
  }
  return text;
}

/**
 * 构建病历新增/编辑提交载荷。
 * patientId/doctorId/appointmentId 是真实业务关联字段，姓名只作为展示快照一起提交。
 */
export function buildRecordSubmitPayload(form) {
  const payload = { ...(form || {}) };
  payload.visitTime = normalizeRecordVisitTime(payload.visitTime);
  return payload;
}
