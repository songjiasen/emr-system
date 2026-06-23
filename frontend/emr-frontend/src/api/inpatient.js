import request from '../utils/request';

export function createTriageRecord(data) {
  return request.post('/triage-records', data);
}

export function fetchTriageRecords(params = {}) {
  return request.get('/triage-records', { params });
}

export function createAdmission(data) {
  return request.post('/inpatients/admissions', data);
}

export function fetchAdmissions(params = {}) {
  return request.get('/inpatients/admissions', { params });
}

export function dischargeAdmission(id, data) {
  return request.post(`/inpatients/admissions/${id}/discharge`, data);
}

export function fetchDischarges(params = {}) {
  return request.get('/inpatients/discharges', { params });
}

