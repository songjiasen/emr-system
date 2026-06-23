import request from '../utils/request';

export function createFee(data) {
  return request.post('/fees', data);
}

export function fetchFees(params = {}) {
  return request.get('/fees', { params });
}

export function payFee(id) {
  return request.post(`/fees/${id}/pay`);
}

export function updateFee(id, data) {
  return request.put(`/fees/${id}`, data);
}

export function deleteFee(id) {
  return request.delete(`/fees/${id}`);
}
