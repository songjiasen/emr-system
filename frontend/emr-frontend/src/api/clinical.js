import request from '../utils/request';

export function createMedicalOrder(data) {
  return request.post('/medical-orders', data);
}

export function fetchMedicalOrders(params = {}) {
  return request.get('/medical-orders', { params });
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

export function createTestRequest(data) {
  return request.post('/test-requests', data);
}

export function fetchTestRequests(params = {}) {
  return request.get('/test-requests', { params });
}

