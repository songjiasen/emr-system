<template>
  <div class="profile-page">
    <div class="profile-card">
      <div class="profile-header">
        <div class="user-avatar">{{ avatarText }}</div>
        <div class="user-name">
          <h2>{{ session.name || session.username || '患者' }}</h2>
          <el-tag type="success" size="small">{{ session.roleCode || 'patient' }}</el-tag>
        </div>
      </div>

      <el-descriptions :column="1" border class="info-table">
        <el-descriptions-item label="用户ID">{{ session.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ session.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ session.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ session.roleCode || 'patient' }}</el-descriptions-item>
        <el-descriptions-item label="账户余额">
          <span class="balance-highlight">¥{{ patientBalance }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="action-buttons">
        <el-button type="primary" @click="passwordDialog = true">修改密码</el-button>
        <el-button type="warning" @click="rechargeDialog = true">充 值</el-button>
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    <el-dialog v-model="passwordDialog" title="修改密码" width="420px" destroy-on-close>
      <el-form label-position="top" :model="passwordForm">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <div class="dialog-footer">
          <el-button @click="passwordDialog = false">取消</el-button>
          <el-button type="primary" :loading="changingPwd" @click="submitPasswordChange">保存新密码</el-button>
        </div>
      </el-form>
    </el-dialog>

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
import { ref, computed, inject } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getStoredToken, clearAuthState } from '../utils/session';

const router = useRouter();

const session = inject('session', ref({}));
const patientBalance = inject('patientBalance', ref('0.00'));
const submitRechargeFn = inject('submitRecharge', () => {});
const submitPasswordChangeFn = inject('submitPasswordChange', () => {});

const avatarText = computed(() => String(session.value.name || session.value.username || '患').slice(0, 1));

const passwordDialog = ref(false);
const rechargeDialog = ref(false);
const changingPwd = ref(false);
const recharging = ref(false);
const rechargeAmount = ref(100);

const passwordForm = ref({
  oldPassword: '',
  newPassword: ''
});

async function submitPasswordChange() {
  if (!passwordForm.value.oldPassword) {
    ElMessage.warning('请输入旧密码');
    return;
  }
  if (!passwordForm.value.newPassword) {
    ElMessage.warning('请输入新密码');
    return;
  }
  changingPwd.value = true;
  try {
    const ok = await submitPasswordChangeFn(passwordForm.value);
    if (ok) {
      passwordDialog.value = false;
      passwordForm.value = { oldPassword: '', newPassword: '' };
      ElMessage.success('密码已修改');
    }
  } finally {
    changingPwd.value = false;
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

function handleLogout() {
  clearAuthState();
  ElMessage.success('已退出登录');
  window.location.reload();
}
</script>

<style scoped>
.profile-page {
  max-width: 600px;
  margin: 0 auto;
}

.profile-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #eef2ee;
  padding: 32px;
  box-shadow: 0 8px 24px rgba(94, 110, 95, 0.06);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eef2ee;
}

.user-avatar {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #63b878, #8bcf9a);
  color: #ffffff;
  border-radius: 50%;
  font-size: 28px;
  font-weight: 700;
}

.user-name h2 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #2c3e3a;
}

.info-table {
  margin-bottom: 24px;
}

.balance-highlight {
  font-size: 20px;
  font-weight: 700;
  color: #e6a23c;
}

.action-buttons {
  display: flex;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #eef2ee;
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

@media (max-width: 600px) {
  .profile-card {
    padding: 20px;
  }

  .action-buttons {
    flex-wrap: wrap;
  }
}
</style>
