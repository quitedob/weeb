<template>
  <div class="explore-page">
    <div class="page-header">
      <h1>发现</h1>
      <p>探索社区的热门文章</p>
    </div>
    <div class="article-grid" v-infinite-scroll="loadMore" :infinite-scroll-disabled="loading || noMoreData">
      <el-card v-for="article in articles" :key="article.article_id" class="article-card" shadow="hover">
        <div class="card-content">
          <h2 class="article-title" @click="viewArticle(article.article_id)">{{ article.articleTitle }}</h2>
          <p class="article-summary">{{ truncate(article.articleContent, 100) }}</p>
          <div class="article-meta">
            <span><el-icon><User /></el-icon> {{ article.authorNickname || '匿名用户' }}</span>
            <span><el-icon><Pointer /></el-icon> {{ article.likesCount }}</span>
            <span><el-icon><View /></el-icon> {{ article.exposureCount }}</span>
          </div>
        </div>
      </el-card>
    </div>
    <p v-if="loading" class="loading-text">加载中…</p>
    <el-empty v-if="!loading && articles.length === 0" description="暂无推荐内容">
      <el-button type="primary" @click="fetchRecommendedArticles">刷新试试</el-button>
    </el-empty>
    <p v-if="noMoreData && articles.length > 0" class="loading-text">没有更多了</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import articleApi from '@/api/modules/article';
import { User, Pointer, View } from '@element-plus/icons-vue';

const articles = ref([]);
const router = useRouter();
const loading = ref(false);
const noMoreData = ref(false);
const pagination = ref({
  page: 1,
  pageSize: 12
});

const fetchRecommendedArticles = async () => {
  if (loading.value || noMoreData.value) return;
  loading.value = true;
  try {
    // 使用正确的参数格式调用API
    const response = await articleApi.getRecommendedArticles(
      pagination.value.page,
      pagination.value.pageSize
    );
    const newArticles = response.data.list || [];
    if (newArticles.length < pagination.value.pageSize) {
      noMoreData.value = true;
    }
    articles.value = [...articles.value, ...newArticles];
    pagination.value.page++;
  } catch (error) {
    console.error('获取推荐文章失败:', error);
  } finally {
    loading.value = false;
  }
};

const loadMore = () => {
  fetchRecommendedArticles();
};

const viewArticle = (articleId) => {
  router.push(`/article/${articleId}`);
};

const truncate = (text, length) => {
  if (!text) return '';
  return text.length > length ? text.substring(0, length) + '…' : text;
};

onMounted(() => {
  fetchRecommendedArticles();
});
</script>

<style scoped>
.explore-page {
  padding: 20px;
  background-color: #f5f7fa;
}
.page-header {
  text-align: center;
  margin-bottom: 40px;
}
.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
.article-card {
  cursor: pointer;
  transition: transform 0.2s;
}
.article-card:hover {
  transform: translateY(-5px);
}
.article-title {
  font-size: 1.2rem;
  margin-bottom: 10px;
}
.article-summary {
  font-size: 0.9rem;
  color: #606266;
  margin-bottom: 15px;
}
.article-meta {
  font-size: 0.8rem;
  color: #909399;
  display: flex;
  gap: 15px;
  align-items: center;
}
.loading-text {
  text-align: center;
  margin-top: 20px;
  color: #909399;
}
</style>
