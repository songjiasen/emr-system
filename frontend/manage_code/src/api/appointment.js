import request from '../utils/request';

export function fetchAppointments(params = {}) {
  return request.get('/appointments', { params });
}

export function fetchAppointmentDetail(id) {
  return request.get(`/appointments/${id}`);
}

export function cancelAppointment(id, data = {}) {
  return request.post(`/appointments/${id}/cancel`, data);
}

