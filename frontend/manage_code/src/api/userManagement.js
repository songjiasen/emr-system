import request from '../utils/request';

export function createManagedUser(type, data) {
  return request.post(`/user-management/${type}`, data);
}

export function fetchManagedUsers(type, params = {}) {
  return request.get(`/user-management/${type}`, { params });
}

export function fetchManagedUserDetail(type, id) {
  return request.get(`/user-management/${type}/${id}`);
}

export function updateManagedUser(type, id, data) {
  return request.put(`/user-management/${type}/${id}`, data);
}

export function deleteManagedUser(type, id) {
  return request.delete(`/user-management/${type}/${id}`);
}

