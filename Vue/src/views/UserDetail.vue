<template>
  <div class="user-detail-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="skeleton-loader">
        <div class="skeleton-item" v-for="i in 8" :key="i">
          <div class="skeleton-line"></div>
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <AppleCard class="error-card">
        <div class="error-content">
          <div class="error-icon">⚠️</div>
          <h3 class="error-title">加载失败</h3>
          <p class="error-message">{{ error }}</p>
          <AppleButton
            @click="loadUserData"
            type="primary"
            size="small"
          >
            重试
          </AppleButton>
        </div>
      </AppleCard>
    </div>

    <!-- 用户信息 -->
    <div v-else-if="userProfile" class="user-profile">
      <!-- 用户基本信息卡片 -->
      <AppleCard class="user-info-card">
        <div class="user-header">
          <div class="avatar-section">
            <div class="user-avatar">
              <img
                :src="userProfile.user?.avatar || 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'"
                :alt="userProfile.user?.username"
                class="avatar-img"
              />
            </div>
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
              <div
                v-if="userProfile.user?.type"
                :class="['user-type-tag', getUserTypeClass(userProfile.user.type)]"
              >
                {{ getUserTypeText(userProfile.user.type) }}
              </div>
              <span class="join-date" v-if="userProfile.user?.registrationDate">
                加入时间：{{ formatDate(userProfile.user.registrationDate) }}
              </span>
            </div>
          </div>

          <div class="user-actions" v-if="!isCurrentUser">
            <AppleButton
              type="primary"
              @click="toggleFollow"
              :loading="followLoading"
            >
              {{ isFollowing ? '取消关注' : '关注' }}
            </AppleButton>
            <AppleButton @click="sendMessage" :loading="chatLoading">发消息</AppleButton>
          </div>
        </div>
      </AppleCard>

      <!-- 用户统计信息 -->
      <AppleCard class="stats-card" v-if="userProfile.userStats">
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
      </AppleCard>

      <!-- 用户文章列表 -->
      <AppleCard class="articles-card">
        <template #header>
          <div class="card-header">
            <span>发布的文章</span>
            <AppleButton @click="refreshArticles" :loading="articlesLoading" size="small">
              刷新
            </AppleButton>
          </div>
        </template>

        <div v-if="articlesLoading" class="articles-loading">
          <div class="skeleton-loader">
            <div class="skeleton-item" v-for="i in 3" :key="i">
              <div class="skeleton-line"></div>
            </div>
          </div>
        </div>

        <div v-else-if="articles.length === 0" class="no-articles">
          <div class="empty-state">
            <div class="empty-icon">📝</div>
            <p class="empty-text">暂无发布的文章</p>
          </div>
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
                <span class="stat-icon">👁️</span>
                {{ article.exposureCount || 0 }}
              </span>
              <span class="article-stats">
                <span class="stat-icon">⭐</span>
                {{ article.likesCount || 0 }}
              </span>
              <span class="article-date">
                {{ formatDate(article.updatedAt || article.createdAt) }}
              </span>
            </div>
          </div>
        </div>
      </AppleCard>
    </div>
  </div>
</template>

<script setup>
import appleMessage from '@/utils/appleMessage';
import { ref, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import AppleButton from '@/components/common/AppleButton.vue';
import AppleCard from '@/components/common/AppleCard.vue';
import userApi from '@/api/modules/user';
import chatApi from '@/api/modules/chat';
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
const chatLoading = ref(false);
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
    const response = await userApi.getUserById(userId.value);
    if (response.code === 0 && response.data) {
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
    if (!isCurrentUser.value && authStore.isLoggedIn) {
      try {
        const status = await userApi.checkFollowStatus(userId.value);
        isFollowing.value = status.data === true;
      } catch (err) {
        appleMessage.error('获取关注状态失败，请刷新重试');
      }
    }
  }
};

// 加载用户文章
const loadUserArticles = async () => {
  articlesLoading.value = true;
  try {
    const response = await getArticlesByUserId(userId.value);
    if (response.code === 0 && response.data) {
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
    // 使用原生 alert 替代 ElMessage
    appleMessage.warning('请先登录');
    router.push('/login');
    return;
  }

  followLoading.value = true;
  try {
    const response = await (isFollowing.value
      ? userApi.unfollowUser(userId.value)
      : userApi.followUser(userId.value));
    if (response.code === 0) {
      isFollowing.value = !isFollowing.value;
      appleMessage.success(isFollowing.value ? '关注成功' : '取消关注成功');
      if (userProfile.value.userStats) {
        userProfile.value.userStats.fansCount = Math.max(0,
          (userProfile.value.userStats.fansCount || 0) + (isFollowing.value ? 1 : -1));
      }
    } else {
      appleMessage.error(response.message || '操作失败');
    }
  } catch (err) {
    console.error('关注操作失败:', err);
    appleMessage.error('操作失败，请稍后重试');
  } finally {
    followLoading.value = false;
  }
};

// 发送消息
const sendMessage = async () => {
  if (!authStore.currentUser) {
    appleMessage.warning('请先登录');
    router.push('/login');
    return;
  }

  if (chatLoading.value) return;
  chatLoading.value = true;
  try {
    const response = await chatApi.createChat({ targetId: String(userId.value) });
    if (response.code !== 0 || !response.data?.sharedChatId) throw new Error(response.message || '创建聊天失败');
    router.push({ name: 'SpecificChat', params: { type: 'private', id: String(response.data.sharedChatId) } });
  } catch (error) {
    appleMessage.error(error.message || '创建聊天失败，请稍后重试');
  } finally {
    chatLoading.value = false;
  }
};

// 获取用户类型标签样式
const getUserTypeClass = (type) => {
  switch (type?.toUpperCase()) {
    case 'ADMIN':
      return 'user-type-admin';
    case 'VIP':
      return 'user-type-vip';
    default:
      return 'user-type-normal';
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

watch(userId, loadUserData, { immediate: true });
</script>

<style scoped>
.user-detail-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  background: var(--apple-bg-secondary);
  min-height: 100vh;
}

/* 加载骨架屏 */
.loading-container, .error-container {
  padding: 40px;
}

.skeleton-loader {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, var(--apple-bg-tertiary) 25%, var(--apple-bg-quaternary) 50%, var(--apple-bg-tertiary) 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
}

.skeleton-line:nth-child(1) { width: 60%; }
.skeleton-line:nth-child(2) { width: 80%; }
.skeleton-line:nth-child(3) { width: 45%; }

@keyframes loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 错误状态 */
.error-card {
  border-left: 4px solid var(--apple-red);
}

.error-content {
  text-align: center;
  padding: 20px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-title {
  margin: 0 0 8px 0;
  color: var(--apple-text-primary);
  font-size: 18px;
  font-weight: 600;
}

.error-message {
  margin: 0 0 20px 0;
  color: var(--apple-text-secondary);
}

/* 用户信息 */
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

.user-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid var(--apple-bg-quaternary);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-basic-info {
  flex: 1;
}

.username {
  margin: 0 0 10px 0;
  color: var(--apple-text-primary);
  font-size: 28px;
  font-weight: 700;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.nickname {
  margin: 0 0 10px 0;
  color: var(--apple-text-secondary);
  font-size: 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.bio {
  margin: 0 0 15px 0;
  color: var(--apple-text-tertiary);
  line-height: 1.6;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.user-type-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  color: white;
}

.user-type-admin {
  background: var(--apple-red);
}

.user-type-vip {
  background: var(--apple-orange);
}

.user-type-normal {
  background: var(--apple-blue);
}

.join-date {
  color: var(--apple-text-tertiary);
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
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
  padding: 20px;
  border-radius: 12px;
  background: var(--apple-bg-tertiary);
  transition: all 0.2s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-number {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-blue);
  margin-bottom: 5px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.stat-label {
  color: var(--apple-text-secondary);
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
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

.empty-state {
  text-align: center;
  padding: 40px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  margin: 0;
  color: var(--apple-text-tertiary);
  font-size: 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.article-item {
  padding: 20px;
  border: 1px solid var(--apple-bg-quaternary);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--apple-bg-primary);
}

.article-item:hover {
  border-color: var(--apple-blue);
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.1);
  transform: translateY(-2px);
}

.article-title {
  margin: 0 0 10px 0;
  color: var(--apple-text-primary);
  font-size: 16px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  color: var(--apple-text-tertiary);
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.article-stats {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-icon {
  font-size: 16px;
}

.article-date {
  margin-left: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-detail-container {
    padding: 16px;
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

  .article-item {
    padding: 16px;
  }

  .username {
    font-size: 24px;
  }

  .user-avatar {
    width: 100px;
    height: 100px;
  }
}
</style>