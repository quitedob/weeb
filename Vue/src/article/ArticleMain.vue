<template>
  <div class="article-main-container">
    <!-- 搜索和筛选区域 -->
    <el-card class="filter-card" shadow="hover">
      <div class="filter-container">
        <div class="search-section">
          <el-input
            v-model="searchQuery"
            placeholder="搜索文章标题或内容..."
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button @click="handleSearch" type="primary">搜索</el-button>
            </template>
          </el-input>
        </div>
        
        <div class="sort-section">
          <span class="sort-label">排序方式：</span>
          <el-select v-model="sortBy" @change="handleSortChange" class="sort-select">
            <el-option label="最新发布" value="created_at" />
            <el-option label="最多阅读" value="exposure_count" />
            <el-option label="最多点赞" value="likes_count" />
            <el-option label="最多收藏" value="favorites_count" />
          </el-select>
          
          <el-select v-model="sortOrder" @change="handleSortChange" class="order-select">
            <el-option label="降序" value="desc" />
            <el-option label="升序" value="asc" />
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- 操作按钮区域 -->
    <div class="actions">
      <el-button type="primary" @click="goToWriteArticle">
        <el-icon><EditPen /></el-icon>发布文章
      </el-button>
      <el-button @click="goToManageArticles">
        <el-icon><Document /></el-icon>管理我的文章
      </el-button>
      <el-button type="info" @click="refreshArticles">
        <el-icon><Refresh /></el-icon>刷新列表
      </el-button>
    </div>
    
    <!-- 文章列表区域 -->
    <el-card class="article-list-card" shadow="hover">
      <template #header>
        <div class="list-header">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="推荐文章" name="recommended">
              <span class="list-title">推荐文章</span>
            </el-tab-pane>
            <el-tab-pane label="最新文章" name="latest">
              <span class="list-title">最新文章</span>
            </el-tab-pane>
            <el-tab-pane label="搜索结果" name="search" v-if="searchQuery">
              <span class="list-title">搜索结果</span>
            </el-tab-pane>
          </el-tabs>
          <span class="list-count" v-if="pagination.total > 0">共 {{ pagination.total }} 篇文章</span>
        </div>
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
              <span class="author">
                <el-icon><User /></el-icon>
                作者ID: {{ article.userId }}
              </span>
              <span class="update-time">
                <el-icon><Clock /></el-icon>
                {{ formatDate(article.updatedAt) }}
              </span>
            </div>
          </div>
          
          <div class="card-content">
            <p class="article-preview">
              {{ getArticlePreview(article.articleContent) }}
            </p>
            <!-- 文章标签 -->
            <div class="article-tags" v-if="article.tags">
              <el-tag 
                v-for="tag in getTagsArray(article.tags)" 
                :key="tag" 
                size="small" 
                type="info"
                class="article-tag"
              >
                {{ tag }}
              </el-tag>
            </div>
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
          <el-empty :description="searchQuery ? '没有找到匹配的文章' : '暂无文章'">
            <el-button type="primary" @click="goToWriteArticle">发布第一篇文章</el-button>
          </el-empty>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-block" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getAllArticles, searchArticles, getRecommendedArticles } from '../api/modules/article';
import { ElMessage } from 'element-plus';
import { 
  Search, 
  EditPen, 
  Document, 
  Refresh, 
  View, 
  Star, 
  Collection,
  User,
  Clock
} from '@element-plus/icons-vue';

const router = useRouter();
const articles = ref([]);
const loading = ref(false);
const searchQuery = ref('');
const sortBy = ref('created_at');
const sortOrder = ref('desc');
const activeTab = ref('latest'); // 新增：控制当前激活的标签页

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

// 获取文章列表
const fetchArticles = async () => {
  loading.value = true;
  try {
    let response;
    
    if (activeTab.value === 'recommended') {
      // 推荐文章
      response = await getRecommendedArticles(pagination.page, pagination.pageSize);
    } else if (activeTab.value === 'search' && searchQuery.value.trim()) {
      // 搜索文章
      response = await searchArticles({
        query: searchQuery.value,
        page: pagination.page,
        pageSize: pagination.pageSize,
        sortBy: sortBy.value,
        sortOrder: sortOrder.value
      });
    } else {
      // 最新文章
      response = await getAllArticles(pagination.page, pagination.pageSize, sortBy.value, sortOrder.value);
    }
    
    if (response && response.data) {
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

// 搜索处理
const handleSearch = () => {
  if (searchQuery.value.trim()) {
    activeTab.value = 'search'; // 搜索时切换到搜索标签页
  } else {
    activeTab.value = 'latest'; // 搜索框为空时切换到最新文章标签页
  }
  pagination.page = 1;
  fetchArticles();
};

// 排序变化处理
const handleSortChange = () => {
  pagination.page = 1;
  fetchArticles();
};

// 分页大小变化处理
const handleSizeChange = (newSize) => {
  pagination.pageSize = newSize;
  pagination.page = 1;
  fetchArticles();
};

// 分页变化处理
const handlePageChange = (newPage) => {
  pagination.page = newPage;
  fetchArticles();
};

// 刷新文章列表
const refreshArticles = () => {
  searchQuery.value = '';
  if (activeTab.value === 'search') {
    activeTab.value = 'latest'; // 如果当前是搜索标签页，切换到最新文章标签页
  }
  pagination.page = 1;
  fetchArticles();
};

// 跳转到写文章页面
const goToWriteArticle = () => {
  router.push({ name: 'ArticleWrite' });
};

// 跳转到管理文章页面
const goToManageArticles = () => {
  router.push({ name: 'ArticleManage' });
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
};

// 获取文章预览
const getArticlePreview = (content) => {
  if (!content) return '暂无内容预览...';
  // 移除HTML标签并截取前150个字符
  const plainText = content.replace(/<[^>]*>/g, '').replace(/\n/g, ' ');
  return plainText.length > 150 ? plainText.substring(0, 150) + '...' : plainText;
};

// 将标签字符串转换为数组
const getTagsArray = (tags) => {
  if (!tags) return [];
  return tags.split(',').map(tag => tag.trim()).filter(tag => tag);
};

// 阅读文章
const readArticle = (article) => {
  if (article.id) {
    router.push({ name: 'ArticleRead', params: { articleId: article.id } });
  } else {
    ElMessage.warning('无法打开文章：缺少文章ID。');
  }
};

// 标签页变化处理
const handleTabChange = (name) => {
  activeTab.value = name;
  pagination.page = 1; // 切换标签页时重置页码
  fetchArticles();
};

onMounted(() => {
  fetchArticles();
});
</script>

<style scoped>
.article-main-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 搜索和筛选区域 */
.filter-card {
  margin-bottom: 20px;
}

.filter-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: center;
}

.search-section {
  flex: 1;
  min-width: 300px;
}

.search-input {
  width: 100%;
}

.sort-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-label {
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

.sort-select, .order-select {
  width: 120px;
}

/* 操作按钮区域 */
.actions {
  margin-bottom: 20px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 文章列表区域 */
.article-list-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-title {
  font-weight: 600;
  font-size: 16px;
}

.list-count {
  color: #909399;
  font-size: 14px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
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
  height: 100%;
  display: flex;
  flex-direction: column;
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

.author, .update-time {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-content {
  margin-bottom: 20px;
  flex: 1;
}

.article-preview {
  color: #606266;
  line-height: 1.6;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.article-tag {
  margin: 0;
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
  
  .filter-container {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-section {
    min-width: auto;
  }
  
  .sort-section {
    justify-content: space-between;
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
