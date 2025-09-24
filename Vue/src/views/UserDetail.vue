<template>
  <div class="user-detail-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <el-alert :title="error" type="error" :closable="false">
        <template #default>
          <p>{{ error }}</p>
          <el-button @click="loadUserData" type="primary" size="small">重试</el-button>
        </template>
      </el-alert>
    </div>

    <!-- 用户信息 -->
    <div v-else-if="userProfile" class="user-profile">
      <!-- 用户基本信息卡片 -->
      <el-card class="user-info-card">
        <div class="user-header">
          <div class="avatar-section">
            <el-avatar 
              :size="120" 
              :src="userProfile.user?.avatar" 
              :alt="userProfile.user?.username"
            >
              <img src="https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png" />
            </el-avatar>
          </div>
          
          <div class="user-basic-info">
            <h2 class="username">{{ userProfile.user?.username || '未知用户' }}</h2>
            <p class="nickname" v-if="userProfile.user?.nickname">
              {{ userProfile.user.nickname }}
            </p>
            <p class="bio" v-if="userProfile.user?.bio">
              {{ userProfile.user.bio }}
            </p>
            <div class="user-meta">
              <el-tag v-if="userProfile.user?.type" :type="getUserTypeTagType(userProfile.user.type)">
                {{ getUserTypeText(userProfile.user.type) }}
              </el-tag>
              <span class="join-date" v-if="userProfile.user?.registrationDate">
                加入时间：{{ formatDate(userProfile.user.registrationDate) }}
              </span>
            </div>
          </div>

          <div class="user-actions" v-if="!isCurrentUser">
            <el-button 
              type="primary" 
              @click="toggleFollow"
              :loading="followLoading"
            >
              {{ isFollowing ? '取消关注' : '关注' }}
            </el-button>
            <el-button @click="sendMessage">发消息</el-button>
          </div>
        </div>
      </el-card>

      <!-- 用户统计信息 -->
      <el-card class="stats-card" v-if="userProfile.userStats">
        <template #header>
          <span>用户统计</span>
        </template>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-number">{{ userProfile.userStats.fansCount || 0 }}</div>
            <div class="stat-label">粉丝</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ userProfile.userStats.totalLikes || 0 }}</div>
            <div class="stat-label">获赞</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ userProfile.userStats.totalFavorites || 0 }}</div>
            <div class="stat-label">收藏</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ userProfile.userStats.totalArticleExposure || 0 }}</div>
            <div class="stat-label">阅读</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ userProfile.userStats.websiteCoins || 0 }}</div>
            <div class="stat-label">金币</div>
          </div>
        </div>
      </el-card>

      <!-- 用户文章列表 -->
      <el-card class="articles-card">
        <template #header>
          <div class="card-header">
            <span>发布的文章</span>
            <el-button @click="refreshArticles" :loading="articlesLoading" size="small">
              刷新
            </el-button>
          </div>
        </template>
        
        <div v-if="articlesLoading" class="articles-loading">
          <el-skeleton :rows="3" animated />
        </div>
        
        <div v-else-if="articles.length === 0" class="no-articles">
          <el-empty description="暂无发布的文章" />
        </div>
        
        <div v-else class="articles-list">
          <div 
            v-for="article in articles" 
            :key="article.id" 
            class="article-item"
            @click="viewArticle(article.id)"
          >
            <h4 class="article-title">{{ article.articleTitle }}</h4>
            <div class="article-meta">
              <span class="article-stats">
                <el-icon><View /></el-icon>
                {{ article.exposureCount || 0 }}
              </span>
              <span class="article-stats">
                <el-icon><Star /></el-icon>
                {{ article.likesCount || 0 }}
              </span>
              <span class="article-date">
                {{ formatDate(article.updatedAt || article.createdAt) }}
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { ElMessage } from 'element-plus';
import { View, Star } from '@element-plus/icons-vue';
import userApi from '@/api/modules/user';
import { getArticlesByUserId } from '@/api/modules/article';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(true);
const error = ref('');
const userProfile = ref(null);
const articles = ref([]);
const articlesLoading = ref(false);
const followLoading = ref(false);
const isFollowing = ref(false);

// 获取路由参数中的用户ID
const userId = computed(() => route.params.userId);

// 判断是否是当前用户
const isCurrentUser = computed(() => {
  return authStore.currentUser && 
         authStore.currentUser.id && 
         authStore.currentUser.id.toString() === userId.value;
});

// 加载用户数据
const loadUserData = async () => {
  if (!userId.value) {
    error.value = '用户ID无效';
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = '';

  try {
    // 获取用户完整信息（包含统计数据）
    const response = await userApi.getUserInfoById(userId.value);
    if (response.code === 200 && response.data) {
      userProfile.value = response.data;
      // 如果返回的是User对象而不是UserWithStats，需要适配
      if (!userProfile.value.user && userProfile.value.username) {
        userProfile.value = {
          user: response.data,
          userStats: null
        };
      }
    } else {
      error.value = response.message || '获取用户信息失败';
    }
  } catch (err) {
    console.error('获取用户信息失败:', err);
    error.value = '获取用户信息失败，请稍后重试';
  } finally {
    loading.value = false;
  }

  // 加载用户文章
  if (!error.value) {
    loadUserArticles();
  }
};

// 加载用户文章
const loadUserArticles = async () => {
  articlesLoading.value = true;
  try {
    const response = await getArticlesByUserId(userId.value);
    if (response.code === 200 && response.data) {
      articles.value = Array.isArray(response.data) ? response.data : [];
    } else {
      console.warn('获取用户文章失败:', response.message);
      articles.value = [];
    }
  } catch (err) {
    console.error('获取用户文章失败:', err);
    articles.value = [];
  } finally {
    articlesLoading.value = false;
  }
};

// 刷新文章列表
const refreshArticles = () => {
  loadUserArticles();
};

// 查看文章
const viewArticle = (articleId) => {
  router.push({ name: 'ArticleRead', params: { articleId } });
};

// 切换关注状态
const toggleFollow = async () => {
  if (!authStore.currentUser) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }

  followLoading.value = true;
  try {
    const endpoint = isFollowing.value ? 'unfollow' : 'follow';
    const response = await fetch(`/api/user/${userId.value}/${endpoint}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authStore.accessToken}`,
        'Content-Type': 'application/json'
      }
    });
    
    const result = await response.json();
    if (result.code === 200) {
      isFollowing.value = !isFollowing.value;
      ElMessage.success(result.message || (isFollowing.value ? '关注成功' : '取消关注成功'));
      // 更新粉丝数
      if (userProfile.value.userStats) {
        userProfile.value.userStats.fansCount += isFollowing.value ? 1 : -1;
      }
    } else {
      ElMessage.error(result.message || '操作失败');
    }
  } catch (err) {
    console.error('关注操作失败:', err);
    ElMessage.error('操作失败，请稍后重试');
  } finally {
    followLoading.value = false;
  }
};

// 发送消息
const sendMessage = () => {
  if (!authStore.currentUser) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }
  
  // 跳转到聊天页面
  router.push({ 
    name: 'ChatPage', 
    query: { userId: userId.value } 
  });
};

// 获取用户类型标签类型
const getUserTypeTagType = (type) => {
  switch (type?.toUpperCase()) {
    case 'ADMIN':
      return 'danger';
    case 'VIP':
      return 'warning';
    default:
      return 'info';
  }
};

// 获取用户类型文本
const getUserTypeText = (type) => {
  switch (type?.toUpperCase()) {
    case 'ADMIN':
      return '管理员';
    case 'VIP':
      return 'VIP用户';
    default:
      return '普通用户';
  }
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN');
};

onMounted(() => {
  loadUserData();
});
</script>

<style scoped>
.user-detail-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.loading-container, .error-container {
  padding: 40px;
}

.user-profile {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-info-card {
  margin-bottom: 20px;
}

.user-header {
  display: flex;
  gap: 30px;
  align-items: flex-start;
}

.avatar-section {
  flex-shrink: 0;
}

.user-basic-info {
  flex: 1;
}

.username {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 28px;
  font-weight: 600;
}

.nickname {
  margin: 0 0 10px 0;
  color: #606266;
  font-size: 16px;
}

.bio {
  margin: 0 0 15px 0;
  color: #909399;
  line-height: 1.6;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.join-date {
  color: #909399;
  font-size: 14px;
}

.user-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stats-card {
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
  text-align: center;
}

.stat-item {
  padding: 15px;
  border-radius: 8px;
  background: #f8f9fa;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.articles-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.articles-loading {
  padding: 20px;
}

.no-articles {
  padding: 40px;
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.article-item {
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.article-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.article-title {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  color: #909399;
  font-size: 14px;
}

.article-stats {
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-date {
  margin-left: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-detail-container {
    padding: 10px;
  }
  
  .user-header {
    flex-direction: column;
    text-align: center;
    gap: 20px;
  }
  
  .user-actions {
    flex-direction: row;
    justify-content: center;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .article-meta {
    flex-wrap: wrap;
  }
}
</style>