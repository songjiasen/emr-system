<template>
  <div class="tests-page">
    <div class="page-header">
      <h1>检查申请</h1>
      <p>查看检查申请、审核意见与检查结果</p>
    </div>

    <div class="table-section">
      <el-table :data="testRequests" stripe style="width: 100%">
        <el-table-column prop="testItem" label="检查项目" min-width="150" />
        <el-table-column prop="departmentName" label="负责科室" min-width="120" />
        <el-table-column prop="doctorName" label="申请医生" min-width="110" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'approved' ? 'warning' : row.status === 'finished' ? 'success' : 'info'"
              size="small"
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultContent" label="检查结果" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'approved'"
              size="small"
              type="primary"
              @click="handleGoCheck(row)"
            >
              去检查
            </el-button>
            <span v-else-if="row.status === 'paid'" class="paid-tag">已支付</span>
            <span v-else-if="row.status === 'finished'" class="finished-tag">已完成</span>
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
import { ElMessage } from 'element-plus';
import { fetchTestRequests, payTestRequest } from '../api/clinical';
import { fetchFees, createFee } from '../api/billing';
import { unwrap, showError, statusText, rowsOf } from '../utils/common';

const testRequests = ref([]);
const total = ref(0);
const pagination = reactive({ page: 1, limit: 10 });

async function loadTests() {
  try {
    const data = unwrap(await fetchTestRequests({ page: pagination.page, limit: pagination.limit }));
    testRequests.value = data.rows || data || [];
    total.value = data.total || 0;
  } catch (error) {
    showError(error);
  }
}

async function handleGoCheck(row) {
  try {
    const existingFeePage = unwrap(await fetchFees({
      businessType: 'test_request',
      businessId: row.id,
      payStatus: 'unpaid',
      page: 1,
      limit: 1
    }));
    const existingFee = rowsOf(existingFeePage)[0];
    if (!existingFee) {
      unwrap(await createFee({
        patientId: row.patientId,
        patientName: row.patientName || '患者',
        businessType: 'test_request',
        businessId: row.id,
        feeItemCode: 'lab_test'
      }));
    }
    unwrap(await payTestRequest(row.id));
    await loadTests();
    ElMessage.success('已生成检查费用，请在费用支付页面完成支付');
  } catch (error) {
    showError(error);
  }
}

function handlePageChange(page) {
  pagination.page = page;
  loadTests();
}

onMounted(() => {
  loadTests();
});
</script>

<style scoped>
.tests-page {
  max-width: 1100px;
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

.paid-tag {
  display: inline-block;
  padding: 2px 10px;
  color: #0b74b8;
  background: #dbeafe;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.finished-tag {
  display: inline-block;
  padding: 2px 10px;
  color: #16a34a;
  background: #dcfce7;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}
</style>
