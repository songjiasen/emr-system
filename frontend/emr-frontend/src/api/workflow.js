import request from '../utils/request';

export function createWorkflowTask(data) {
  return request.post('/workflow/tasks', data);
}

export function fetchWorkflowTasks(params = {}) {
  return request.get('/workflow/tasks', { params });
}

export function auditWorkflowTask(id, data) {
  return request.post(`/workflow/tasks/${id}/audit`, data);
}

export function fetchAuditRecords(params = {}) {
  return request.get('/workflow/audit-records', { params });
}

