<template>
  <div class="article-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>我的文章</h2>
          <el-button type="primary" @click="goToWriteArticle"><el-icon><PlusIcon /></el-icon>发布新文章</el-button> <!-- Create new article -->
        </div>
      </template>
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>
      <el-table :data="articles" style="width: 100%" v-else-if="articles.length > 0" empty-text="您还没有发布任何文章。">
        <el-table-column prop="articleTitle" label="标题" min-width="200">
            <template #default="scope">
                <a @click.prevent="readArticle(scope.row.articleId || scope.row.id)" href="#" class="article-title-link">{{ scope.row.articleTitle }}</a>
            </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusTag(scope.row.status).type">
              {{ getStatusTag(scope.row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最后更新" width="180">
          <template #default="scope">
            <span>{{ formatDate(scope.row.updatedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="likesCount" label="点赞数" width="100" sortable />
        <el-table-column prop="exposureCount" label="阅读数" width="100" sortable />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="editArticle(scope.row.articleId || scope.row.id)"><el-icon><EditIcon /></el-icon>编辑</el-button>
            <el-button size="small" type="danger" @click="confirmDelete(scope.row.articleId || scope.row.id)"><el-icon><DeleteIcon /></el-icon>删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- ✅ 改进的空状态UI -->
      <div v-else class="empty-state">
        <el-empty description="您还没有发布任何文章">
          <el-button type="primary" @click="goToWriteArticle">
            <el-icon><PlusIcon /></el-icon>
            发布第一篇文章
          </el-button>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getArticlesByUserId, deleteArticle, getStatusTag } from '@/api/modules/article'; // Use @ alias
import { useAuthStore } from '@/stores/authStore'; // Use @ alias
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus as PlusIcon, Edit as EditIcon, Delete as DeleteIcon } from '@element-plus/icons-vue'; // Icons

const router = useRouter();
const authStore = useAuthStore();
const articles = ref([]);
const loading = ref(true);

// Ensure userId is fetched correctly. Handle cases where currentUser might be null initially.
// Assuming authStore.currentUser.id is the correct path to user's ID
const userId = authStore.currentUser ? authStore.currentUser.id : null;

// Helper function to format date
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
};

const fetchUserArticles = async () => {
  if (!userId) {
    ElMessage.error('无法获取用户信息，请重新登录。');
    loading.value = false;
    // Optionally redirect to login if strict about it: router.push('/login');
    return;
  }
  loading.value = true;
  try {
    const response = await getArticlesByUserId(userId);
    // 后端ApiResponse成功时code为0，不是200
    if (response.code === 0) {
        // ✅ 改进：处理不同的响应结构，确保总是得到数组
        if (Array.isArray(response.data)) {
            articles.value = response.data;
        } else if (response.data && Array.isArray(response.data.list)) {
            articles.value = response.data.list;
        } else if (response.data && Array.isArray(response.data.data)) {
            articles.value = response.data.data;
        } else {
            // 如果data为null或其他格式，设置为空数组
            articles.value = [];
        }
        
        // ✅ 只在真正有错误消息时显示警告
        if (articles.value.length === 0 && response.message && response.message !== '成功') {
            console.log('暂无文章数据');
        }
    } else {
        // 如果返回的code不是0，显示错误
        articles.value = [];
        ElMessage.warning(response.message || '获取文章列表失败。');
    }
  } catch (error) {
    console.error('获取个人文章列表失败:', error);
    ElMessage.error('加载文章列表失败。');
    articles.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
    if (userId) {
        fetchUserArticles();
    } else {
        ElMessage.error('用户未登录，无法查看个人文章。');
        loading.value = false;
        router.replace('/login');
    }
});

const goToWriteArticle = () => {
  router.push({ name: 'ArticleWrite' });
};

const readArticle = (id) => {
  router.push({ name: 'ArticleRead', params: { articleId: id } });
};

const editArticle = (id) => {
  router.push({ name: 'ArticleEdit', params: { articleId: id } });
};

const confirmDelete = (id) => {
  ElMessageBox.confirm('确定要删除这篇文章吗？此操作不可逆。', '警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await handleDelete(id);
    })
    .catch(() => {
      // Optional: ElMessage.info('已取消删除');
    });
};

const handleDelete = async (id) => {
  try {
    const response = await deleteArticle(id);
    // 后端ApiResponse成功时code为0，不是200
    if (response.code === 0) {
        ElMessage.success('文章已删除！');
        fetchUserArticles();
    } else {
        ElMessage.error(response.message || '删除失败。');
    }
  } catch (error) {
    console.error('删除文章失败:', error);
    ElMessage.error('删除失败。');
  }
};
</script>

<style scoped>
.article-manage-container {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.loading-state {
  padding: 20px;
}
.article-title-link {
  color: #409EFF;
  text-decoration: none;
  cursor: pointer;
}
.article-title-link:hover {
  text-decoration: underline;
}
/* ✅ 空状态样式 */
.empty-state {
  padding: 60px 20px;
  text-align: center;
}
</style>
