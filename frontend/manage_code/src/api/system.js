import request from '../utils/request';

export function createNews(data) {
  return request.post('/news', data);
}

export function fetchNews(params = {}) {
  return request.get('/news', { params });
}

export function updateNews(id, data) {
  return request.put(`/news/${id}`, data);
}

export function deleteNews(id) {
  return request.delete(`/news/${id}`);
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

export function deleteMessage(id) {
  return request.delete(`/messages/${id}`);
}

export function createCarousel(data) {
  return request.post('/carousels', data);
}

export function fetchCarousels(params = {}) {
  return request.get('/carousels', { params });
}

export function updateCarousel(id, data) {
  return request.put(`/carousels/${id}`, data);
}

export function deleteCarousel(id) {
  return request.delete(`/carousels/${id}`);
}

export function saveConfig(data) {
  return request.post('/config', data);
}

export function saveMenu(data) {
  return request.post('/menus', data);
}

export function fetchMenu(roleCode) {
  return request.get(`/menus/${roleCode}`);
}

export function fetchSyslogs(params = {}) {
  return request.get('/syslogs', { params });
}
