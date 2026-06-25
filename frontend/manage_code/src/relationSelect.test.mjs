import assert from 'node:assert/strict';
import test from 'node:test';

import {
  applyAppointmentToRecordForm,
  applyPatientToForm,
  applyRecordToClinicalForm,
  mergeRelationOptions,
  relationLabel
} from './relationSelect.mjs';

test('关联下拉选项按 ID 去重并使用业务名称展示', () => {
  const options = mergeRelationOptions([
    { id: 1, name: '患者演示' },
    { patientId: 1, patientName: '患者演示重复' },
    { id: 2, username: 'doctor_demo' }
  ], 'patient');

  assert.deepEqual(options.map((item) => item.id), [1, 2]);
  assert.equal(relationLabel(options[0], 'patient'), '患者演示（ID 1）');
  assert.equal(relationLabel(options[1], 'patient'), 'doctor_demo（ID 2）');
});

test('选择患者后同步患者 ID 和姓名快照', () => {
  const form = { patientId: null, patientName: '' };

  applyPatientToForm(form, { id: 4101, name: '落库患者' });

  assert.deepEqual(form, { patientId: 4101, patientName: '落库患者' });
});

test('选择预约后同步病历关联字段和就诊时间', () => {
  const form = {};

  applyAppointmentToRecordForm(form, {
    id: 11,
    appointmentNo: 'APPT202606260011',
    patientId: 4101,
    patientName: '落库病历患者',
    doctorId: 21,
    doctorName: '落库医生',
    appointmentTime: '2026-06-26 09:10'
  });

  assert.deepEqual(form, {
    appointmentId: 11,
    appointmentNo: 'APPT202606260011',
    patientId: 4101,
    patientName: '落库病历患者',
    doctorId: 21,
    doctorName: '落库医生',
    visitTime: '2026-06-26 09:10:00'
  });
});

test('清空预约后清掉由预约带出的关联快照', () => {
  const form = {
    appointmentId: 11,
    appointmentNo: 'APPT202606260011',
    patientId: 4101,
    patientName: '落库病历患者',
    doctorId: 21,
    doctorName: '落库医生',
    visitTime: '2026-06-26 09:10:00'
  };

  applyAppointmentToRecordForm(form, null);

  assert.deepEqual(form, {
    appointmentId: null,
    appointmentNo: '',
    patientId: null,
    patientName: '',
    doctorId: null,
    doctorName: '',
    visitTime: '2026-06-26 09:10:00'
  });
});

test('选择病历后同步医嘱处方检查所需上下文', () => {
  const form = {};

  applyRecordToClinicalForm(form, {
    id: 31,
    patientId: 4101,
    patientName: '病历患者',
    doctorId: 21,
    doctorName: '病历医生'
  });

  assert.deepEqual(form, {
    recordId: 31,
    patientId: 4101,
    patientName: '病历患者',
    doctorId: 21,
    doctorName: '病历医生'
  });
});
