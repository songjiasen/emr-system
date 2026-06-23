<template>
  <main v-if="!hasPatientToken" class="auth-page patient-auth-page">
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
  </main>

  <main v-else class="patient-workspace">
    <header class="topbar">
      <div class="brand">
        <span class="brand-icon">✚</span>
        <strong>安心医疗</strong>
      </div>
      <nav class="patient-nav" aria-label="患者端功能导航">
        <template v-for="group in patientNavGroups" :key="group.label">
          <button
            v-if="!group.children"
            :class="{ active: activeTab === group.name }"
            type="button"
            @click="goToPatientTab(group.name)"
          >
            {{ group.label }}
          </button>
          <el-dropdown v-else trigger="click" @command="goToPatientTab">
            <button
              class="nav-parent"
              :class="{ active: isPatientGroupActive(group) }"
              type="button"
            >
              {{ group.label }}
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="child in group.children"
                  :key="child.name"
                  :command="child.name"
                >
                  {{ child.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </nav>
      <div class="session">
        <span class="avatar">{{ patientAvatarText }}</span>
        <span>{{ session.name }}</span>
        <el-tag type="success">{{ session.roleCode }}</el-tag>
        <el-button v-if="hasPatientToken" link type="primary" @click="logoutPatientAction">退出</el-button>
      </div>
    </header>

    <section class="page-hero">
      <h1>{{ patientPageTitle }}</h1>
      <p>{{ patientPageSubtitle }}</p>
    </section>

    <el-tabs v-model="activeTab" class="workspace-tabs" @tab-change="goToPatientTab">
      <el-tab-pane label="首页" name="home">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>首页轮播</h2>
              <el-button @click="loadCarousels">刷新</el-button>
            </div>
            <el-table :data="carousels" height="360">
              <el-table-column prop="title" label="轮播标题" min-width="180" />
              <el-table-column prop="imageUrl" label="图片地址" min-width="220" />
              <el-table-column prop="linkUrl" label="跳转地址" min-width="180" />
            </el-table>
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pagers.carousels.limit"
              :current-page="pagers.carousels.page"
              :total="pagers.carousels.total"
              @current-change="(page) => changePage('carousels', page, loadCarousels)"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="医生介绍" name="doctors">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>医生介绍</h2>
              <el-button @click="loadDoctors">刷新</el-button>
            </div>
            <el-table :data="doctors" height="360">
              <el-table-column prop="name" label="医生" min-width="110" />
              <el-table-column prop="departmentName" label="科室" min-width="110" />
              <el-table-column prop="specialty" label="擅长" min-width="150" />
            </el-table>
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pagers.doctors.limit"
              :current-page="pagers.doctors.page"
              :total="pagers.doctors.total"
              @current-change="(page) => changePage('doctors', page, loadDoctors)"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="预约挂号" name="appointment">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>我的预约</h2>
              <div class="button-row">
                <el-button type="primary" @click="patientDialogs.appointment = true">新增预约</el-button>
                <el-button @click="loadAppointments">刷新列表</el-button>
              </div>
            </div>
            <el-table :data="appointments" height="360">
              <el-table-column prop="appointmentNo" label="预约号" min-width="130" />
              <el-table-column prop="doctorName" label="医生" min-width="90" />
              <el-table-column prop="appointmentTime" label="时间" min-width="150" />
              <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button link type="danger" @click="cancelAppointmentAction(row)">取消</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pagers.appointments.limit"
              :current-page="pagers.appointments.page"
              :total="pagers.appointments.total"
              @current-change="(page) => changePage('appointments', page, loadAppointments)"
            />
          </div>

          <el-dialog v-model="patientDialogs.appointment" title="新增预约" width="520px">
            <el-form class="dialog-form" label-position="top" :model="appointmentForm">
              <el-form-item label="医生">
                <el-select v-model="appointmentForm.doctorId" @change="syncDoctor">
                  <el-option
                    v-for="doctor in doctors"
                    :key="doctor.id"
                    :label="doctorOptionLabel(doctor)"
                    :value="doctor.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="预约时间">
                <el-input v-model="appointmentForm.appointmentTime" placeholder="2026-06-23 09:00:00" />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="appointmentForm.remark" type="textarea" :rows="3" />
              </el-form-item>
              <div class="dialog-footer">
                <el-button @click="patientDialogs.appointment = false">取消</el-button>
                <el-button type="primary" @click="submitAppointment">提交预约</el-button>
              </div>
            </el-form>
          </el-dialog>
        </section>
      </el-tab-pane>

      <el-tab-pane label="就诊记录" name="record">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>病历记录</h2>
              <el-button @click="loadRecords">刷新</el-button>
            </div>
            <el-table :data="records" height="220" @row-click="selectRecordDetail">
              <el-table-column prop="recordNo" label="病历号" min-width="130" />
              <el-table-column prop="doctorName" label="医生" min-width="90" />
              <el-table-column prop="diagnosis" label="诊断" min-width="140" />
              <el-table-column prop="archiveStatus" label="归档状态" width="120" :formatter="statusFormatter" />
            </el-table>
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pagers.records.limit"
              :current-page="pagers.records.page"
              :total="pagers.records.total"
              @current-change="(page) => changePage('records', page, loadRecords)"
            />
            <el-descriptions v-if="selectedRecordDetail" :column="1" border class="detail-box">
              <el-descriptions-item label="主诉">{{ selectedRecordDetail.chiefComplaint || '-' }}</el-descriptions-item>
              <el-descriptions-item label="现病史">{{ selectedRecordDetail.presentIllness || '-' }}</el-descriptions-item>
              <el-descriptions-item label="治疗建议">{{ selectedRecordDetail.treatmentAdvice || '-' }}</el-descriptions-item>
              <el-descriptions-item label="附件">
                <a v-if="selectedRecordDetail.fileUrl" :href="selectedRecordDetail.fileUrl" target="_blank" rel="noreferrer">查看附件</a>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="费用支付" name="fees">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>费用支付</h2>
              <el-button @click="loadFees">刷新</el-button>
            </div>
            <el-table :data="fees" height="360">
              <el-table-column prop="feeNo" label="费用号" min-width="130" />
              <el-table-column prop="patientName" label="患者" min-width="90" />
              <el-table-column prop="amount" label="金额" width="100" />
              <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button link type="primary" @click="payFeeAction(row)">支付</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pagers.fees.limit"
              :current-page="pagers.fees.page"
              :total="pagers.fees.total"
              @current-change="(page) => changePage('fees', page, loadFees)"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="处方记录" name="prescriptions">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>处方记录</h2>
              <el-button @click="loadPrescriptions">刷新</el-button>
            </div>
            <el-table :data="prescriptions" height="360">
              <el-table-column prop="medicineName" label="药品" min-width="140" />
              <el-table-column prop="quantity" label="数量" width="100" />
              <el-table-column prop="doctorName" label="医生" width="100" />
              <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
            </el-table>
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pagers.prescriptions.limit"
              :current-page="pagers.prescriptions.page"
              :total="pagers.prescriptions.total"
              @current-change="(page) => changePage('prescriptions', page, loadPrescriptions)"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="检查申请" name="tests">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>检查申请</h2>
              <el-button @click="loadTestRequests">刷新</el-button>
            </div>
            <el-table :data="testRequests" height="360">
              <el-table-column prop="testItem" label="项目" min-width="140" />
              <el-table-column prop="doctorName" label="医生" width="100" />
              <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
              <el-table-column prop="auditOpinion" label="审核意见" min-width="140" />
            </el-table>
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pagers.testRequests.limit"
              :current-page="pagers.testRequests.page"
              :total="pagers.testRequests.total"
              @current-change="(page) => changePage('testRequests', page, loadTestRequests)"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="健康资讯" name="news">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>健康资讯</h2>
              <el-button @click="loadNews">刷新</el-button>
            </div>
            <el-table :data="newsList" height="220" @row-click="selectNews">
              <el-table-column prop="title" label="标题" min-width="180" />
              <el-table-column prop="category" label="分类" width="110" />
              <el-table-column prop="publishStatus" label="状态" width="100" :formatter="statusFormatter" />
            </el-table>
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pagers.news.limit"
              :current-page="pagers.news.page"
              :total="pagers.news.total"
              @current-change="(page) => changePage('news', page, loadNews)"
            />
            <el-descriptions v-if="selectedNews" :column="1" border class="detail-box">
              <el-descriptions-item label="标题">{{ selectedNews.title }}</el-descriptions-item>
              <el-descriptions-item label="摘要">{{ selectedNews.summary || '-' }}</el-descriptions-item>
              <el-descriptions-item label="内容">{{ selectedNews.content || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="留言咨询" name="messages">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>留言咨询</h2>
              <div class="button-row">
                <el-button type="primary" @click="patientDialogs.message = true">新增留言</el-button>
                <el-button @click="loadMessages">刷新回复</el-button>
              </div>
            </div>
            <el-table :data="messages" height="190">
              <el-table-column prop="title" label="标题" min-width="130" />
              <el-table-column prop="replyContent" label="回复" min-width="150" />
              <el-table-column prop="status" label="状态" width="90" :formatter="statusFormatter" />
            </el-table>
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pagers.messages.limit"
              :current-page="pagers.messages.page"
              :total="pagers.messages.total"
              @current-change="(page) => changePage('messages', page, loadMessages)"
            />
          </div>

          <el-dialog v-model="patientDialogs.message" title="新增留言" width="520px">
            <el-form class="dialog-form" label-position="top" :model="messageForm">
              <el-form-item label="标题">
                <el-input v-model="messageForm.title" />
              </el-form-item>
              <el-form-item label="内容">
                <el-input v-model="messageForm.content" type="textarea" :rows="4" />
              </el-form-item>
              <div class="dialog-footer">
                <el-button @click="patientDialogs.message = false">取消</el-button>
                <el-button type="primary" @click="submitMessage">提交留言</el-button>
              </div>
            </el-form>
          </el-dialog>
        </section>
      </el-tab-pane>

      <el-tab-pane label="个人中心" name="profile">
        <section class="single-column">
          <div class="panel">
            <div class="panel-head">
              <h2>当前会话</h2>
              <div class="button-row">
                <el-button type="primary" @click="patientDialogs.password = true">修改密码</el-button>
                <el-button @click="logoutPatientAction">退出登录</el-button>
              </div>
            </div>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="用户ID">{{ session.userId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="账号">{{ session.username }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ session.name }}</el-descriptions-item>
              <el-descriptions-item label="角色">{{ session.roleCode }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <el-dialog v-model="patientDialogs.password" title="修改密码" width="480px">
            <el-form class="dialog-form" label-position="top" :model="passwordForm">
              <el-form-item label="旧密码">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="passwordForm.newPassword" type="password" show-password />
              </el-form-item>
              <div class="dialog-footer">
                <el-button @click="patientDialogs.password = false">取消</el-button>
                <el-button type="primary" @click="submitPasswordChange">保存新密码</el-button>
              </div>
            </el-form>
          </el-dialog>
        </section>
      </el-tab-pane>

      <el-tab-pane label="智能检索" name="ai">
        <section class="two-column">
          <div class="panel">
            <h2>智能检索</h2>
            <el-form label-position="top" :model="aiForm">
              <el-form-item label="关键词">
                <el-input v-model="aiForm.keyword" />
              </el-form-item>
              <el-form-item label="诊断">
                <el-input v-model="aiForm.diagnosis" />
              </el-form-item>
              <div class="button-row">
                <el-button type="primary" @click="runAiSearch">检索</el-button>
                <el-button @click="runMedicineRecommend">荐药</el-button>
              </div>
            </el-form>
          </div>

          <div class="panel">
            <h2>智能结果</h2>
            <pre class="result-box">{{ aiResult }}</pre>
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <router-view />
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { changePassword, login, logout, registerPatient, validateToken } from './api/auth';
import { fetchDoctors } from './api/doctor';
import { createAppointment, fetchAppointments, cancelAppointment } from './api/appointment';
import { fetchMedicalRecords, fetchMedicalRecordDetail } from './api/medicalRecord';
import { fetchFees, payFee } from './api/billing';
import { fetchPrescriptions, fetchTestRequests } from './api/clinical';
import { createMessage, fetchCarousels, fetchMessages, fetchNews } from './api/system';
import { recommendMedicine, smartSearch } from './api/ai';
import { AUTH_EVENT_NAME, clearAuthState, getStoredToken, readStoredSession, saveAuthState } from './utils/session';

const route = useRoute();
const router = useRouter();
const activeTab = ref('home');
const patientAuthMode = ref('login');
const session = ref(createEmptyPatientSession());
const hasPatientToken = ref(Boolean(getStoredToken()));
const patientSessionReady = ref(!hasPatientToken.value);
const doctors = ref([]);
const appointments = ref([]);
const records = ref([]);
const selectedRecordDetail = ref(null);
const fees = ref([]);
const prescriptions = ref([]);
const testRequests = ref([]);
const newsList = ref([]);
const carousels = ref([]);
const selectedNews = ref(null);
const messages = ref([]);
const aiResult = ref('等待检索');
const passwordForm = ref({ oldPassword: '123456', newPassword: '12345678' });
const pagers = reactive({
  doctors: { page: 1, limit: 10, total: 0 },
  appointments: { page: 1, limit: 10, total: 0 },
  records: { page: 1, limit: 10, total: 0 },
  fees: { page: 1, limit: 10, total: 0 },
  prescriptions: { page: 1, limit: 10, total: 0 },
  testRequests: { page: 1, limit: 10, total: 0 },
  news: { page: 1, limit: 10, total: 0 },
  carousels: { page: 1, limit: 10, total: 0 },
  messages: { page: 1, limit: 10, total: 0 }
});

const loginForm = ref({ username: 'patient_demo', password: '123456', roleCode: 'patient' });
const registerForm = ref({ username: 'patient_new', password: '123456', name: '新患者', gender: '女', phone: '13800000000' });
const appointmentForm = ref({
  patientId: 1,
  patientName: '患者演示',
  doctorId: 1,
  doctorName: '王医生',
  departmentId: 1,
  departmentName: '心内科',
  appointmentTime: '2026-06-23 09:00:00',
  remark: '复诊咨询'
});
const messageForm = ref({
  userId: 1,
  userName: '患者演示',
  title: '用药咨询',
  content: '请问处方药需要饭后服用吗？'
});
const aiForm = ref({ keyword: '高血压 随访', diagnosis: '高血压' });
const patientDialogs = reactive({
  appointment: false,
  message: false,
  password: false
});
const patientNavGroups = [
  { name: 'home', label: '首页', path: '/home' },
  {
    label: '医疗服务',
    children: [
      { name: 'doctors', label: '医生介绍', path: '/doctors' },
      { name: 'appointment', label: '预约挂号', path: '/appointment' }
    ]
  },
  {
    label: '就诊账单',
    children: [
      { name: 'record', label: '就诊记录', path: '/record' },
      { name: 'fees', label: '费用支付', path: '/fees' }
    ]
  },
  {
    label: '处方检查',
    children: [
      { name: 'prescriptions', label: '处方记录', path: '/prescriptions' },
      { name: 'tests', label: '检查申请', path: '/tests' }
    ]
  },
  {
    label: '健康互动',
    children: [
      { name: 'news', label: '健康资讯', path: '/news' },
      { name: 'messages', label: '留言咨询', path: '/messages' }
    ]
  },
  { name: 'profile', label: '个人中心', path: '/profile' },
  { name: 'ai', label: '智能检索', path: '/ai' }
];
const patientNavItems = patientNavGroups.flatMap((group) => group.children || [group]);

/**
 * 创建患者端游客态。
 * 没有有效 Token 时页面只保留公共浏览能力，避免把本地表单默认值误当成真实登录身份。
 */
function createEmptyPatientSession() {
  return {
    userId: null,
    username: 'patient_guest',
    name: '未登录患者',
    roleCode: 'guest'
  };
}

const currentPatient = computed(() => ({
  patientId: session.value.userId || 1,
  patientName: session.value.name || session.value.username || '患者演示'
}));

const patientPageMeta = {
  home: ['首页', '欢迎来到安心医疗，随时查看院内服务公告'],
  doctors: ['医生介绍', '了解医生科室、擅长方向与就诊服务'],
  appointment: ['预约挂号', '健康从这里开始，我们随时为您提供专业服务'],
  record: ['就诊记录', '在这里查看您的历史就诊记录，帮助您更好了解自己的健康状况'],
  fees: ['费用支付', '查看您的费用记录并处理待支付账单'],
  prescriptions: ['处方记录', '查看医生为您开具的处方记录'],
  tests: ['检查申请', '查看检查申请、审核意见与检查结果'],
  news: ['健康资讯', '了解最新健康知识与院内公告'],
  messages: ['留言咨询', '向医护人员提交咨询并查看回复'],
  profile: ['个人中心', '管理您的账号信息和登录安全'],
  ai: ['智能检索', '通过智能工具快速检索病历摘要与用药建议']
};

const patientPageTitle = computed(() => patientPageMeta[activeTab.value]?.[0] || '安心医疗');
const patientPageSubtitle = computed(() => patientPageMeta[activeTab.value]?.[1] || '为您提供贴心医疗服务');
const patientAvatarText = computed(() => String(session.value.name || session.value.username || '患').slice(0, 1));

function resolvePatientTab(tabName) {
  return patientNavItems.some((item) => item.name === tabName) ? tabName : 'home';
}

function findPatientNavItem(tabName) {
  const safeTab = resolvePatientTab(tabName);
  return patientNavItems.find((item) => item.name === safeTab) || patientNavItems[0];
}

function isPatientGroupActive(group) {
  return Boolean(group.children?.some((item) => item.name === activeTab.value));
}

function goToPatientTab(tabName) {
  const item = findPatientNavItem(tabName);
  if (!item || route.name === item.name) {
    return;
  }
  router.push({ name: item.name });
}

function unwrap(response) {
  const body = response?.data;
  if (!body || body.code !== 0) {
    throw new Error(body?.message || '接口返回异常');
  }
  return body.data;
}

function rowsOf(pageData) {
  return Array.isArray(pageData?.rows) ? pageData.rows : [];
}

function applyPageData(target, pagerKey, pageData) {
  target.value = rowsOf(pageData);
  pagers[pagerKey].total = Number(pageData?.total || 0);
  pagers[pagerKey].page = Number(pageData?.page || pagers[pagerKey].page);
  pagers[pagerKey].limit = Number(pageData?.limit || pagers[pagerKey].limit);
}

function changePage(pagerKey, page, loader) {
  pagers[pagerKey].page = page;
  loader();
}

const STATUS_TEXT = {
  pending: '待处理',
  pending_audit: '待审核',
  approved: '已通过',
  rejected: '已驳回',
  executed: '已执行',
  not_submitted: '未提交',
  archived: '已归档',
  cancelled: '已取消',
  in_hospital: '住院中',
  discharged: '已出院',
  unpaid: '未支付',
  paid: '已支付',
  published: '已发布'
};

function statusText(value) {
  return STATUS_TEXT[value] || value || '-';
}

function statusFormatter(_row, _column, value) {
  return statusText(value);
}

function doctorOptionLabel(doctor) {
  const name = doctor?.name || doctor?.username || '未命名医生';
  const departmentName = doctor?.departmentName || '未分配科室';
  return `${name} - ${departmentName}`;
}

function showError(error) {
  const status = error?.response?.status;
  if (status === 401 || status === 403) {
    return;
  }
  ElMessage.error(error?.response?.data?.message || error?.message || '操作失败');
}

/**
 * 登录后刷新患者上下文，后续预约、留言等表单使用同一份患者快照。
 */
async function submitLogin() {
  try {
    const data = unwrap(await login(loginForm.value));
    persistPatientSession(data);
    loadPatientRouteData(activeTab.value);
    ElMessage.success('登录成功');
  } catch (error) {
    showError(error);
  }
}

/**
 * 注册成功后回填登录表单。
 * 注册接口不会直接签发 Token，因此这里不再伪造已登录状态，而是引导用户马上完成一次真实登录。
 */
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

/**
 * 规范化患者会话数据。
 * 登录接口和 Token 校验接口返回字段口径略有差异，这里统一成页面使用的一套会话结构。
 */
function normalizePatientSession(data, fallback = {}) {
  return {
    userId: data?.userId ?? data?.patientId ?? fallback.userId ?? null,
    username: data?.username || fallback.username || 'patient_guest',
    name: data?.name || fallback.name || data?.username || '未登录患者',
    roleCode: data?.roleCode || fallback.roleCode || 'patient'
  };
}

/**
 * 把患者会话应用到页面。
 * 预约、留言等表单都依赖这份快照，因此会话变化后要同步刷新相关默认值。
 */
function applyPatientSession(nextSession) {
  session.value = nextSession;
  appointmentForm.value.patientId = nextSession.userId || 1;
  appointmentForm.value.patientName = nextSession.name || nextSession.username || '患者演示';
  messageForm.value.userId = nextSession.userId || 1;
  messageForm.value.userName = nextSession.name || nextSession.username || '患者演示';
}

/**
 * 保存患者登录态。
 * 登录成功和 Token 校验成功都复用这一入口，确保本地存储和页面显示始终同步。
 */
function persistPatientSession(data) {
  const sessionSnapshot = normalizePatientSession(data, readStoredSession() || {});
  saveAuthState(data?.token || getStoredToken(), sessionSnapshot);
  hasPatientToken.value = Boolean(getStoredToken());
  applyPatientSession(sessionSnapshot);
}

/**
 * 清理患者受保护数据。
 * 登录失效或退出后只清理需要鉴权的列表，公共医生/资讯数据继续保留，页面不会显得“全空”。
 */
function clearProtectedPatientData() {
  appointments.value = [];
  records.value = [];
  selectedRecordDetail.value = null;
  fees.value = [];
  prescriptions.value = [];
  testRequests.value = [];
  messages.value = [];
}

/**
 * 重置患者认证态。
 * 401 失效或主动退出时统一回到未登录状态，并把受保护数据从页面上撤掉。
 */
function resetPatientAuth({ resetTab = true } = {}) {
  clearAuthState();
  hasPatientToken.value = false;
  patientSessionReady.value = true;
  applyPatientSession(createEmptyPatientSession());
  clearProtectedPatientData();
  if (resetTab) {
    activeTab.value = 'home';
    patientAuthMode.value = 'login';
  }
}

/**
 * 恢复本地患者会话快照。
 * 仅恢复展示所需字段，后续仍会调用后端校验 Token 再决定是否加载受保护资源。
 */
function restorePatientSession() {
  const storedSession = readStoredSession();
  if (storedSession) {
    applyPatientSession(normalizePatientSession(storedSession, storedSession));
  }
  hasPatientToken.value = Boolean(getStoredToken());
}

/**
 * 校验患者 Token 是否仍然有效。
 * 校验通过后刷新本地快照；校验失败由统一认证事件负责清理会话。
 */
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

/**
 * 退出患者登录。
 * 即使后端退出接口失败，也要先释放本地会话，保证用户能重新登录。
 */
async function logoutPatientAction() {
  try {
    if (getStoredToken()) {
      await logout();
    }
  } catch (error) {
    // 退出失败时继续按本地退出处理，避免保留一份已经不可控的旧登录态。
  }
  resetPatientAuth();
  goToPatientTab('home');
  ElMessage.success('已退出登录');
}

async function loadDoctors() {
  try {
    applyPageData(doctors, 'doctors', unwrap(await fetchDoctors({ page: pagers.doctors.page, limit: pagers.doctors.limit })));
    syncDoctor(appointmentForm.value.doctorId);
  } catch (error) {
    showError(error);
  }
}

function syncDoctor(doctorId) {
  const doctor = doctors.value.find((item) => item.id === doctorId);
  if (!doctor) {
    return;
  }
  appointmentForm.value.doctorName = doctor.name || doctor.username || '未命名医生';
  appointmentForm.value.departmentId = doctor.departmentId;
  appointmentForm.value.departmentName = doctor.departmentName || '未分配科室';
}

function normalizeAppointmentTime(value) {
  const text = String(value || '').trim().replace('T', ' ');
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(text)) {
    return `${text}:00`;
  }
  return text;
}

function selectNews(row) {
  selectedNews.value = row || null;
}

/**
 * 创建预约时把患者和医生快照一起提交，满足后端演示版不跨服务补全的约束。
 */
async function submitAppointment() {
  try {
    Object.assign(appointmentForm.value, currentPatient.value);
    syncDoctor(appointmentForm.value.doctorId);
    const payload = {
      ...appointmentForm.value,
      appointmentTime: normalizeAppointmentTime(appointmentForm.value.appointmentTime)
    };
    unwrap(await createAppointment(payload));
    appointmentForm.value.appointmentTime = payload.appointmentTime;
    pagers.appointments.page = 1;
    await loadAppointments();
    patientDialogs.appointment = false;
    ElMessage.success('预约已提交');
  } catch (error) {
    showError(error);
  }
}

async function loadAppointments() {
  try {
    applyPageData(appointments, 'appointments', unwrap(await fetchAppointments({ patientId: currentPatient.value.patientId, page: pagers.appointments.page, limit: pagers.appointments.limit })));
  } catch (error) {
    showError(error);
  }
}

async function cancelAppointmentAction(row) {
  try {
    await cancelAppointment(row.id, { cancelReason: '患者主动取消' });
    await loadAppointments();
    ElMessage.success('预约已取消');
  } catch (error) {
    showError(error);
  }
}

async function loadRecords() {
  try {
    applyPageData(records, 'records', unwrap(await fetchMedicalRecords({ patientId: currentPatient.value.patientId, page: pagers.records.page, limit: pagers.records.limit })));
  } catch (error) {
    showError(error);
  }
}

/**
 * 查看病历详情。
 * 列表只展示摘要字段，点击后再取详情，保证患者看到的是包含病情描述和附件地址的完整病历。
 */
async function selectRecordDetail(row) {
  if (!row?.id) {
    selectedRecordDetail.value = null;
    return;
  }

  try {
    selectedRecordDetail.value = unwrap(await fetchMedicalRecordDetail(row.id));
  } catch (error) {
    showError(error);
  }
}

async function loadFees() {
  try {
    applyPageData(fees, 'fees', unwrap(await fetchFees({ patientId: currentPatient.value.patientId, page: pagers.fees.page, limit: pagers.fees.limit })));
  } catch (error) {
    showError(error);
  }
}

async function payFeeAction(row) {
  try {
    await payFee(row.id);
    await loadFees();
    ElMessage.success('支付成功');
  } catch (error) {
    showError(error);
  }
}

async function loadPrescriptions() {
  try {
    applyPageData(prescriptions, 'prescriptions', unwrap(await fetchPrescriptions({ patientId: currentPatient.value.patientId, page: pagers.prescriptions.page, limit: pagers.prescriptions.limit })));
  } catch (error) {
    showError(error);
  }
}

async function loadTestRequests() {
  try {
    applyPageData(testRequests, 'testRequests', unwrap(await fetchTestRequests({ patientId: currentPatient.value.patientId, page: pagers.testRequests.page, limit: pagers.testRequests.limit })));
  } catch (error) {
    showError(error);
  }
}

async function loadNews() {
  try {
    applyPageData(newsList, 'news', unwrap(await fetchNews({ page: pagers.news.page, limit: pagers.news.limit })));
    if (newsList.value.length > 0 && !selectedNews.value) {
      selectedNews.value = newsList.value[0];
    }
  } catch (error) {
    showError(error);
  }
}

async function loadCarousels() {
  try {
    applyPageData(carousels, 'carousels', unwrap(await fetchCarousels({ page: pagers.carousels.page, limit: pagers.carousels.limit })));
  } catch (error) {
    showError(error);
  }
}

/**
 * 留言提交保留患者身份快照，后台回复时可以直接看到咨询人。
 */
async function submitMessage() {
  try {
    Object.assign(messageForm.value, { userId: currentPatient.value.patientId, userName: currentPatient.value.patientName });
    await createMessage(messageForm.value);
    await loadMessages();
    patientDialogs.message = false;
    ElMessage.success('留言已提交');
  } catch (error) {
    showError(error);
  }
}

async function loadMessages() {
  try {
    applyPageData(messages, 'messages', unwrap(await fetchMessages({ page: pagers.messages.page, limit: pagers.messages.limit })));
  } catch (error) {
    showError(error);
  }
}

async function runAiSearch() {
  try {
    aiResult.value = JSON.stringify(unwrap(await smartSearch({ keyword: aiForm.value.keyword })), null, 2);
  } catch (error) {
    showError(error);
  }
}

async function runMedicineRecommend() {
  try {
    aiResult.value = JSON.stringify(unwrap(await recommendMedicine({ diagnosis: aiForm.value.diagnosis })), null, 2);
  } catch (error) {
    showError(error);
  }
}

/**
 * 修改患者密码。
 * 成功后保留当前会话，方便继续演示预约、病历和费用主链路。
 */
async function submitPasswordChange() {
  try {
    unwrap(await changePassword(passwordForm.value));
    patientDialogs.password = false;
    ElMessage.success('密码已修改');
  } catch (error) {
    showError(error);
  }
}

/**
 * 统一加载患者受保护数据。
 * 只有登录态通过校验后才触发，避免游客首次进入页面就连续收到多次 401。
 */
function loadProtectedPatientData() {
  loadAppointments();
  loadRecords();
  loadFees();
  loadPrescriptions();
  loadTestRequests();
  loadMessages();
}

/**
 * 根据当前路由加载患者端页面数据。
 * 菜单点击和刷新恢复都走这里，避免回到首页后再一次性请求所有业务接口。
 */
function loadPatientRouteData(tabName) {
  const tab = resolvePatientTab(tabName);
  if (!hasPatientToken.value && tab !== 'home') {
    return;
  }

  if (tab === 'home') {
    loadCarousels();
    return;
  }

  if (tab === 'doctors') {
    loadDoctors();
    return;
  }

  if (tab === 'appointment') {
    loadDoctors();
    loadAppointments();
    return;
  }

  if (tab === 'record') {
    loadRecords();
    return;
  }

  if (tab === 'fees') {
    loadFees();
    return;
  }

  if (tab === 'prescriptions') {
    loadPrescriptions();
    return;
  }

  if (tab === 'tests') {
    loadTestRequests();
    return;
  }

  if (tab === 'news') {
    loadNews();
    return;
  }

  if (tab === 'messages') {
    loadMessages();
  }
}

function syncPatientRouteState(routeName) {
  const nextTab = resolvePatientTab(routeName);
  if (activeTab.value !== nextTab) {
    activeTab.value = nextTab;
  }
  if (patientSessionReady.value) {
    loadPatientRouteData(nextTab);
  }
}

/**
 * 处理患者端统一认证事件。
 * 401 需要清理登录态并提示重新登录；403 只提醒当前权限不足，保留已有会话。
 */
function handlePatientAuthEvent(event) {
  const status = event?.detail?.status;
  if (status === 401) {
    resetPatientAuth();
    goToPatientTab('home');
    ElMessage.warning(event?.detail?.message || '登录状态已过期，请重新登录');
    return;
  }
  if (status === 403) {
    ElMessage.warning(event?.detail?.message || '当前账号无权执行该操作');
  }
}

onMounted(async () => {
  window.addEventListener(AUTH_EVENT_NAME, handlePatientAuthEvent);
  restorePatientSession();
  await verifyPatientSession();
  patientSessionReady.value = true;
  syncPatientRouteState(route.name);
});

watch(
  () => route.name,
  (routeName) => {
    syncPatientRouteState(routeName);
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  window.removeEventListener(AUTH_EVENT_NAME, handlePatientAuthEvent);
});
</script>

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
  color: #1f2937;
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
  letter-spacing: 0;
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

.patient-workspace {
  min-height: 100vh;
  padding: 0 0 28px;
  background: #f7f6ef;
  color: #2f3f47;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  min-height: 64px;
  padding: 0 max(24px, calc((100vw - 980px) / 2));
  margin-bottom: 34px;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid #edf1ed;
  box-shadow: 0 4px 14px rgba(28, 57, 44, 0.08);
  backdrop-filter: blur(12px);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #3f8067;
  font-size: 20px;
  font-weight: 700;
  white-space: nowrap;
}

.patient-nav {
  display: flex;
  align-items: stretch;
  min-width: 0;
  height: 64px;
  overflow-x: auto;
  scrollbar-width: none;
}

.patient-nav::-webkit-scrollbar {
  display: none;
}

.patient-nav .el-dropdown {
  display: flex;
}

.patient-nav button {
  min-width: 86px;
  padding: 0 14px;
  color: #53656a;
  background: transparent;
  border: 0;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  white-space: nowrap;
}

.patient-nav button.active,
.patient-nav button:hover {
  color: #4fa66b;
  background: #eef8f0;
  border-bottom-color: #63b878;
}

.avatar {
  display: inline-grid;
  width: 28px;
  height: 28px;
  place-items: center;
  color: #ffffff;
  background: #8bcf9a;
  border-radius: 50%;
  font-size: 13px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 14px;
}

h1,
h2 {
  margin: 0;
  letter-spacing: 0;
}

h1 {
  font-size: 28px;
}

h2 {
  margin-bottom: 16px;
  font-size: 18px;
}

.session,
.button-row,
.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.session {
  justify-content: flex-end;
  min-width: 220px;
  color: #536366;
}

.page-hero {
  width: min(880px, calc(100vw - 40px));
  min-height: 96px;
  display: grid;
  place-items: center;
  margin: 0 auto 20px;
  padding: 18px 28px;
  text-align: center;
  background: linear-gradient(100deg, #9cd7ec 0%, #bfe7b1 100%);
  border-radius: 9px;
  color: #2e4b56;
}

.page-hero h1 {
  font-size: 24px;
  font-weight: 700;
}

.page-hero p {
  margin: 8px 0 0;
  color: #567070;
  font-size: 13px;
}

.workspace-tabs {
  width: min(880px, calc(100vw - 40px));
  margin: 0 auto;
  padding: 0;
}

.workspace-tabs :deep(.el-tabs__header) {
  display: none;
}

.workspace-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #edf1ed;
}

.workspace-tabs :deep(.el-tabs__item) {
  min-width: 92px;
  height: 52px;
  color: #58686b;
}

.workspace-tabs :deep(.el-tabs__item.is-active) {
  color: #57a76e;
  background: #eef8f0;
}

.workspace-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  background: #64b77b;
}

.home-layout {
  display: grid;
  gap: 18px;
}

.single-column {
  display: grid;
  gap: 18px;
}

.two-column {
  display: grid;
  grid-template-columns: minmax(260px, 360px) minmax(380px, 1fr);
  gap: 18px;
  align-items: start;
}

.panel {
  min-width: 0;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #eef2ee;
  border-radius: 9px;
  box-shadow: 0 8px 22px rgba(94, 110, 95, 0.06);
}

.panel-head {
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-head h2 {
  margin-bottom: 0;
}

.detail-box {
  margin-top: 12px;
}

.dialog-form {
  padding-top: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.result-box {
  min-height: 260px;
  max-height: 360px;
  overflow: auto;
  padding: 14px;
  margin: 0;
  background: #173d43;
  color: #d9f8e6;
  border-radius: 8px;
  white-space: pre-wrap;
}

:deep(.el-button--primary:not(.is-link)) {
  --el-button-bg-color: #2f9e58;
  --el-button-border-color: #2f9e58;
  --el-button-text-color: #ffffff;
  --el-button-hover-bg-color: #267e47;
  --el-button-hover-border-color: #267e47;
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: #206d3d;
  --el-button-active-border-color: #206d3d;
  --el-button-active-text-color: #ffffff;
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

:deep(.el-button--danger:not(.is-link):not(.is-plain)) {
  --el-button-bg-color: #d12d2d;
  --el-button-border-color: #d12d2d;
  --el-button-text-color: #ffffff;
  --el-button-hover-bg-color: #b91c1c;
  --el-button-hover-border-color: #b91c1c;
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: #991b1b;
  --el-button-active-border-color: #991b1b;
  --el-button-active-text-color: #ffffff;
  background-color: #d12d2d;
  border-color: #d12d2d;
  color: #ffffff;
  font-weight: 600;
}

:deep(.el-button--danger:not(.is-link):not(.is-plain):hover) {
  background-color: #b91c1c;
  border-color: #b91c1c;
  color: #ffffff;
}

:deep(.el-button--danger.is-plain:not(.is-link)) {
  --el-button-bg-color: #fff1f1;
  --el-button-border-color: #ef9a9a;
  --el-button-text-color: #b42318;
  --el-button-hover-bg-color: #d12d2d;
  --el-button-hover-border-color: #d12d2d;
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: #b91c1c;
  --el-button-active-border-color: #b91c1c;
  background-color: #fff1f1;
  border-color: #ef9a9a;
  color: #b42318;
}

:deep(.el-button--danger.is-plain:not(.is-link):hover) {
  background-color: #d12d2d;
  border-color: #d12d2d;
  color: #ffffff;
}

:deep(.el-button:not(.el-button--primary):not(.el-button--danger):not(.is-link)) {
  --el-button-bg-color: #ffffff;
  --el-button-border-color: #cfd9d3;
  --el-button-text-color: #334155;
  --el-button-hover-bg-color: #f3faf5;
  --el-button-hover-border-color: #76bd84;
  --el-button-hover-text-color: #1f7a4d;
  background-color: #ffffff;
  border-color: #cfd9d3;
  color: #334155;
  font-weight: 600;
}

:deep(.el-button:not(.el-button--primary):not(.el-button--danger):not(.is-link):hover) {
  background-color: #f3faf5;
  border-color: #76bd84;
  color: #1f7a4d;
}

:deep(.el-button.is-link) {
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-hover-bg-color: transparent;
  --el-button-hover-border-color: transparent;
  background-color: transparent;
  border-color: transparent;
  font-weight: 600;
}

:deep(.el-button.is-link.el-button--primary) {
  --el-button-text-color: #1f7a4d;
  --el-button-hover-text-color: #145c39;
  color: #1f7a4d;
}

:deep(.el-button.is-link.el-button--primary:hover) {
  background-color: #eef8f0;
  color: #145c39;
}

:deep(.el-button.is-link.el-button--danger) {
  --el-button-text-color: #b42318;
  --el-button-hover-text-color: #8f1d14;
  background-color: transparent;
  color: #b42318;
}

:deep(.el-button.is-link.el-button--danger:hover) {
  background-color: #fff1f1;
  color: #8f1d14;
}

:deep(.el-button.is-disabled),
:deep(.el-button.is-disabled:hover) {
  background-color: #eef2f5;
  border-color: #d9e1e8;
  color: #7a8790;
}

:deep(.el-table) {
  color: #3c4d52;
}

:deep(.el-table th.el-table__cell) {
  background: #f8fbf8;
  color: #526464;
  font-weight: 600;
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: #63b878;
}

@media (max-width: 900px) {
  .patient-workspace {
    padding-bottom: 16px;
  }

  .auth-card {
    padding: 20px;
  }

  .two-column {
    grid-template-columns: 1fr;
  }

  .topbar {
    display: grid;
    grid-template-columns: 1fr;
    padding: 14px 16px;
  }

  .patient-nav {
    height: 44px;
  }

  .session {
    justify-content: flex-start;
  }
}
</style>
