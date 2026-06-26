<template>
  <div class="fees-page">
    <div class="balance-card">
      <div class="balance-info">
        <span class="balance-label">账户余额</span>
        <span class="balance-value">¥{{ patientBalance }}</span>
      </div>
      <el-button type="warning" size="large" round @click="rechargeDialog = true">
        充 值
      </el-button>
    </div>

    <div class="table-section">
      <h2>费用记录</h2>
      <el-table :data="fees" stripe style="width: 100%">
        <el-table-column prop="feeNo" label="费用号" min-width="140" />
        <el-table-column prop="feeItem" label="费用项目" min-width="140" />
        <el-table-column prop="amount" label="金额 (元)" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'paid' ? 'success' : 'warning'"
              size="small"
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'unpaid'"
              size="small"
              type="primary"
              @click="handlePayFee(row)"
            >
              支付
            </el-button>
            <span v-else-if="row.status === 'paid'" class="paid-tag">已支付</span>
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

    <el-dialog v-model="rechargeDialog" title="余额充值" width="420px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="充值金额 (元)">
          <el-input-number
            v-model="rechargeAmount"
            :precision="2"
            :min="0.01"
            :step="100"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="快捷金额">
          <div class="quick-amounts">
            <el-button
              v-for="amt in [100, 200, 500, 1000]"
              :key="amt"
              :type="rechargeAmount === amt ? 'warning' : 'default'"
              @click="rechargeAmount = amt"
            >
              {{ amt }}
            </el-button>
          </div>
        </el-form-item>
        <div class="dialog-footer">
          <el-button @click="rechargeDialog = false">取消</el-button>
          <el-button type="primary" :loading="recharging" @click="submitRecharge">确认充值</el-button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, inject } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchFees, payFee } from '../api/billing';
import { unwrap, showError, statusText } from '../utils/common';

const patientBalance = inject('patientBalance', ref('0.00'));
const submitRechargeFn = inject('submitRecharge', () => {});
const loadBalanceFn = inject('loadPatientBalance', () => {});

const fees = ref([]);
const total = ref(0);
const pagination = reactive({ page: 1, limit: 10 });

const rechargeDialog = ref(false);
const rechargeAmount = ref(100);
const recharging = ref(false);

async function loadFees() {
  try {
    const data = unwrap(await fetchFees({ page: pagination.page, limit: pagination.limit }));
    fees.value = data.rows || data || [];
    total.value = data.total || 0;
  } catch (error) {
    showError(error);
  }
}

async function handlePayFee(row) {
  try {
    unwrap(await payFee(row.id));
    await loadFees();
    ElMessage.success('支付成功');
  } catch (error) {
    showError(error);
  }
}

async function submitRecharge() {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    ElMessage.warning('请输入充值金额');
    return;
  }
  recharging.value = true;
  try {
    const ok = await submitRechargeFn(rechargeAmount.value);
    if (ok) {
      rechargeDialog.value = false;
      ElMessage.success(`充值成功，当前余额 ¥${patientBalance.value}`);
    }
  } finally {
    recharging.value = false;
  }
}

function handlePageChange(page) {
  pagination.page = page;
  loadFees();
}

onMounted(() => {
  loadFees();
});
</script>

<style scoped>
.fees-page {
  max-width: 900px;
  margin: 0 auto;
}

.balance-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #fef9e7, #fdebd0);
  border: 1px solid #f0d89d;
  border-radius: 14px;
  padding: 24px 32px;
  margin-bottom: 24px;
}

.balance-info {
  display: grid;
  gap: 6px;
}

.balance-label {
  font-size: 14px;
  color: #7d6608;
  font-weight: 500;
}

.balance-value {
  font-size: 32px;
  color: #e6a23c;
  font-weight: 700;
}

.table-section h2 {
  margin: 0 0 16px;
  font-size: 18px;
  color: #2c4a3e;
  font-weight: 700;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.paid-tag {
  display: inline-block;
  padding: 2px 10px;
  color: #16a34a;
  background: #dcfce7;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.quick-amounts {
  display: flex;
  gap: 8px;
}
</style>
