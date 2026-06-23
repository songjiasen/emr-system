import request from '../utils/request';

export function createMedicalRecord(data) {
  return request.post('/medical-records', data);
}

export function fetchMedicalRecords(params = {}) {
  return request.get('/medical-records', { params });
}

export function fetchMedicalRecordDetail(id) {
  return request.get(`/medical-records/${id}`);
}

export function updateMedicalRecord(id, data) {
  return request.put(`/medical-records/${id}`, data);
}

export function deleteMedicalRecord(id) {
  return request.delete(`/medical-records/${id}`);
}

export function uploadMedicalRecordFile(data) {
  return request.post('/medical-records/upload', data);
}

export function createMedicalRecordTemplate(data) {
  return request.post('/medical-record-templates', data);
}

export function fetchMedicalRecordTemplates(params = {}) {
  return request.get('/medical-record-templates', { params });
}

export function updateMedicalRecordTemplate(id, data) {
  return request.put(`/medical-record-templates/${id}`, data);
}

export function deleteMedicalRecordTemplate(id) {
  return request.delete(`/medical-record-templates/${id}`);
}

export function createArchiveApplication(data) {
  return request.post('/medical-record-archives/applications', data);
}

export function fetchArchiveApplications(params = {}) {
  return request.get('/medical-record-archives/applications', { params });
}

export function auditArchiveApplication(id, data) {
  return request.post(`/medical-record-archives/applications/${id}/audit`, data);
}

export function fetchArchives(params = {}) {
  return request.get('/medical-record-archives', { params });
}
