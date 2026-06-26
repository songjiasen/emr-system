<template>
  <div class="prescriptions-page">
    <div class="page-header">
      <h1>处方记录</h1>
      <p>查看医生为您开具的处方详情</p>
    </div>

    <div class="table-section">
      <el-table :data="prescriptions" stripe style="width: 100%">
        <el-table-column prop="medicineName" label="药品名称" min-width="160" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="unitPrice" label="单价 (元)" width="110" />
        <el-table-column prop="doctorName" label="开方医生" min-width="110" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'executed' || row.status === 'finished' ? 'success' : 'warning'"
              size="small"
            >
              {{ statusText(row.status) }}
            </el-tag>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchPrescriptions } from '../api/clinical';
import { unwrap, showError, statusText } from '../utils/common';

const prescriptions = ref([]);
const total = ref(0);
const pagination = reactive({ page: 1, limit: 10 });

async function loadPrescriptions() {
  try {
    const data = unwrap(await fetchPrescriptions({ page: pagination.page, limit: pagination.limit }));
    prescriptions.value = data.rows || data || [];
    total.value = data.total || 0;
  } catch (error) {
    showError(error);
  }
}

function handlePageChange(page) {
  pagination.page = page;
  loadPrescriptions();
}

onMounted(() => {
  loadPrescriptions();
});
</script>

<style scoped>
.prescriptions-page {
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

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
