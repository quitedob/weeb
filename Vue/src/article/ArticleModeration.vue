<template>
  <div class="article-moderation-container">
    <el-card class="statistics-card">
      <template #header>
        <h2>审核统计</h2>
      </template>
      <el-row :gutter="20" v-if="!loadingStats">
        <el-col :span="6">
          <el-statistic title="待审核" :value="statistics.pendingArticles || 0">
            <template #suffix>篇</template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="已通过" :value="statistics.approvedArticles || 0">
            <template #suffix>篇</template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="今日审核" :value="statistics.todayReviewed || 0">
            <template #suffix>篇</template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="通过率" :value="statistics.approvalRate || 0" :precision="1">
            <template #suffix>%</template>
          </el-statistic>
        </el-col>
      </el-row>
      <el-skeleton :rows="1" animated v-else />
    </el-card>

    <el-card class="articles-card">
      <template #header>
        <div class="card-header">
          <h2>待审核文章</h2>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索文章标题"
              clearable
              style="width: 200px; margin-right: 10px;"
              @keyup.enter="fetchPendingArticles"
            >
              <template #append>
                <el-button @click="fetchPendingArticles"><el-icon><Search /></el-icon></el-button>
              </template>
            </el-input>
            <el-select v-model="statusFilter" placeholder="状态筛选" style="width: 150px; margin-right: 10px;" @change="fetchPendingArticles">
              <el-option label="全部" :value="null" />
              <el-option label="待审核" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已拒绝" :value="2" />
            </el-select>
            <el-button type="primary" @click="fetchPendingArticles"><el-icon><Refresh /></el-icon>刷新</el-button>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>
      <div v-else-if="articles.length === 0" class="empty-state">
        <el-empty description="暂无待审核文章" />
      </div>
      <el-table v-else :data="articles" style="width: 100%">
        <el-table-column prop="articleId" label="ID" width="80" />
        <el-table-column prop="articleTitle" label="标题" min-width="200">
          <template #default="scope">
            <el-link @click="previewArticle(scope.row)" type="primary">
              {{ scope.row.articleTitle }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="120">
          <template #default="scope">
            {{ scope.row.authorName || `用户${scope.row.userId}` }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 0"
              size="small"
              type="success"
              @click="handleApprove(scope.row)"
            >
              通过
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              size="small"
              type="danger"
              @click="handleReject(scope.row)"
            >
              拒绝
            </el-button>
            <el-button
              size="small"
              @click="previewArticle(scope.row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 文章预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="文章预览"
      width="70%"
      :close-on-click-modal="false"
    >
      <div v-if="previewArticle" class="article-preview">
        <h2>{{ previewArticle.articleTitle }}</h2>
        <div class="article-meta">
          <span>作者：{{ previewArticle.authorName || `用户${previewArticle.userId}` }}</span>
          <span>提交时间：{{ formatDate(previewArticle.createdAt) }}</span>
          <el-tag :type="getStatusType(previewArticle.status)">
            {{ getStatusText(previewArticle.status) }}
          </el-tag>
        </div>
        <el-divider />
        <div class="article-content" v-html="previewArticle.articleContent"></div>
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
        <el-button
          v-if="previewArticle && previewArticle.status === 0"
          type="success"
          @click="handleApprove(previewArticle)"
        >
          通过审核
        </el-button>
        <el-button
          v-if="previewArticle && previewArticle.status === 0"
          type="danger"
          @click="handleReject(previewArticle)"
        >
          拒绝审核
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { Search, Refresh } from '@element-plus/icons-vue';
import articleApi from '@/api/modules/article';

const router = useRouter();

// 统计数据
const statistics = ref({});
const loadingStats = ref(true);

// 文章列表
const articles = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);
const searchKeyword = ref('');
const statusFilter = ref(null);

// 预览对话框
const previewDialogVisible = ref(false);
const previewArticle = ref(null);

// 获取审核统计数据
const fetchStatistics = async () => {
  loadingStats.value = true;
  try {
    const response = await articleApi.getModerationStatistics();
    if (response.code === 0 && response.data) {
      statistics.value = response.data;
    }
  } catch (error) {
    console.error('获取审核统计失败:', error);
  } finally {
    loadingStats.value = false;
  }
};

// 获取待审核文章列表
const fetchPendingArticles = async () => {
  loading.value = true;
  try {
    const response = await articleApi.getPendingArticles(
      currentPage.value,
      pageSize.value,
      statusFilter.value,
      searchKeyword.value
    );
    
    if (response.code === 0 && response.data) {
      articles.value = response.data.list || [];
      total.value = response.data.total || 0;
      
      // 获取作者信息
      await fetchAuthorsInfo();
    } else {
      articles.value = [];
      total.value = 0;
    }
  } catch (error) {
    console.error('获取待审核文章失败:', error);
    ElMessage.error('获取文章列表失败');
    articles.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

// 获取作者信息
const fetchAuthorsInfo = async () => {
  const userIds = [...new Set(articles.value.map(a => a.userId))];
  
  for (const userId of userIds) {
    try {
      const response = await articleApi.getUserInformation(userId);
      if (response.code === 0 && response.data) {
        articles.value.forEach(article => {
          if (article.userId === userId) {
            article.authorName = response.data.username || response.data.nickname;
          }
        });
      }
    } catch (error) {
      console.warn(`获取用户${userId}信息失败:`, error);
    }
  }
};

// 处理审核通过
const handleApprove = async (article) => {
  ElMessageBox.confirm(
    `确定要通过文章《${article.articleTitle}》的审核吗？`,
    '确认通过',
    {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'success',
    }
  ).then(async () => {
    const loadingInstance = ElLoading.service({ text: '正在处理...' });
    try {
      const response = await articleApi.approveArticle(article.articleId);
      
      if (response.code === 0) {
        ElMessage.success('审核通过');
        previewDialogVisible.value = false;
        fetchPendingArticles();
        fetchStatistics();
      } else {
        ElMessage.error(response.message || '操作失败');
      }
    } catch (error) {
      console.error('审核通过失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '操作失败');
    } finally {
      loadingInstance.close();
    }
  }).catch(() => {});
};

// 处理审核拒绝
const handleReject = async (article) => {
  ElMessageBox.prompt(
    `确定要拒绝文章《${article.articleTitle}》的审核吗？`,
    '拒绝审核',
    {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入拒绝原因',
      inputType: 'textarea',
      inputValidator: (value) => {
        if (!value || value.trim().length === 0) {
          return '请输入拒绝原因';
        }
        return true;
      },
      type: 'warning',
    }
  ).then(async ({ value }) => {
    const loadingInstance = ElLoading.service({ text: '正在处理...' });
    try {
      const response = await articleApi.rejectArticle(article.articleId, value);
      
      if (response.code === 0) {
        ElMessage.success('已拒绝审核');
        previewDialogVisible.value = false;
        fetchPendingArticles();
        fetchStatistics();
      } else {
        ElMessage.error(response.message || '操作失败');
      }
    } catch (error) {
      console.error('拒绝审核失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '操作失败');
    } finally {
      loadingInstance.close();
    }
  }).catch(() => {});
};

// 预览文章
const previewArticle = (article) => {
  previewArticle.value = article;
  previewDialogVisible.value = true;
};

// 分页处理
const handlePageChange = (page) => {
  currentPage.value = page;
  fetchPendingArticles();
};

const handleSizeChange = (size) => {
  pageSize.value = size;
  currentPage.value = 1;
  fetchPendingArticles();
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString();
};

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  };
  return statusMap[status] || 'info';
};

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝'
  };
  return statusMap[status] || '未知';
};

onMounted(() => {
  fetchStatistics();
  fetchPendingArticles();
});
</script>

<style scoped>
.article-moderation-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.statistics-card {
  margin-bottom: 20px;
}

.articles-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.loading-state,
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.article-preview {
  padding: 20px;
}

.article-preview h2 {
  margin-bottom: 10px;
}

.article-meta {
  display: flex;
  gap: 20px;
  align-items: center;
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
}

.article-content {
  line-height: 1.8;
  font-size: 16px;
}

.article-content :deep(img) {
  max-width: 100%;
  height: auto;
}
</style>
