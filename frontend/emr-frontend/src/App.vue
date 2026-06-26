<template>
  <div v-if="!hasPatientToken" class="auth-page patient-auth-page">
    <section class="auth-card">
      <div class="auth-brand">
        <span class="brand-icon">✚</span>
        <h1>安心医疗</h1>
        <p>请登录您的账号，开启健康管理之旅</p>
      </div>

      <el-tabs v-model="patientAuthMode" class="auth-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form label-position="top" :model="loginForm">
            <el-form-item label="账号">
              <el-input v-model="loginForm.username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password />
            </el-form-item>
            <el-button type="primary" class="auth-submit" @click="submitLogin">登录</el-button>
            <button class="auth-switch" type="button" @click="patientAuthMode = 'register'">立即注册</button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form label-position="top" :model="registerForm">
            <el-form-item label="账号">
              <el-input v-model="registerForm.username" />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="registerForm.name" />
            </el-form-item>
            <el-form-item label="性别">
              <el-select v-model="registerForm.gender">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="registerForm.phone" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" show-password />
            </el-form-item>
            <el-button type="primary" class="auth-submit" @click="submitRegister">注册</el-button>
            <button class="auth-switch" type="button" @click="patientAuthMode = 'login'">返回登录</button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>

  <div v-else class="app-layout">
    <header class="app-header">
      <div class="brand">
        <span class="brand-icon">✚</span>
        <strong>安心医疗</strong>
      </div>
      <nav class="nav-links">
        <router-link to="/home" active-class="active">首页</router-link>
        <router-link to="/doctors" active-class="active">医生团队</router-link>
        <router-link to="/appointment" active-class="active">预约挂号</router-link>
        <router-link to="/news" active-class="active">健康资讯</router-link>
        <router-link to="/messages" active-class="active">留言咨询</router-link>
      </nav>
      <div class="user-area">
        <span class="balance">余额 ¥{{ patientBalance }}</span>
        <el-dropdown trigger="click">
          <span class="avatar-btn">{{ avatarText }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="logoutPatientAction">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <div class="app-body">
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <span v-if="!sidebarCollapsed">◀ 收起</span>
          <span v-else>▶</span>
        </div>
        <el-menu
          :default-active="$route.path"
          :collapse="sidebarCollapsed"
          router
          background-color="transparent"
          text-color="#53656a"
          active-text-color="#3f8067"
        >
          <el-menu-item index="/appointment">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><rect x="3" y="6" width="18" height="15" rx="2" fill="none" stroke="currentColor" stroke-width="2"/><line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="2"/><line x1="8" y1="3" x2="8" y2="7" stroke="currentColor" stroke-width="2"/><line x1="16" y1="3" x2="16" y2="7" stroke="currentColor" stroke-width="2"/></svg></el-icon>
            <span>我的预约</span>
          </el-menu-item>
          <el-menu-item index="/records">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" fill="none" stroke="currentColor" stroke-width="2"/><polyline points="14 2 14 8 20 8" fill="none" stroke="currentColor" stroke-width="2"/></svg></el-icon>
            <span>我的病历</span>
          </el-menu-item>
          <el-menu-item index="/fees">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><rect x="2" y="4" width="20" height="16" rx="2" fill="none" stroke="currentColor" stroke-width="2"/><line x1="12" y1="8" x2="12" y2="16" stroke="currentColor" stroke-width="2"/><line x1="8" y1="12" x2="16" y2="12" stroke="currentColor" stroke-width="2"/></svg></el-icon>
            <span>我的缴费</span>
          </el-menu-item>
          <el-menu-item index="/prescriptions">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><rect x="3" y="5" width="18" height="14" rx="2" fill="none" stroke="currentColor" stroke-width="2"/><line x1="8" y1="3" x2="8" y2="7" stroke="currentColor" stroke-width="2"/><line x1="16" y1="3" x2="16" y2="7" stroke="currentColor" stroke-width="2"/><line x1="7" y1="11" x2="17" y2="11" stroke="currentColor" stroke-width="2"/><line x1="7" y1="14" x2="14" y2="14" stroke="currentColor" stroke-width="2"/></svg></el-icon>
            <span>我的处方</span>
          </el-menu-item>
          <el-menu-item index="/tests">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2"/><line x1="12" y1="7" x2="12" y2="12" stroke="currentColor" stroke-width="2"/><line x1="12" y1="12" x2="15" y2="14" stroke="currentColor" stroke-width="2"/></svg></el-icon>
            <span>我的检查</span>
          </el-menu-item>
        </el-menu>
      </aside>
      <main class="app-main" :class="{ expanded: sidebarCollapsed }">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, provide, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { login, logout, registerPatient, validateToken, changePassword } from './api/auth';
import { unwrap, showError } from './utils/common';
import request from './utils/request';
import { AUTH_EVENT_NAME, clearAuthState, getStoredToken, readStoredSession, saveAuthState } from './utils/session';

const patientAuthMode = ref('login');
const session = ref(createEmptyPatientSession());
const sidebarCollapsed = ref(false);
const hasPatientToken = ref(Boolean(getStoredToken()));
const patientBalance = ref('0.00');
const patientSessionReady = ref(!hasPatientToken.value);

const loginForm = ref({ username: 'patient_demo', password: '123456', roleCode: 'patient' });
const registerForm = ref({ username: 'patient_new', password: '123456', name: '新患者', gender: '女', phone: '13800000000' });

const avatarText = computed(() => String(session.value.name || session.value.username || '患').slice(0, 1));

function createEmptyPatientSession() {
  return {
    userId: null,
    username: 'patient_guest',
    name: '未登录患者',
    roleCode: 'guest'
  };
}

function normalizePatientSession(data, fallback = {}) {
  return {
    userId: data?.userId ?? data?.patientId ?? fallback.userId ?? null,
    username: data?.username || fallback.username || 'patient_guest',
    name: data?.name || fallback.name || data?.username || '未登录患者',
    roleCode: data?.roleCode || fallback.roleCode || 'patient'
  };
}

function applyPatientSession(nextSession) {
  session.value = nextSession;
}

function persistPatientSession(data) {
  const sessionSnapshot = normalizePatientSession(data, readStoredSession() || {});
  saveAuthState(data?.token || getStoredToken(), sessionSnapshot);
  hasPatientToken.value = Boolean(getStoredToken());
  applyPatientSession(sessionSnapshot);
}

function restorePatientSession() {
  const storedSession = readStoredSession();
  if (storedSession) {
    applyPatientSession(normalizePatientSession(storedSession, storedSession));
  }
  hasPatientToken.value = Boolean(getStoredToken());
}

async function verifyPatientSession() {
  if (!getStoredToken()) {
    return false;
  }
  try {
    const data = unwrap(await validateToken());
    persistPatientSession(data);
    return true;
  } catch (error) {
    return false;
  }
}

function resetPatientAuth() {
  clearAuthState();
  hasPatientToken.value = false;
  patientSessionReady.value = true;
  applyPatientSession(createEmptyPatientSession());
  patientAuthMode.value = 'login';
}

async function submitLogin() {
  try {
    const data = unwrap(await login(loginForm.value));
    persistPatientSession(data);
    await loadPatientBalance();
    ElMessage.success('登录成功');
  } catch (error) {
    showError(error);
  }
}

async function submitRegister() {
  try {
    unwrap(await registerPatient(registerForm.value));
    loginForm.value.username = registerForm.value.username;
    loginForm.value.password = registerForm.value.password;
    loginForm.value.roleCode = 'patient';
    patientAuthMode.value = 'login';
    ElMessage.success('注册成功，请继续登录');
  } catch (error) {
    showError(error);
  }
}

async function logoutPatientAction() {
  try {
    if (getStoredToken()) {
      await logout();
    }
  } catch (error) {
  }
  resetPatientAuth();
  ElMessage.success('已退出登录');
}

async function loadPatientBalance() {
  if (!session.value.userId) {
    patientBalance.value = '0.00';
    return;
  }
  try {
    const data = unwrap(await request.get(`/user-management/patients/${session.value.userId}/balance`));
    patientBalance.value = Number(data).toFixed(2);
  } catch {
    patientBalance.value = '0.00';
  }
}

async function submitRecharge(amount) {
  try {
    const data = unwrap(await request.post(`/user-management/patients/${session.value.userId}/recharge`, { amount }));
    patientBalance.value = Number(data).toFixed(2);
    return true;
  } catch (error) {
    showError(error);
    return false;
  }
}

async function submitPasswordChange(passwordData) {
  try {
    unwrap(await changePassword(passwordData));
    return true;
  } catch (error) {
    showError(error);
    return false;
  }
}

function handlePatientAuthEvent(event) {
  const status = event?.detail?.status;
  const clearState = event?.detail?.clearState;
  if (status === 401 && clearState !== false) {
    resetPatientAuth();
    ElMessage.warning(event?.detail?.message || '登录状态已过期，请重新登录');
    return;
  }
  if (status === 401 && clearState === false) {
    return;
  }
  if (status === 403) {
    ElMessage.warning(event?.detail?.message || '当前账号无权执行该操作');
  }
}

provide('session', session);
provide('patientBalance', patientBalance);
provide('loadPatientBalance', loadPatientBalance);
provide('submitRecharge', submitRecharge);
provide('submitPasswordChange', submitPasswordChange);

onMounted(async () => {
  window.addEventListener(AUTH_EVENT_NAME, handlePatientAuthEvent);
  restorePatientSession();
  const valid = await verifyPatientSession();
  patientSessionReady.value = true;
  if (valid) {
    await loadPatientBalance();
  }
});

onBeforeUnmount(() => {
  window.removeEventListener(AUTH_EVENT_NAME, handlePatientAuthEvent);
});
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f7f6ef;
  color: #2f3f47;
}
</style>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 22% 34%, rgba(174, 218, 183, 0.24), transparent 18%),
    radial-gradient(circle at 78% 24%, rgba(167, 217, 200, 0.28), transparent 16%),
    radial-gradient(circle at 68% 72%, rgba(202, 230, 190, 0.22), transparent 15%),
    #f7f7ef;
}

.auth-card {
  width: min(360px, 100%);
  padding: 34px 30px 32px;
  background: #ffffff;
  border: 1px solid rgba(218, 231, 219, 0.85);
  border-radius: 14px;
  box-shadow: 0 18px 45px rgba(67, 92, 75, 0.13);
}

.auth-brand {
  display: grid;
  justify-items: center;
  gap: 8px;
  margin-bottom: 22px;
  text-align: center;
}

.brand-icon {
  display: inline-grid;
  width: 28px;
  height: 28px;
  place-items: center;
  color: #62ad77;
  border: 1px solid #9ac7a5;
  border-radius: 6px;
  font-weight: 700;
}

.auth-brand h1 {
  margin: 0;
  color: #3c7965;
  font-size: 25px;
}

.auth-brand p {
  margin: 0;
  color: #7c8b88;
  font-size: 13px;
}

.auth-tabs :deep(.el-tabs__header) {
  display: none;
}

.auth-switch {
  display: block;
  margin: 14px auto 0;
  color: #5b9276;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 13px;
}

.auth-submit {
  width: 100%;
  min-height: 38px;
  background: #64ad70;
  border-color: #64ad70;
}

.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f6ef;
}

.app-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 32px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e8efe6;
  box-shadow: 0 2px 12px rgba(28, 57, 44, 0.06);
  backdrop-filter: blur(10px);
}

.app-header .brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #3f8067;
  font-size: 20px;
  font-weight: 700;
  white-space: nowrap;
}

.nav-links {
  display: flex;
  gap: 4px;
}

.nav-links a {
  display: inline-flex;
  align-items: center;
  height: 64px;
  padding: 0 18px;
  color: #53656a;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  border-bottom: 3px solid transparent;
  transition: all 0.2s;
}

.nav-links a:hover,
.nav-links a.active {
  color: #4fa66b;
  background: #eef8f0;
  border-bottom-color: #63b878;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 16px;
}

.balance {
  display: inline-block;
  padding: 4px 14px;
  color: #0b74b8;
  background: #dbeafe;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
}

.avatar-btn {
  display: inline-grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: #ffffff;
  background: #8bcf9a;
  border-radius: 50%;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.avatar-btn:hover {
  background: #6ab87d;
}

.sidebar-toggle {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px 8px;
  color: #8a9e95;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
  transition: color 0.2s;
}

.sidebar-toggle:hover {
  color: #3f8067;
}

.sidebar {
  background: #ffffff;
  border-right: 1px solid #e8efe6;
  box-shadow: 2px 0 10px rgba(28, 57, 44, 0.04);
  transition: width 0.25s ease;
  width: 220px;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar :deep(.el-menu) {
  border-right: none;
}

.sidebar :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  border-radius: 6px;
  margin: 2px 8px;
  transition: all 0.2s;
}

.sidebar :deep(.el-menu-item:hover) {
  background: #eef8f0;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: #e2f3e5;
  color: #3f8067;
  font-weight: 600;
}

.app-main {
  flex: 1;
  overflow-y: auto;
  padding: 28px 32px 40px;
  transition: padding 0.25s ease;
}

.app-main.expanded {
  padding: 28px 32px 40px;
}

:deep(.el-button--primary:not(.is-link)) {
  --el-button-bg-color: #2f9e58;
  --el-button-border-color: #2f9e58;
  background-color: #2f9e58;
  border-color: #2f9e58;
  color: #ffffff;
  font-weight: 600;
}

:deep(.el-button--primary:not(.is-link):hover) {
  background-color: #267e47;
  border-color: #267e47;
  color: #ffffff;
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: #63b878;
}

@media (max-width: 900px) {
  .app-header {
    flex-wrap: wrap;
    height: auto;
    padding: 12px 16px;
    gap: 8px;
  }

  .nav-links {
    order: 3;
    width: 100%;
    overflow-x: auto;
  }

  .nav-links a {
    height: 44px;
    white-space: nowrap;
  }

  .app-main {
    padding: 16px;
  }
}
</style>
