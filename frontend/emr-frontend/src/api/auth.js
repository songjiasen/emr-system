import request from '../utils/request';

export function login(data) {
  return request.post('/auth/login', data);
}

export function registerPatient(data) {
  return request.post('/auth/patient/register', data);
}

export function validateToken() {
  return request.post('/auth/token/validate');
}

export function changePassword(data) {
  return request.post('/auth/password/change', data);
}

export function resetPassword(data) {
  return request.post('/auth/password/reset', data);
}

export function logout() {
  return request.post('/auth/logout');
}
