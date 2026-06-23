import request from '../utils/request';

export function fetchDoctors(params = {}) {
  return request.get('/doctors', { params });
}

export function fetchDoctorDetail(id) {
  return request.get(`/doctors/${id}`);
}

