<template>
  <div class="home-page">
    <section class="hero-carousel" v-if="carousels.length > 0">
      <el-carousel height="360px" :interval="5000" arrow="hover">
        <el-carousel-item v-for="item in carousels" :key="item.id">
          <div class="carousel-slide" :style="{ backgroundImage: `url(${item.imageUrl})` }">
            <div class="carousel-overlay">
              <h2>{{ item.title }}</h2>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="section featured-doctors" v-if="featuredDoctors.length > 0">
      <div class="section-header">
        <h2>金牌医生团队</h2>
        <p>汇聚各科室资深专家，为您提供专业医疗服务</p>
      </div>
      <div class="doctor-grid">
        <el-card
          v-for="doctor in featuredDoctors"
          :key="doctor.id"
          class="doctor-card"
          shadow="hover"
          @click="$router.push(`/doctor/${doctor.id}`)"
        >
          <div class="doctor-avatar">{{ (doctor.name || '医')[0] }}</div>
          <h3>{{ doctor.name }}</h3>
          <p class="department">{{ doctor.departmentName }}</p>
          <p class="specialty">{{ doctor.specialty }}</p>
        </el-card>
      </div>
    </section>

    <section class="section hospital-intro">
      <div class="intro-card">
        <div class="intro-icon">
          <span>✚</span>
        </div>
        <h2>医院简介</h2>
        <p>安心医疗是一所集医疗、教学、科研、预防、保健为一体的现代化三级甲等综合性医院。医院始建于 1998 年，经过二十余年的发展，现已成为区域内重要的医疗服务中心。</p>
        <p>医院占地面积 12 万平方米，开放床位 1500 张，设有 48 个临床科室和 12 个医技科室。拥有一支由知名专家领衔的高素质医疗团队，现有职工 2800 余人，其中高级职称 450 余人。</p>
        <div class="intro-stats">
          <div class="stat-item">
            <span class="stat-number">25+</span>
            <span class="stat-label">建院年限</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">1500</span>
            <span class="stat-label">开放床位</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">48</span>
            <span class="stat-label">临床科室</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">2800+</span>
            <span class="stat-label">全院职工</span>
          </div>
        </div>
      </div>
    </section>

    <section class="section news-preview" v-if="newsList.length > 0">
      <div class="section-header">
        <h2>健康资讯</h2>
        <el-button link type="primary" @click="$router.push('/news')">查看更多 →</el-button>
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
          <h3>{{ item.title }}</h3>
          <p class="news-summary">{{ item.summary || item.content?.slice(0, 80) || '' }}</p>
          <span class="news-time">{{ item.createTime || '' }}</span>
        </el-card>
      </div>
    </section>

    <section class="section messages-preview">
      <div class="section-header">
        <h2>留言咨询</h2>
        <el-button link type="primary" @click="$router.push('/messages')">查看更多 →</el-button>
      </div>
      <div class="messages-grid">
        <el-card
          v-for="msg in latestMessages"
          :key="msg.id"
          class="message-card"
          shadow="hover"
        >
          <template #header>
            <div class="msg-card-header">
              <span class="msg-user">{{ msg.userName || '患者' }}</span>
              <span class="msg-time">{{ msg.createdAt || '' }}</span>
            </div>
          </template>
          <h3 class="msg-title">{{ msg.title }}</h3>
          <p class="msg-content">{{ msg.content?.slice(0, 100) || '' }}</p>
          <div v-if="msg.replyContent" class="msg-reply-badge">
            <el-tag size="small" type="success">已回复</el-tag>
          </div>
        </el-card>
      </div>
      <div v-if="latestMessages.length === 0" class="empty-hint">
        <p>暂无留言，快去提交你的问题吧</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchCarousels, fetchNews, fetchMessages } from '../api/system';
import { fetchDoctors } from '../api/doctor';
import { unwrap, showError } from '../utils/common';

const carousels = ref([]);
const featuredDoctors = ref([]);
const newsList = ref([]);
const latestMessages = ref([]);

async function loadCarousels() {
  try {
    const data = unwrap(await fetchCarousels());
    carousels.value = data.rows || data || [];
  } catch (error) {
    showError(error);
  }
}

async function loadFeaturedDoctors() {
  try {
    const data = unwrap(await fetchDoctors({ limit: 100 }));
    const rows = data.rows || data || [];
    featuredDoctors.value = rows.filter(d => d.isFeatured === true);
  } catch (error) {
    showError(error);
  }
}

async function loadLatestNews() {
  try {
    const data = unwrap(await fetchNews({ page: 1, limit: 6 }));
    newsList.value = data.rows || data || [];
  } catch (error) {
    showError(error);
  }
}

async function loadLatestMessages() {
  try {
    const data = unwrap(await fetchMessages({ page: 1, limit: 4 }));
    latestMessages.value = data.rows || data || [];
  } catch (error) {
    showError(error);
  }
}

onMounted(() => {
  loadCarousels();
  loadFeaturedDoctors();
  loadLatestNews();
  loadLatestMessages();
});
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 32px;
}

.section {
  max-width: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  font-size: 22px;
  color: #2c4a3e;
  font-weight: 700;
}

.section-header p {
  margin: 4px 0 0;
  color: #7c8b88;
  font-size: 14px;
}

.hero-carousel {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.carousel-slide {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-color: #e8f0e8;
}

.carousel-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24px 32px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.5));
  color: #ffffff;
}

.carousel-overlay h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

.doctor-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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

.department {
  margin: 0 0 4px;
  color: #4fa66b;
  font-size: 13px;
  font-weight: 500;
}

.specialty {
  margin: 0;
  color: #7c8b88;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.intro-card {
  background: linear-gradient(135deg, #f0f9f3, #ffffff);
  border: 1px solid #d8ebdb;
  border-radius: 12px;
  padding: 32px;
  text-align: center;
}

.intro-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  display: grid;
  place-items: center;
  background: #eef8f0;
  border: 2px solid #63b878;
  border-radius: 14px;
  color: #3f8067;
  font-size: 24px;
  font-weight: 700;
}

.intro-card h2 {
  margin: 0 0 12px;
  font-size: 22px;
  color: #2c4a3e;
}

.intro-card p {
  margin: 0 0 10px;
  color: #5a6b65;
  font-size: 14px;
  line-height: 1.8;
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;
}

.intro-stats {
  display: flex;
  justify-content: center;
  gap: 48px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #d8ebdb;
}

.stat-item {
  display: grid;
  gap: 4px;
  text-align: center;
}

.stat-number {
  font-size: 26px;
  font-weight: 700;
  color: #3f8067;
}

.stat-label {
  font-size: 13px;
  color: #7c8b88;
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.news-card {
  border-radius: 10px;
  border: 1px solid #eef2ee;
}

.news-tag {
  margin-bottom: 10px;
}

.news-card h3 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #2c3e3a;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.news-summary {
  margin: 0 0 10px;
  color: #7c8b88;
  font-size: 13px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.news-time {
  font-size: 12px;
  color: #b0b8b4;
}

.messages-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.message-card {
  border-radius: 10px;
  border: 1px solid #eef2ee;
}

.msg-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.msg-user {
  font-weight: 600;
  color: #3f8067;
  font-size: 14px;
}

.msg-time {
  font-size: 12px;
  color: #b0b8b4;
}

.msg-title {
  margin: 0 0 8px;
  font-size: 15px;
  color: #2c3e3a;
}

.msg-content {
  margin: 0 0 8px;
  color: #5a6b65;
  font-size: 13px;
  line-height: 1.6;
}

.msg-reply-badge {
  margin-top: 4px;
}

.empty-hint {
  text-align: center;
  padding: 32px;
  color: #a0b0a8;
  font-size: 14px;
}

@media (max-width: 900px) {
  .doctor-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .news-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .messages-grid {
    grid-template-columns: 1fr;
  }

  .intro-stats {
    gap: 24px;
  }
}

@media (max-width: 600px) {
  .doctor-grid,
  .news-grid {
    grid-template-columns: 1fr;
  }

  .intro-stats {
    flex-wrap: wrap;
    gap: 16px;
  }
}
</style>
