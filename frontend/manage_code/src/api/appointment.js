import request from '../utils/request';

export function fetchAppointments(params = {}) {
  return request.get('/appointments', { params });
}

export function fetchAppointmentDetail(id) {
  return request.get(`/appointments/${id}`);
}

export function confirmAppointment(id) {
  return request.post(`/appointments/${id}/confirm`);
}

export function cancelAppointment(id, data = {}) {
  return request.post(`/appointments/${id}/cancel`, data);
}
