<template>
  <div class="news-page">
    <div class="page-header">
      <h1>健康资讯</h1>
      <p>了解最新健康知识、医院公告与公益活动</p>
    </div>

    <div class="news-grid">
      <el-card
        v-for="item in newsList"
        :key="item.id"
        class="news-card"
        shadow="hover"
      >
        <div class="news-tag">
          <el-tag size="small" type="success">{{ item.category || '健康' }}</el-tag>
        </div>
        <h3 class="news-title">{{ item.title }}</h3>
        <p class="news-summary">{{ item.summary || item.content?.slice(0, 120) || '' }}</p>
        <div class="news-meta">
          <span class="news-time">{{ item.createTime || item.publishTime || '' }}</span>
        </div>
      </el-card>
    </div>

    <div v-if="newsList.length === 0 && !loading" class="empty-state">
      <p>暂无健康资讯</p>
    </div>

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
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchNews } from '../api/system';
import { unwrap, showError } from '../utils/common';

const newsList = ref([]);
const loading = ref(false);
const total = ref(0);
const pagination = reactive({ page: 1, limit: 9 });

async function loadNews() {
  loading.value = true;
  try {
    const data = unwrap(await fetchNews({ page: pagination.page, limit: pagination.limit }));
    newsList.value = data.rows || data || [];
    total.value = data.total || 0;
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
}

function handlePageChange(page) {
  pagination.page = page;
  loadNews();
}

onMounted(() => {
  loadNews();
});
</script>

<style scoped>
.news-page {
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

.news-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.news-card {
  border-radius: 10px;
  border: 1px solid #eef2ee;
  transition: transform 0.2s, box-shadow 0.2s;
}

.news-card:hover {
  transform: translateY(-2px);
}

.news-tag {
  margin-bottom: 10px;
}

.news-title {
  margin: 0 0 10px;
  font-size: 16px;
  color: #2c3e3a;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.news-summary {
  margin: 0 0 14px;
  color: #7c8b88;
  font-size: 14px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.news-meta {
  display: flex;
  justify-content: flex-end;
}

.news-time {
  font-size: 12px;
  color: #b0b8b4;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

.empty-state {
  text-align: center;
  padding: 48px;
  color: #a0b0a8;
}

@media (max-width: 900px) {
  .news-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .news-grid {
    grid-template-columns: 1fr;
  }
}
</style>
