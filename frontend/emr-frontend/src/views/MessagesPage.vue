<template>
  <div class="messages-page">
    <div class="page-header">
      <h1>留言咨询</h1>
      <p>向医护人员提交咨询，获取专业回复</p>
    </div>

    <div class="post-form-card">
      <h3>发布留言</h3>
      <el-form label-position="top" :model="form">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入留言标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="请详细描述您的问题..."
          />
        </el-form-item>
        <el-button type="primary" :loading="posting" @click="submitMessage">提交留言</el-button>
      </el-form>
    </div>

    <div class="messages-list">
      <div v-if="messages.length === 0 && !loading" class="empty-state">
        <p>暂无留言记录</p>
      </div>

      <el-card
        v-for="msg in messages"
        :key="msg.id"
        class="message-card"
        shadow="hover"
      >
        <template #header>
          <div class="message-header">
            <span class="user-name">{{ msg.userName || '患者' }}</span>
            <span class="message-time">{{ msg.createdAt || '' }}</span>
          </div>
        </template>
        <h3 class="msg-title">{{ msg.title }}</h3>
        <p class="msg-content">{{ msg.content }}</p>
        <div v-if="msg.replyContent" class="reply-section">
          <div class="reply-header">
            <el-tag size="small" type="success">医生回复</el-tag>
          </div>
          <p class="reply-content">{{ msg.replyContent }}</p>
        </div>
      </el-card>

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
import { ref, reactive, onMounted, inject } from 'vue';
import { ElMessage } from 'element-plus';
import { createMessage, fetchMessages } from '../api/system';
import { unwrap, showError } from '../utils/common';

const session = inject('session', ref({}));

const messages = ref([]);
const loading = ref(false);
const posting = ref(false);
const total = ref(0);
const pagination = reactive({ page: 1, limit: 10 });

const form = ref({
  title: '',
  content: ''
});

async function loadMessages() {
  loading.value = true;
  try {
    const data = unwrap(await fetchMessages({ page: pagination.page, limit: pagination.limit }));
    messages.value = data.rows || data || [];
    total.value = data.total || 0;
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
}

async function submitMessage() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入留言标题');
    return;
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入留言内容');
    return;
  }
  posting.value = true;
  try {
    await createMessage({
      userId: session.value.userId,
      userName: session.value.name || session.value.username,
      title: form.value.title,
      content: form.value.content
    });
    form.value.title = '';
    form.value.content = '';
    pagination.page = 1;
    await loadMessages();
    ElMessage.success('留言已提交');
  } catch (error) {
    showError(error);
  } finally {
    posting.value = false;
  }
}

function handlePageChange(page) {
  pagination.page = page;
  loadMessages();
}

onMounted(() => {
  loadMessages();
});
</script>

<style scoped>
.messages-page {
  max-width: 800px;
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

.post-form-card {
  background: #ffffff;
  border: 1px solid #eef2ee;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(94, 110, 95, 0.05);
}

.post-form-card h3 {
  margin: 0 0 16px;
  font-size: 17px;
  color: #2c4a3e;
  font-weight: 600;
}

.messages-list {
  display: grid;
  gap: 16px;
}

.message-card {
  border-radius: 10px;
  border: 1px solid #eef2ee;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-name {
  font-weight: 600;
  color: #3f8067;
  font-size: 14px;
}

.message-time {
  font-size: 12px;
  color: #b0b8b4;
}

.msg-title {
  margin: 0 0 10px;
  font-size: 16px;
  color: #2c3e3a;
}

.msg-content {
  margin: 0;
  font-size: 14px;
  color: #5a6b65;
  line-height: 1.6;
}

.reply-section {
  margin-top: 16px;
  padding: 16px;
  background: #f6fdf7;
  border-left: 3px solid #63b878;
  border-radius: 6px;
}

.reply-header {
  margin-bottom: 8px;
}

.reply-content {
  margin: 0;
  font-size: 14px;
  color: #3c4d52;
  line-height: 1.6;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.empty-state {
  text-align: center;
  padding: 48px;
  color: #a0b0a8;
}
</style>
