import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const appSource = readFileSync(new URL('./App.vue', import.meta.url), 'utf8');

test('分诊、入院、出院使用三个独立弹窗', () => {
  const tabsCloseIndex = appSource.indexOf('</el-tabs>');
  const triageDialogIndex = appSource.indexOf('v-model="adminDialogs.triage"');
  const admissionDialogIndex = appSource.indexOf('v-model="adminDialogs.admission"');
  const dischargeDialogIndex = appSource.indexOf('v-model="adminDialogs.discharge"');

  assert.ok(triageDialogIndex > tabsCloseIndex);
  assert.ok(admissionDialogIndex > tabsCloseIndex);
  assert.ok(dischargeDialogIndex > tabsCloseIndex);
  assert.equal(appSource.includes('v-model="adminDialogs.inpatient"'), false);
  assert.equal(appSource.includes('createTriageAction">创建分诊</el-button>\\n            <el-button @click="createAdmissionAction"'), false);
});
