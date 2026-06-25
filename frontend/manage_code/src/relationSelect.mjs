import { normalizeRecordVisitTime } from './recordForm.mjs';

function normalizeId(item, type) {
  if (!item) {
    return null;
  }
  if (item.id) {
    return item.id;
  }
  if (type === 'patient') {
    return item.patientId || null;
  }
  if (type === 'doctor') {
    return item.doctorId || null;
  }
  if (type === 'record') {
    return item.recordId || null;
  }
  if (type === 'appointment') {
    return item.appointmentId || null;
  }
  return null;
}

function normalizeName(item, type) {
  if (!item) {
    return '';
  }
  if (type === 'patient') {
    return item.name || item.patientName || item.username || '';
  }
  if (type === 'doctor') {
    return item.name || item.doctorName || item.username || '';
  }
  return item.name || item.patientName || item.doctorName || item.username || '';
}

/**
 * 合并可选关联对象。
 * 业务列表和人员列表字段名不同，这里统一成 `{ id, name, raw }`，供下拉组件复用。
 */
export function mergeRelationOptions(items, type) {
  const seen = new Set();
  return (items || []).flatMap((item) => {
    const id = normalizeId(item, type);
    if (!id || seen.has(id)) {
      return [];
    }
    seen.add(id);
    return [{ id, name: normalizeName(item, type), raw: item }];
  });
}

/**
 * 关联对象下拉展示文案。
 * 保留 ID 仅作为辅助辨识，不要求使用者手动输入。
 */
export function relationLabel(option, type) {
  if (!option) {
    return '';
  }
  const name = option.name || normalizeName(option.raw, type) || '未命名';
  const id = option.id || normalizeId(option.raw, type);
  return id ? `${name}（ID ${id}）` : name;
}

export function appointmentLabel(option) {
  const item = option?.raw || option || {};
  const no = item.appointmentNo || `预约 ${option?.id || item.id || ''}`;
  const patient = item.patientName || '未知患者';
  const doctor = item.doctorName || '未知医生';
  return `${no} / ${patient} / ${doctor}`;
}

export function recordLabel(option) {
  const item = option?.raw || option || {};
  const no = item.recordNo || `病历 ${option?.id || item.id || ''}`;
  const patient = item.patientName || '未知患者';
  const diagnosis = item.diagnosis || '未填诊断';
  return `${no} / ${patient} / ${diagnosis}`;
}

export function applyPatientToForm(form, option) {
  const raw = option?.raw || option || {};
  form.patientId = option?.id || raw.id || raw.patientId || null;
  form.patientName = option?.name || normalizeName(raw, 'patient');
}

export function applyDoctorToForm(form, option) {
  const raw = option?.raw || option || {};
  form.doctorId = option?.id || raw.id || raw.doctorId || null;
  form.doctorName = option?.name || normalizeName(raw, 'doctor');
}

export function applyAppointmentToRecordForm(form, option) {
  const raw = option?.raw || option || {};
  form.appointmentId = raw.id || option?.id || raw.appointmentId || null;
  form.appointmentNo = raw.appointmentNo || '';
  form.patientId = raw.patientId || null;
  form.patientName = raw.patientName || '';
  form.doctorId = raw.doctorId || null;
  form.doctorName = raw.doctorName || '';
  form.visitTime = normalizeRecordVisitTime(raw.appointmentTime || form.visitTime || '');
}

export function applyRecordToClinicalForm(form, option) {
  const raw = option?.raw || option || {};
  form.recordId = raw.id || option?.id || raw.recordId || null;
  form.patientId = raw.patientId || null;
  form.patientName = raw.patientName || '';
  form.doctorId = raw.doctorId || null;
  form.doctorName = raw.doctorName || '';
}
