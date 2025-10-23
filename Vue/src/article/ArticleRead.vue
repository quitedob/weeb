<template>
  <div class="article-read-container" v-if="article">
    <el-card class="article-card">
      <template #header>
        <div class="article-header">
          <h1 class="article-title">{{ article.articleTitle }}</h1>
          <div class="article-meta">
            <div class="meta-item">
              <el-avatar :size="32" :src="article.userAvatar">
                {{ article.userId ? 'U' + article.userId.toString().slice(-2) : 'U' }}
              </el-avatar>
              <span class="author">作者ID: {{ article.userId }}</span>
            </div>
            <div class="meta-stats">
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
            <div class="meta-time">
              <el-icon><Clock /></el-icon>
              {{ formatDate(article.updatedAt) }}
            </div>
          </div>
        </div>
      </template>
      
      <div class="article-content">
        <!-- 显示文章内容 -->
        <div v-if="article.articleContent" class="article-body" v-html="formatContent(article.articleContent)"></div>
        <div v-else class="no-content">
          <el-empty description="暂无文章内容" />
        </div>
        
        <!-- 如果有外部链接，显示链接 -->
        <div v-if="article.articleLink" class="article-link">
          <h3>相关链接</h3>
          <el-link :href="article.articleLink" target="_blank" type="primary">
            {{ article.articleLink }}
            <el-icon><Link /></el-icon>
          </el-link>
        </div>
      </div>
      
      <div class="article-actions">
        <el-button type="primary" @click="like" :loading="isLiking">
          <el-icon><Plus /></el-icon>
          点赞 ({{ article.likesCount || 0 }})
        </el-button>
        <el-button 
          :type="isFavorited ? 'warning' : 'default'" 
          @click="toggleFavorite"
          :loading="isFavoriteLoading"
        >
          <el-icon><Star /></el-icon>
          {{ isFavorited ? '已收藏' : '收藏' }} ({{ article.favoritesCount || 0 }})
        </el-button>
        <el-button @click="shareArticle">
          <el-icon><Share /></el-icon>
          分享
        </el-button>
      </div>
    </el-card>
    
    <!-- 评论区域 -->
    <el-card class="comments-card" v-if="showComments">
      <template #header>
        <div class="comments-header">
          <h3>评论 ({{ commentCount }})</h3>
          <el-button 
            size="small" 
            @click="toggleComments"
            :icon="showComments ? ArrowUp : ArrowDown"
          >
            {{ showComments ? '收起评论' : '展开评论' }}
          </el-button>
        </div>
      </template>
      
      <!-- 发表评论 -->
      <div class="comment-form" v-if="authStore.currentUser">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
          maxlength="1000"
          show-word-limit
        ></el-input>
        <div class="comment-actions">
          <el-button type="primary" @click="submitComment" :loading="isSubmittingComment">
            发表评论
          </el-button>
        </div>
      </div>
      <div v-else class="login-prompt">
        <el-alert title="请先登录后再发表评论" type="info" :closable="false">
          <el-button type="primary" size="small" @click="goToLogin">去登录</el-button>
        </el-alert>
      </div>
      
      <!-- 评论列表 -->
      <div class="comments-list" v-loading="loadingComments">
        <div v-if="comments.length === 0 && !loadingComments" class="no-comments">
          <el-empty description="暂无评论，快来发表第一条评论吧！" />
        </div>
        <div v-else>
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <el-avatar :src="comment.userAvatar" :size="40">
                {{ comment.username ? comment.username.charAt(0).toUpperCase() : 'U' }}
              </el-avatar>
              <div class="comment-info">
                <div class="comment-author">{{ comment.username || '匿名用户' }}</div>
                <div class="comment-time">{{ formatDate(comment.createdAt) }}</div>
              </div>
              <div class="comment-delete" v-if="authStore.currentUser && authStore.currentUser.id === comment.userId">
                <el-button type="danger" size="small" text @click="confirmDeleteComment(comment)">
                  删除
                </el-button>
              </div>
            </div>
            <div class="comment-content">
              {{ comment.content }}
            </div>
          </div>
        </div>
      </div>
    </el-card>
    
    <!-- 评论收起时的按钮 -->
    <div class="comments-toggle" v-if="!showComments && commentCount > 0">
      <el-button @click="toggleComments" :icon="ArrowDown" type="primary" plain>
        查看评论 ({{ commentCount }})
      </el-button>
    </div>
  </div>
  
  <div v-else-if="loading" class="loading-placeholder">
    <el-skeleton :rows="10" animated />
  </div>
  
  <div v-else class="error-placeholder">
    <el-result icon="warning" title="文章不存在" sub-title="抱歉，您访问的文章不存在或已被删除">
      <template #extra>
        <el-button type="primary" @click="goBack">返回</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router'; // Added useRouter
import { getArticleById, increaseReadCount, likeArticle,
         getArticleComments, addArticleComment, deleteArticleComment,
         checkFavoriteStatus as checkFavoriteStatusApi, favoriteArticle, unfavoriteArticle } from '../api/modules/article';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Star, View, Collection, Clock, Link, ArrowUp, ArrowDown, Share } from '@element-plus/icons-vue'; // Icon for like action
import { useAuthStore } from '@/stores/authStore'; // Added useAuthStore
import DOMPurify from 'dompurify'; // Import DOMPurify for HTML sanitization

const route = useRoute();
const router = useRouter(); // Added
const article = ref(null);
const loading = ref(true); // Added loading state
const articleId = route.params.articleId;
const authStore = useAuthStore(); // Added authStore

const newComment = ref(''); // Added for new comment input
const isSubmittingComment = ref(false); // Added for comment submission loading
const comments = ref([]); // Added for comments list
const loadingComments = ref(false); // Added for comments loading state
const commentCount = ref(0); // Added for comment count
const isFavorited = ref(false); // Added for favorite status
const isFavoriteLoading = ref(false); // Added for favorite loading state
const showComments = ref(true); // Added for comments visibility
const isLiking = ref(false); // Added for like loading state

// Helper function to format date
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
};

// Helper function to format article content (simple markdown-like formatting)
const formatContent = (content) => {
  if (!content) return '';

  // 简单的格式化：换行转换为<br>，支持基本的markdown语法
  const formattedContent = content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')  // **粗体**
    .replace(/\*(.*?)\*/g, '<em>$1</em>')              // *斜体*
    .replace(/`(.*?)`/g, '<code>$1</code>')            // `代码`
    .replace(/#{3}\s+(.*)/g, '<h3>$1</h3>')            // ### 三级标题
    .replace(/#{2}\s+(.*)/g, '<h2>$1</h2>')            // ## 二级标题
    .replace(/#{1}\s+(.*)/g, '<h1>$1</h1>');           // # 一级标题

  // 使用DOMPurify对HTML进行安全过滤，防止XSS攻击
  return DOMPurify.sanitize(formattedContent, {
    ALLOWED_TAGS: ['br', 'strong', 'em', 'code', 'h1', 'h2', 'h3'],
    ALLOWED_ATTR: [] // 不允许任何属性
  });
};

const fetchArticle = async () => {
  loading.value = true;
  article.value = null; // Reset article on fetch
  try {
    const response = await getArticleById(articleId);
    if (response && response.data) {
      article.value = response.data;
      // Increase read count only if article is successfully fetched
      await increaseReadCount(articleId);
      // No need to re-fetch for read count, backend handles it.
      // If read count needs to be reflected immediately on this page from this action,
      // the 'increaseReadCount' API might need to return the updated article or count.
      // Or, increment locally if backend guarantees success and eventual consistency:
      // if (article.value) article.value.exposureCount = (article.value.exposureCount || 0) + 1;
    } else {
      ElMessage.error(response.message || '加载文章失败。');
    }
  } catch (error) {
    console.error('获取文章详情失败:', error);
    ElMessage.error('加载文章失败。');
  } finally {
    loading.value = false;
  }
};

const like = async () => {
    if (!article.value) return;
    isLiking.value = true;
    try {
        const response = await likeArticle(articleId); // API returns {code, message, data (can be new like count or updated article)}
        if (response && response.code === 200) {
            ElMessage.success('点赞成功！');
            // To update like count:
            // 1. Re-fetch article (fetchArticle()) - simplest but full refresh
            // 2. Backend returns updated article/like count in 'response.data' - preferred
            // 3. Optimistically update locally
            if (response.data && typeof response.data.likesCount !== 'undefined') {
                article.value.likesCount = response.data.likesCount;
            } else {
                // Optimistic update or simple increment if backend doesn't return new count
                article.value.likesCount = (article.value.likesCount || 0) + 1;
            }
        } else {
            ElMessage.error(response.message || '点赞失败。');
        }
    } catch (error) {
        console.error('点赞失败:', error);
        ElMessage.error('点赞失败。');
    } finally {
        isLiking.value = false;
    }
};

const goBack = () => { // Added goBack function
    router.go(-1);
};

const fetchComments = async () => {
  loadingComments.value = true;
  try {
    const response = await getArticleComments(articleId);
    if (response && response.data) {
      comments.value = response.data;
      commentCount.value = response.data.length;
    } else {
      ElMessage.error(response.message || '加载评论失败。');
    }
  } catch (error) {
    console.error('获取评论失败:', error);
    ElMessage.error('加载评论失败。');
  } finally {
    loadingComments.value = false;
  }
};

const submitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('评论内容不能为空！');
    return;
  }
  if (!authStore.currentUser) {
    ElMessage.error('请先登录后再发表评论。');
    return;
  }

  isSubmittingComment.value = true;
  try {
    const commentData = {
      content: newComment.value.trim()
    };
    const response = await addArticleComment(articleId, commentData);
    if (response && response.code === 200) {
      ElMessage.success('评论发表成功！');
      newComment.value = ''; // Clear input
      await fetchComments(); // Refresh comments
    } else {
      ElMessage.error(response.message || '评论发表失败。');
    }
  } catch (error) {
    console.error('发表评论失败:', error);
    ElMessage.error('评论发表失败。');
  } finally {
    isSubmittingComment.value = false;
  }
};

const confirmDeleteComment = async (comment) => {
  ElMessageBox.confirm(`确定要删除这条评论吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      const response = await deleteArticleComment(articleId, comment.id);
      if (response && response.code === 200) {
        ElMessage.success('评论删除成功！');
        await fetchComments(); // Refresh comments
      } else {
        ElMessage.error(response.message || '评论删除失败。');
      }
    } catch (error) {
      console.error('删除评论失败:', error);
      ElMessage.error('评论删除失败。');
    }
  }).catch(() => {
    // User cancelled
  });
};

const goToLogin = () => {
  router.push({ name: 'Login' });
};

// 检查收藏状态
const checkFavoriteStatus = async () => {
  if (!authStore.currentUser) {
    return; // 用户未登录，不检查收藏状态
  }
  
  try {
    const response = await checkFavoriteStatusApi(articleId);
    if (response && response.data !== undefined) {
      isFavorited.value = response.data;
    }
  } catch (error) {
    console.error('检查收藏状态失败:', error);
  }
};

// 切换收藏状态
const toggleFavorite = async () => {
  if (!authStore.currentUser) {
    ElMessage.warning('请先登录后再收藏文章');
    return;
  }
  
  isFavoriteLoading.value = true;
  try {
    let response;
    if (isFavorited.value) {
      // 取消收藏
      response = await unfavoriteArticle(articleId);
    } else {
      // 收藏
      response = await favoriteArticle(articleId);
    }
    
    if (response && response.code === 200) {
      isFavorited.value = !isFavorited.value;
      ElMessage.success(isFavorited.value ? '收藏成功' : '取消收藏成功');
    } else {
      ElMessage.error(response.message || '操作失败');
    }
  } catch (error) {
    console.error('收藏操作失败:', error);
    ElMessage.error('操作失败，请稍后重试');
  } finally {
    isFavoriteLoading.value = false;
  }
};

const toggleComments = () => {
  showComments.value = !showComments.value;
};

const shareArticle = () => {
  // 构建分享链接
  const shareUrl = `${window.location.origin}/article/${articleId}`;

  // 复制到剪贴板
  if (navigator.clipboard) {
    navigator.clipboard.writeText(shareUrl).then(() => {
      ElMessage.success('分享链接已复制到剪贴板');
    }).catch(err => {
      console.error('复制失败:', err);
      // 降级方案：显示分享对话框
      showShareDialog(shareUrl);
    });
  } else {
    // 降级方案：对于不支持 Clipboard API 的浏览器
    showShareDialog(shareUrl);
  }
};

// 显示分享对话框
const showShareDialog = (url) => {
  const shareData = {
    title: article.value.title || '精彩文章',
    text: article.value.summary || `快来看看这篇精彩的文章：${article.value.title}`,
    url: url
  };

  // 检查是否支持 Web Share API
  if (navigator.share) {
    navigator.share(shareData)
      .then(() => {
        ElMessage.success('分享成功');
      })
      .catch(err => {
        console.log('分享取消或失败:', err);
      });
  } else {
    // 降级方案：创建一个临时的输入框来复制链接
    copyToClipboard(url);
  }
};

// 降级的复制到剪贴板方法
const copyToClipboard = (text) => {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  textArea.style.position = 'fixed';
  textArea.style.left = '-999999px';
  textArea.style.top = '-999999px';
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();

  try {
    document.execCommand('copy');
    ElMessage.success('分享链接已复制到剪贴板');
  } catch (err) {
    console.error('复制失败:', err);
    ElMessage.error('复制失败，请手动复制链接');
  }

  document.body.removeChild(textArea);
};

onMounted(() => {
    if (articleId) {
        fetchArticle();
        fetchComments(); // Fetch comments on mount
        checkFavoriteStatus(); // Check favorite status on mount
    } else {
        ElMessage.error('无效的文章ID。');
        loading.value = false;
        // Optionally redirect: router.push({ name: 'ArticleMain' });
    }
});
</script>

<style scoped>
.article-read-container {
  padding: 20px;
}
.article-card {
  margin-bottom: 20px;
}
.article-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.article-title {
  margin-bottom: 10px;
  color: #303133;
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 0.9em;
  color: #606266;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.author {
  font-weight: 500;
  color: #409EFF;
}
.meta-stats {
  display: flex;
  gap: 15px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
}
.meta-time {
  display: flex;
  align-items: center;
  gap: 5px;
}
.article-content {
  margin-top: 20px;
  font-size: 1rem; /* Standard text size */
  line-height: 1.7; /* Improved readability */
}
.article-body {
  line-height: 1.8;
  color: #303133;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.article-body h1, .article-body h2, .article-body h3 {
  margin: 20px 0 10px 0;
  color: #303133;
}

.article-body h1 {
  font-size: 1.5em;
  border-bottom: 2px solid #409EFF;
  padding-bottom: 5px;
}

.article-body h2 {
  font-size: 1.3em;
  border-bottom: 1px solid #DCDFE6;
  padding-bottom: 3px;
}

.article-body h3 {
  font-size: 1.1em;
}

.article-body strong {
  font-weight: 600;
  color: #303133;
}

.article-body em {
  font-style: italic;
  color: #606266;
}

.article-body code {
  background-color: #f5f5f5;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #e96900;
}

.no-content {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-style: italic;
}

.article-link {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #EBEEF5;
}

.article-link h3 {
  margin-bottom: 10px;
  color: #303133;
}

.article-link .el-link {
  display: flex;
  align-items: center;
  gap: 5px;
}

.article-link .el-link .el-icon {
  font-size: 1em;
}

.article-actions {
    margin-top: 25px;
    border-top: 1px solid #ebeef5;
    padding-top: 20px;
    display: flex; /* For button alignment */
    gap: 10px;
}
.loading-placeholder, .error-placeholder {
  padding: 40px;
  text-align: center;
  color: #606266;
}

/* Comments Section Styles */
.comments-card {
  margin-top: 20px;
  padding-top: 30px;
  border-top: 1px solid #EBEEF5;
}

.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.comments-header h3 {
  margin-bottom: 0; /* Remove default margin */
  color: #303133;
}

.comment-form {
  margin-bottom: 20px;
}

.comment-form .el-textarea {
  margin-bottom: 10px;
}

.comment-form .el-button {
  width: 100%;
}

.login-prompt {
  margin-bottom: 20px;
}

.comments-list .el-empty {
  padding: 40px 0;
}

.comment-item {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px dashed #EBEEF5;
}

.comment-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.comment-header .el-avatar {
  margin-right: 10px;
}

.comment-info {
  flex-grow: 1;
  margin-left: 10px;
}

.comment-author {
  font-weight: 600;
  color: #303133;
}

.comment-time {
  font-size: 0.85em;
  color: #909399;
  margin-top: 3px;
}

.comment-delete {
  margin-left: 15px;
}

.comment-content {
  font-size: 1rem;
  line-height: 1.6;
  color: #606266;
  word-break: break-all;
}

.comments-toggle {
  margin-top: 20px;
  text-align: center;
}
</style>
