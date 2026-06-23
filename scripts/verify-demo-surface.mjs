import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');

function read(path) {
  return readFileSync(resolve(root, path), 'utf8');
}

function assertIncludes(file, snippets) {
  const text = read(file);
  const missing = snippets.filter((snippet) => !text.includes(snippet));

  if (missing.length > 0) {
    throw new Error(`${file} 缺少演示入口: ${missing.join(', ')}`);
  }
}

assertIncludes('backend/emr-gateway/src/main/resources/application.yml', [
  '/cl584734139/auth/**',
  '/cl584734139/doctors/**',
  '/cl584734139/appointments/**',
  '/cl584734139/medical-records/**',
  '/cl584734139/medical-orders/**',
  '/cl584734139/workflow/**',
  '/cl584734139/fees/**',
  '/cl584734139/news/**',
  '/cl584734139/carousels/**',
  '/cl584734139/ai/**'
]);

assertIncludes('frontend/emr-frontend/src/App.vue', [
  'submitLogin',
  'submitAppointment',
  'loadRecords',
  'submitMessage',
  'runAiSearch',
  'fetchAppointments',
  'fetchMedicalRecords',
  'fetchFees',
  'fetchNews'
]);

assertIncludes('frontend/manage_code/src/App.vue', [
  'submitAdminLogin',
  'createDepartmentAction',
  'createUserAction',
  'createTriageAction',
  'createAdmissionAction',
  'createRecordAction',
  'createTemplateAction',
  'auditOrderAction',
  'payFeeAction',
  'createCarouselAction',
  'saveConfigAction',
  'runOcrAction',
  'fetchManagedUsers'
]);

console.log('demo surface ok');
