import assert from 'node:assert/strict';
import test from 'node:test';

import {
  buildRecordSubmitPayload,
  normalizeRecordVisitTime
} from './recordForm.mjs';

test('病历就诊时间提交前补齐秒级格式', () => {
  assert.equal(normalizeRecordVisitTime('2026-06-22 10:00'), '2026-06-22 10:00:00');
  assert.equal(normalizeRecordVisitTime('2026-06-22T10:00'), '2026-06-22 10:00:00');
  assert.equal(normalizeRecordVisitTime('2026-06-22 10:00:30'), '2026-06-22 10:00:30');
});

test('病历提交载荷保留医生患者和预约关联字段', () => {
  const payload = buildRecordSubmitPayload({
    appointmentId: 11,
    appointmentNo: 'APPT202606260011',
    patientId: 4101,
    patientName: '落库病历患者',
    doctorId: 21,
    doctorName: '落库医生',
    visitTime: '2026-06-26 09:10',
    diagnosis: '血压波动'
  });

  assert.deepEqual(payload, {
    appointmentId: 11,
    appointmentNo: 'APPT202606260011',
    patientId: 4101,
    patientName: '落库病历患者',
    doctorId: 21,
    doctorName: '落库医生',
    visitTime: '2026-06-26 09:10:00',
    diagnosis: '血压波动'
  });
});
