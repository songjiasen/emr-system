import axios from 'axios';
import { AUTH_EVENT_NAME, clearAuthState, getStoredToken } from './session';

const request = axios.create({
  baseURL: '/cl584734139',
  timeout: 10000
});

request.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers.Token = token;
  }
  return config;
});

let lastAuthEventTime = 0;

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if (status === 401 || status === 403) {
      const now = Date.now();
      if (typeof window !== 'undefined' && now - lastAuthEventTime > 400) {
        lastAuthEventTime = now;
        window.dispatchEvent(new CustomEvent(AUTH_EVENT_NAME, {
          detail: {
            status,
            clearState: false,
            message: error?.response?.data?.message || '登录状态异常'
          }
        }));
      }
    }
    return Promise.reject(error);
  }
);

export default request;
