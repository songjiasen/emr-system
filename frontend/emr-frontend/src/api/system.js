import request from '../utils/request';

export function createNews(data) {
  return request.post('/news', data);
}

export function fetchNews(params = {}) {
  return request.get('/news', { params });
}

export function fetchCarousels(params = {}) {
  return request.get('/carousels', { params });
}

export function createMessage(data) {
  return request.post('/messages', data);
}

export function fetchMessages(params = {}) {
  return request.get('/messages', { params });
}

export function replyMessage(id, data) {
  return request.post(`/messages/${id}/reply`, data);
}
