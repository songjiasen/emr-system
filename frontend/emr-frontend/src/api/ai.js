import request from '../utils/request';

export function ocrMedicalRecord(data) {
  return request.post('/ai/ocr', data);
}

export function recommendMedicine(data) {
  return request.post('/ai/recommend-medicine', data);
}

export function auditPrescription(data) {
  return request.post('/ai/prescription-audit', data);
}

export function smartSearch(data) {
  return request.post('/ai/smart-search', data);
}

