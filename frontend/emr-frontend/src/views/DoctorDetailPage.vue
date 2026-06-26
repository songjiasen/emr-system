<template>
  <div class="doctor-detail-page">
    <div v-if="loading" class="loading-state">
      <span>加载中...</span>
    </div>

    <template v-else-if="doctor">
      <div class="detail-card">
        <div class="doctor-profile">
          <div class="doctor-avatar-large">{{ (doctor.name || '医')[0] }}</div>
          <div class="doctor-info">
            <h1>{{ doctor.name }}</h1>
            <p class="dept-tag">
              <el-tag type="success">{{ doctor.departmentName }}</el-tag>
            </p>
            <p class="specialty-label">擅长领域</p>
            <p class="specialty-text">{{ doctor.specialty }}</p>
            <p v-if="doctor.profile" class="profile-text">{{ doctor.profile }}</p>
            <p v-if="doctor.phone" class="phone-text">
              <el-icon><Phone /></el-icon> {{ doctor.phone }}
            </p>
            <el-button type="primary" size="large" class="appointment-btn" @click="dialogVisible = true">
              预约挂号
            </el-button>
          </div>
        </div>
      </div>

      <el-dialog v-model="dialogVisible" title="预约挂号" width="480px" destroy-on-close>
        <el-form label-position="top" :model="form">
          <el-form-item label="患者姓名">
            <el-input :value="patientName" disabled />
          </el-form-item>
          <el-form-item label="预约医生">
            <el-input :value="doctor.name" disabled />
          </el-form-item>
          <el-form-item label="预约时间" required>
            <el-date-picker
              v-model="form.appointmentTime"
              type="datetime"
              placeholder="请选择预约时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <div class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="submitAppointment">确认预约</el-button>
          </div>
        </el-form>
      </el-dialog>
    </template>

    <div v-else class="empty-state">
      <p>未找到医生信息</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchDoctorDetail } from '../api/doctor';
import { createAppointment } from '../api/appointment';
import { unwrap, showError } from '../utils/common';

const route = useRoute();
const router = useRouter();
const session = inject('session', ref({}));

const doctor = ref(null);
const loading = ref(true);
const dialogVisible = ref(false);
const submitting = ref(false);
const form = ref({
  appointmentTime: tomorrowDefaultTime()
});

const patientName = computed(() => session.value.name || session.value.username || '患者');

function tomorrowDefaultTime() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  d.setHours(9, 0, 0, 0);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

async function loadDetail() {
  const id = route.params.id;
  if (!id) {
    loading.value = false;
    return;
  }
  try {
    doctor.value = unwrap(await fetchDoctorDetail(id));
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
}

async function submitAppointment() {
  if (!form.value.appointmentTime) {
    ElMessage.warning('请选择预约时间');
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      patientId: session.value.userId,
      patientName: session.value.name || session.value.username,
      doctorId: doctor.value.id,
      doctorName: doctor.value.name,
      departmentId: doctor.value.departmentId,
      departmentName: doctor.value.departmentName,
      appointmentTime: form.value.appointmentTime
    };
    unwrap(await createAppointment(payload));
    dialogVisible.value = false;
    ElMessage.success('预约成功');
  } catch (error) {
    showError(error);
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.doctor-detail-page {
  max-width: 800px;
  margin: 0 auto;
}

.loading-state {
  text-align: center;
  padding: 48px;
  color: #7c8b88;
}

.detail-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #eef2ee;
  box-shadow: 0 8px 24px rgba(94, 110, 95, 0.06);
  overflow: hidden;
}

.doctor-profile {
  display: flex;
  gap: 32px;
  padding: 32px;
}

.doctor-avatar-large {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #63b878, #8bcf9a);
  color: #ffffff;
  border-radius: 50%;
  font-size: 42px;
  font-weight: 700;
}

.doctor-info {
  flex: 1;
  min-width: 0;
}

.doctor-info h1 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #2c3e3a;
}

.dept-tag {
  margin: 0 0 16px;
}

.specialty-label {
  margin: 0 0 4px;
  font-size: 13px;
  color: #a0b0a8;
}

.specialty-text {
  margin: 0 0 12px;
  font-size: 15px;
  color: #53656a;
  font-weight: 500;
}

.profile-text {
  margin: 0 0 12px;
  font-size: 14px;
  color: #5a6b65;
  line-height: 1.7;
}

.phone-text {
  margin: 0 0 16px;
  font-size: 14px;
  color: #53656a;
  display: flex;
  align-items: center;
  gap: 4px;
}

.appointment-btn {
  min-width: 160px;
  height: 42px;
  font-size: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.empty-state {
  text-align: center;
  padding: 48px;
  color: #a0b0a8;
}

@media (max-width: 600px) {
  .doctor-profile {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .doctor-avatar-large {
    width: 96px;
    height: 96px;
    font-size: 32px;
  }
}
</style>
