<template>
  <div class="article-main-container">
    <div class="actions">
      <el-button type="primary" @click="goToWriteArticle">发布文章</el-button>
      <el-button @click="goToManageArticles">管理我的文章</el-button>
      <el-button type="info" @click="refreshArticles">刷新列表</el-button>
    </div>
    <el-card class="article-list-card">
      <template #header>
        <span>最新文章</span>
      </template>
      <!-- 文章卡片列表 -->
      <div class="article-grid" v-loading="loading">
        <div 
          v-for="article in articles" 
          :key="article.id" 
          class="article-card"
          @click="readArticle(article)"
        >
          <div class="card-header">
            <h3 class="article-title">{{ article.articleTitle }}</h3>
            <div class="article-meta">
              <span class="author">作者ID: {{ article.userId }}</span>
              <span class="update-time">{{ formatDate(article.updatedAt) }}</span>
            </div>
          </div>
          
          <div class="card-content">
            <p class="article-preview">
              {{ getArticlePreview(article.articleContent) }}
            </p>
          </div>
          
          <div class="card-footer">
            <div class="stats">
              <span class="stat-item">
                <el-icon><View /></el-icon>
                {{ article.exposureCount || 0 }}
              </span>
              <span class="stat-item">
                <el-icon><Star /></el-icon>
                {{ article.likesCount || 0 }}
              </span>
              <span class="stat-item">
                <el-icon><Collection /></el-icon>
                {{ article.favoritesCount || 0 }}
              </span>
            </div>
            <el-button type="primary" size="small" @click.stop="readArticle(article)">
              阅读全文
            </el-button>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-if="!loading && articles.length === 0" class="empty-state">
          <el-empty description="暂无文章">
            <el-button type="primary" @click="goToWriteArticle">发布第一篇文章</el-button>
          </el-empty>
        </div>
      </div>

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
import { View, Star, Collection } from '@element-plus/icons-vue';

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
    // Backend response for paginated articles: { code: 0, message, data: { list: [], total: 0 } }
    if (response && response.data) {
      // 兼容新旧格式：优先使用 list/total，回退到 articles/totalCount
      articles.value = response.data.list || response.data.articles || [];
      pagination.total = response.data.total || response.data.totalCount || 0;
    } else {
      ElMessage.error(response?.message || '加载文章列表失败');
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

const refreshArticles = () => {
  pagination.page = 1;
  fetchArticles();
};

const goToWriteArticle = () => {
  router.push({ name: 'ArticleWrite' });
};

const goToManageArticles = () => {
  router.push({ name: 'ArticleManage' });
};

// Helper function to format date
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString();
};

// Helper function to get article preview
const getArticlePreview = (content) => {
  if (!content) return '暂无内容预览...';
  // 移除HTML标签并截取前100个字符
  const plainText = content.replace(/<[^>]*>/g, '').replace(/\n/g, ' ');
  return plainText.length > 100 ? plainText.substring(0, 100) + '...' : plainText;
};

const readArticle = (article) => {
  if (article.id) {
    router.push({ name: 'ArticleRead', params: { articleId: article.id } });
  } else {
    ElMessage.warning('无法打开文章：缺少文章ID。');
  }
};
</script>

<style scoped>
.article-main-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.actions {
  margin-bottom: 30px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.article-list-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

.article-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.15);
  border-color: #409EFF;
}

.card-header {
  margin-bottom: 15px;
}

.article-title {
  font-size: 1.2em;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85em;
  color: #909399;
}

.author {
  font-weight: 500;
}

.update-time {
  color: #C0C4CC;
}

.card-content {
  margin-bottom: 20px;
}

.article-preview {
  color: #606266;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #f5f5f5;
}

.stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.9em;
  color: #909399;
}

.stat-item .el-icon {
  font-size: 16px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 20px;
}

.pagination-block {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .article-main-container {
    padding: 15px;
  }
  
  .article-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .actions {
    flex-direction: column;
  }
  
  .article-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .card-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .stats {
    justify-content: center;
  }
}
</style>
