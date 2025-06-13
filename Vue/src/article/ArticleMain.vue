<template>
  <div class="article-main-container">
    <div class="actions">
      <el-button type="primary" @click="goToWriteArticle">发布文章</el-button>
      <el-button @click="goToManageArticles">管理我的文章</el-button>
    </div>
    <el-card class="article-list-card">
      <template #header>
        <span>最新文章</span>
      </template>
      <el-table :data="articles" style="width: 100%" @row-click="readArticle" v-loading="loading">
        <el-table-column prop="articleTitle" label="标题"></el-table-column>
        <el-table-column prop="userId" label="作者ID" width="120"></el-table-column>
        <el-table-column prop="likesCount" label="点赞" width="100"></el-table-column>
        <el-table-column prop="exposureCount" label="阅读" width="100"></el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180"></el-table-column>
        <!-- Raw date string will be shown, which is fine as backend formats it -->
      </el-table>

      <div class="pagination-block">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next, jumper"
          @current-change="handlePageChange"
          :hide-on-single-page="false"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getAllArticles } from '../api/modules/article'; // Path should be correct
import { ElMessage } from 'element-plus';

const router = useRouter();
const articles = ref([]);
const loading = ref(false);

const pagination = reactive({
  page: 1,
  pageSize: 10, // Default page size
  total: 0
});

const fetchArticles = async () => {
  loading.value = true;
  try {
    // API function now takes page and pageSize
    const response = await getAllArticles(pagination.page, pagination.pageSize);
    // Backend response for paginated articles: { code, message, data: { list: [], total: 0 } }
    if (response.code === 200 && response.data) {
      articles.value = response.data.list;
      pagination.total = response.data.total;
    } else {
      ElMessage.error(response.message || '加载文章列表失败');
      articles.value = [];
      pagination.total = 0;
    }
  } catch (error) {
    console.error('获取文章列表失败:', error);
    ElMessage.error('加载文章列表失败，请稍后重试。');
    articles.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

onMounted(fetchArticles);

const handlePageChange = (newPage) => {
  pagination.page = newPage;
  fetchArticles();
};

const goToWriteArticle = () => {
  router.push({ name: 'ArticleWrite' });
};

const goToManageArticles = () => {
  router.push({ name: 'ArticleManage' });
};

const readArticle = (row) => {
  if (row.id) { // Assuming article object has 'id'
    router.push({ name: 'ArticleRead', params: { articleId: row.id } });
  } else {
    ElMessage.warning('无法打开文章：缺少文章ID。');
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
  /* cursor: pointer; */ /* Row click is handled by el-table @row-click */
}
.el-table :deep(.el-table__row) {
    cursor: pointer;
}
.pagination-block {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
