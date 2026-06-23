import request from '../utils/request';

export function createDepartment(data) {
  return request.post('/departments', data);
}

export function fetchDepartments(params = {}) {
  return request.get('/departments', { params });
}

export function updateDepartment(id, data) {
  return request.put(`/departments/${id}`, data);
}

export function deleteDepartment(id) {
  return request.delete(`/departments/${id}`);
}

