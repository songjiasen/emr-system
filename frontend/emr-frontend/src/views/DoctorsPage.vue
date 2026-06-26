<template>
  <div class="doctors-page">
    <div class="page-header">
      <h1>医生团队</h1>
      <p>了解各科室专家，选择适合您的医生</p>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><i class="el-icon-loading" /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else>
      <div v-for="group in groupedDoctors" :key="group.department" class="department-group">
        <div class="dept-header">
          <h2>{{ group.department }}</h2>
          <span class="dept-count">共 {{ group.doctors.length }} 位医生</span>
        </div>
        <div class="doctor-grid">
          <el-card
            v-for="doctor in group.doctors"
            :key="doctor.id"
            class="doctor-card"
            shadow="hover"
            @click="$router.push(`/doctor/${doctor.id}`)"
          >
            <div class="doctor-avatar">{{ (doctor.name || '医')[0] }}</div>
            <h3>{{ doctor.name }}</h3>
            <p class="specialty">{{ doctor.specialty }}</p>
            <p class="department">{{ doctor.departmentName }}</p>
            <el-button size="small" type="primary" @click.stop="$router.push(`/doctor/${doctor.id}`)">
              查看详情
            </el-button>
          </el-card>
        </div>
      </div>

      <div v-if="groupedDoctors.length === 0" class="empty-state">
        <p>暂无医生信息</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { fetchDepartments } from '../api/department';
import { fetchDoctors } from '../api/doctor';
import { unwrap, showError } from '../utils/common';

const doctors = ref([]);
const loading = ref(true);

const groupedDoctors = computed(() => {
  const map = {};
  for (const doctor of doctors.value) {
    const dept = doctor.departmentName || '其他科室';
    if (!map[dept]) {
      map[dept] = [];
    }
    map[dept].push(doctor);
  }
  return Object.entries(map).map(([department, docs]) => ({
    department,
    doctors: docs
  }));
});

async function loadData() {
  loading.value = true;
  try {
    await fetchDepartments();
  } catch (error) {
  }
  try {
    const data = unwrap(await fetchDoctors({ limit: 100 }));
    doctors.value = data.rows || data || [];
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.doctors-page {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
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

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px;
  color: #7c8b88;
}

.department-group {
  margin-bottom: 32px;
}

.dept-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #eef8f0;
}

.dept-header h2 {
  margin: 0;
  font-size: 18px;
  color: #3f8067;
  font-weight: 600;
}

.dept-count {
  font-size: 13px;
  color: #a0b0a8;
}

.doctor-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.doctor-card {
  text-align: center;
  cursor: pointer;
  border-radius: 10px;
  border: 1px solid #eef2ee;
  transition: transform 0.2s, box-shadow 0.2s;
}

.doctor-card:hover {
  transform: translateY(-2px);
}

.doctor-avatar {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #63b878, #8bcf9a);
  color: #ffffff;
  border-radius: 50%;
  font-size: 24px;
  font-weight: 700;
}

.doctor-card h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #2c3e3a;
}

.specialty {
  margin: 0 0 4px;
  color: #53656a;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.department {
  margin: 0 0 12px;
  color: #4fa66b;
  font-size: 13px;
  font-weight: 500;
}

.empty-state {
  text-align: center;
  padding: 48px;
  color: #a0b0a8;
}

@media (max-width: 900px) {
  .doctor-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .doctor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
