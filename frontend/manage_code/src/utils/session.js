export const AUTH_EVENT_NAME = 'emr-manage-auth-event';

const TOKEN_KEY = 'emr_manage_token';
const SESSION_KEY = 'emr_manage_session';
const LEGACY_TOKEN_KEY = 'token';

/**
 * 读取当前后台登录 Token。
 * 优先使用新的后台独立存储键，同时兼容旧版统一 `token` 键，便于老本地数据平滑迁移。
 */
export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY) || '';
}

/**
 * 读取后台会话快照。
 * 快照仅用于页面刷新后的展示恢复，真实可用性仍以后端 Token 校验结果为准。
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
 * 保存后台登录态。
 * 这里同时写入 Token 和会话快照，并移除旧版通用 Token 键，避免患者端与后台端互相覆盖。
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
 * 清理后台登录态。
 * 401 失效、主动退出或本地恢复失败时统一走这里，保证各存储键不会残留脏状态。
 */
export function clearAuthState() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(LEGACY_TOKEN_KEY);
}
