export const AUTH_EVENT_NAME = 'emr-patient-auth-event';

const TOKEN_KEY = 'emr_patient_token';
const SESSION_KEY = 'emr_patient_session';
const LEGACY_TOKEN_KEY = 'token';

/**
 * 读取当前患者端登录 Token。
 * 先查患者端自己的键，再兼容旧版统一 `token` 键，保证已有本地登录态还能恢复。
 */
export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY) || '';
}

/**
 * 读取患者会话快照。
 * 这里只恢复页面展示所需字段，实际鉴权能力仍要通过后端校验 Token 决定。
 */
export function readStoredSession() {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

/**
 * 保存患者登录态。
 * 使用患者端独立键保存，避免与后台端共享同一个本地 Token 槽位。
 */
export function saveAuthState(token, session) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  }
  if (session) {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }
  localStorage.removeItem(LEGACY_TOKEN_KEY);
}

/**
 * 清理患者端登录态。
 * 登录过期、主动退出或恢复失败时统一清空，保证页面不会保留“看起来像已登录”的旧快照。
 */
export function clearAuthState() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(LEGACY_TOKEN_KEY);
}
