<template>
  <div class="article-main-container">
    <div class="actions">
      <el-button type="primary" @click="goToWriteArticle">发布文章</el-button>
      <el-button @click="goToManageArticles">管理我的文章</el-button>
    </div>
    <el-card class="article-list-card">
      <template #header>
        <div class="clearfix">
          <span>最新文章</span>
        </div>
      </template>
      <el-table :data="articles" style="width: 100%" @row-click="readArticle">
        <el-table-column prop="articleTitle" label="标题"></el-table-column>
        <el-table-column prop="userId" label="作者ID" width="120"></el-table-column>
        <el-table-column prop="likesCount" label="点赞" width="100"></el-table-column>
        <el-table-column prop="exposureCount" label="阅读" width="100"></el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="scope">
            <span>{{ formatDate(scope.row.updatedAt) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getAllArticles } from '../api/modules/article'; // Correct path assuming ArticleMain.vue is in Vue/src/article/
import { ElMessage } from 'element-plus';

const router = useRouter();
const articles = ref([]);

// Helper function to format date, can be moved to a utils file later
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  // Basic formatting, can be enhanced with libraries like date-fns or moment.js
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
};

onMounted(async () => {
  try {
    const response = await getAllArticles(); // This is an object { code, message, data }
    if (response.code === 200 && response.data) {
      articles.value = response.data;
    } else {
      ElMessage.error(response.message || '加载文章列表失败');
      articles.value = []; // Ensure articles is an array on failure for table
    }
  } catch (error) {
    console.error('获取文章列表失败:', error);
    ElMessage.error('加载文章列表失败，请稍后重试。');
    articles.value = []; // Ensure articles is an array on error
  }
});

const goToWriteArticle = () => {
  router.push({ name: 'ArticleWrite' }); // Assumes named route
};

const goToManageArticles = () => {
  router.push({ name: 'ArticleManage' }); // Assumes named route
};

const readArticle = (row) => {
  // Assuming 'row.id' contains the article ID from the backend data
  // The user's code used row.id. Article model might have 'id' or 'articleId'
  // Let's assume backend 'Article' model has an 'id' field for its primary key.
  if (row.id) {
    router.push({ name: 'ArticleRead', params: { articleId: row.id } }); // Assumes named route
  } else {
    ElMessage.warning('无法打开文章：缺少文章ID。');
    console.warn('Article row data for navigation:', row);
  }
};
</script>

<style scoped>
.article-main-container {
  padding: 20px;
}
.actions {
  margin-bottom: 20px;
}
.article-list-card {
  /* cursor: pointer; */ /* Table rows are clickable, card itself maybe not */
}
.el-table :deep(.el-table__row) { /* Allow clicking on table row */
    cursor: pointer;
}
</style>
