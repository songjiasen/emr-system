<template>
  <div class="appointments-page">
    <div class="page-header">
      <h1>预约挂号</h1>
      <p>在线预约专家门诊，享受便捷就医体验</p>
    </div>

    <div class="table-section">
      <div class="table-header">
        <h2>我的预约</h2>
        <div class="button-group">
          <el-button type="primary" @click="dialogVisible = true">新增预约</el-button>
          <el-button @click="loadAppointments">刷新</el-button>
        </div>
      </div>

      <el-table :data="appointments" stripe style="width: 100%">
        <el-table-column prop="appointmentNo" label="预约号" min-width="150" />
        <el-table-column prop="doctorName" label="医生" min-width="100" />
        <el-table-column prop="appointmentTime" label="预约时间" min-width="170" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'confirmed' ? 'success' : row.status === 'cancelled' ? 'danger' : 'warning'"
              size="small"
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'cancelled'"
              link
              type="danger"
              size="small"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pagination.limit" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next"
          :page-size="pagination.limit"
          :current-page="pagination.page"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新增预约" width="500px" destroy-on-close>
      <el-form label-position="top" :model="form">
        <el-form-item label="医生">
          <el-select v-model="form.doctorId" placeholder="请选择医生" style="width: 100%">
            <el-option
              v-for="doctor in doctors"
              :key="doctor.id"
              :label="`${doctor.name} - ${doctor.departmentName || ''}`"
              :value="doctor.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预约时间">
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
          <el-button type="primary" :loading="submitting" @click="submitAppointment">提交预约</el-button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, inject } from 'vue';
import { ElMessage } from 'element-plus';
import { createAppointment, fetchAppointments, cancelAppointment } from '../api/appointment';
import { fetchDoctors } from '../api/doctor';
import { unwrap, showError, statusText } from '../utils/common';

const session = inject('session', ref({}));

const appointments = ref([]);
const total = ref(0);
const pagination = reactive({ page: 1, limit: 10 });

const doctors = ref([]);
const dialogVisible = ref(false);
const submitting = ref(false);
const form = ref({
  doctorId: null,
  appointmentTime: tomorrowDefaultTime()
});

function tomorrowDefaultTime() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  d.setHours(9, 0, 0, 0);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

async function loadAppointments() {
  try {
    const userId = session.value.userId || 1;
    const data = unwrap(await fetchAppointments({
      patientId: userId,
      page: pagination.page,
      limit: pagination.limit
    }));
    appointments.value = data.rows || data || [];
    total.value = data.total || 0;
  } catch (error) {
    showError(error);
  }
}

async function loadDoctors() {
  try {
    const data = unwrap(await fetchDoctors({ limit: 100 }));
    doctors.value = data.rows || data || [];
  } catch (error) {
    showError(error);
  }
}

async function submitAppointment() {
  if (!form.value.doctorId) {
    ElMessage.warning('请选择医生');
    return;
  }
  if (!form.value.appointmentTime) {
    ElMessage.warning('请选择预约时间');
    return;
  }
  submitting.value = true;
  try {
    const doctor = doctors.value.find(d => d.id === form.value.doctorId);
    const payload = {
      patientId: session.value.userId,
      patientName: session.value.name || session.value.username,
      doctorId: doctor.id,
      doctorName: doctor.name,
      departmentId: doctor.departmentId,
      departmentName: doctor.departmentName,
      appointmentTime: form.value.appointmentTime
    };
    unwrap(await createAppointment(payload));
    dialogVisible.value = false;
    pagination.page = 1;
    await loadAppointments();
    ElMessage.success('预约已提交');
  } catch (error) {
    showError(error);
  } finally {
    submitting.value = false;
  }
}

async function handleCancel(row) {
  try {
    await cancelAppointment(row.id, { cancelReason: '患者主动取消' });
    await loadAppointments();
    ElMessage.success('预约已取消');
  } catch (error) {
    showError(error);
  }
}

function handlePageChange(page) {
  pagination.page = page;
  loadAppointments();
}

onMounted(() => {
  loadAppointments();
  loadDoctors();
});
</script>

<style scoped>
.appointments-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 28px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #2c4a3e;
  font-weight: 700;
}

.page-header p {
  margin: 0;
  color: #7c8b88;
  font-size: 14px;
}

.table-section {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #eef2ee;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(94, 110, 95, 0.05);
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.table-header h2 {
  margin: 0;
  font-size: 18px;
  color: #2c4a3e;
  font-weight: 700;
}

.button-group {
  display: flex;
  gap: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}
</style>
