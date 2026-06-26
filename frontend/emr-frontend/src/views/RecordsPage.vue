<template>
  <div class="records-page">
    <div class="page-header">
      <h1>就诊记录</h1>
      <p>查看您的历史就诊记录和诊断详情</p>
    </div>

    <div class="table-section">
      <el-table
        :data="records"
        stripe
        style="width: 100%"
        @row-click="handleRowClick"
        :row-class-name="rowClassName"
      >
        <el-table-column prop="recordNo" label="病历号" min-width="150" />
        <el-table-column prop="patientName" label="患者" min-width="100" />
        <el-table-column prop="diagnosis" label="诊断" min-width="200" show-overflow-tooltip />
        <el-table-column prop="archiveStatus" label="归档状态" width="120">
          <template #default="{ row }">
            <el-tag
              :type="row.archiveStatus === 'archived' ? 'success' : 'info'"
              size="small"
            >
              {{ statusText(row.archiveStatus) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-collapse-transition>
        <div v-if="selectedDetail" class="detail-panel">
          <h3>病历详情</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="主诉">{{ selectedDetail.chiefComplaint || '-' }}</el-descriptions-item>
            <el-descriptions-item label="现病史">{{ selectedDetail.presentIllness || '-' }}</el-descriptions-item>
            <el-descriptions-item label="治疗建议">{{ selectedDetail.treatmentAdvice || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-collapse-transition>

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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchMedicalRecords, fetchMedicalRecordDetail } from '../api/medicalRecord';
import { unwrap, showError, statusText } from '../utils/common';

const records = ref([]);
const total = ref(0);
const pagination = reactive({ page: 1, limit: 10 });
const selectedDetail = ref(null);
const selectedRowId = ref(null);

async function loadRecords() {
  try {
    const data = unwrap(await fetchMedicalRecords({ page: pagination.page, limit: pagination.limit }));
    records.value = data.rows || data || [];
    total.value = data.total || 0;
  } catch (error) {
    showError(error);
  }
}

async function handleRowClick(row) {
  if (!row?.id) return;
  if (selectedRowId.value === row.id) {
    selectedRowId.value = null;
    selectedDetail.value = null;
    return;
  }
  try {
    selectedDetail.value = unwrap(await fetchMedicalRecordDetail(row.id));
    selectedRowId.value = row.id;
  } catch (error) {
    showError(error);
  }
}

function rowClassName({ row }) {
  return row.id === selectedRowId.value ? 'selected-row' : '';
}

function handlePageChange(page) {
  pagination.page = page;
  selectedDetail.value = null;
  selectedRowId.value = null;
  loadRecords();
}

onMounted(() => {
  loadRecords();
});
</script>

<style scoped>
.records-page {
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

.detail-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eef2ee;
}

.detail-panel h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #2c3e3a;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
