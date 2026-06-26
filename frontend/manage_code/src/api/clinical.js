import request from '../utils/request';

export function createMedicalOrder(data) {
  return request.post('/medical-orders', data);
}

export function fetchMedicalOrders(params = {}) {
  return request.get('/medical-orders', { params });
}

export function updateMedicalOrder(id, data) {
  return request.put(`/medical-orders/${id}`, data);
}

export function deleteMedicalOrder(id) {
  return request.delete(`/medical-orders/${id}`);
}

export function updateMedicalOrderAuditResult(id, data) {
  return request.post(`/medical-orders/${id}/audit-result`, data);
}

export function executeMedicalOrder(id, data) {
  return request.post(`/medical-orders/${id}/execute`, data);
}

export function createPrescription(data) {
  return request.post('/prescriptions', data);
}

export function fetchPrescriptions(params = {}) {
  return request.get('/prescriptions', { params });
}

export function updatePrescription(id, data) {
  return request.put(`/prescriptions/${id}`, data);
}

export function deletePrescription(id) {
  return request.delete(`/prescriptions/${id}`);
}

export function auditPrescriptionResult(id, data) {
  return request.post(`/prescriptions/${id}/audit-result`, data);
}

export function fetchMedicines() {
  return request.get('/medicines');
}

export function fetchTestItems() {
  return request.get('/test-items');
}

export function createTestItem(data) {
  return request.post('/test-items', data);
}

export function updateTestItem(id, data) {
  return request.put(`/test-items/${id}`, data);
}

export function deleteTestItem(id) {
  return request.delete(`/test-items/${id}`);
}

export function createTestRequest(data) {
  return request.post('/test-requests', data);
}

export function fetchTestRequests(params = {}) {
  return request.get('/test-requests', { params });
}

export function updateTestAuditResult(id, data) {
  return request.post(`/test-requests/${id}/audit-result`, data);
}

export function updateTestRequest(id, data) {
  return request.put(`/test-requests/${id}`, data);
}

export function deleteTestRequest(id) {
  return request.delete(`/test-requests/${id}`);
}
