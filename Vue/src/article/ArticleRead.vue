<template>
  <div class="article-read-container" v-if="article">
    <el-card>
      <template #header>
        <h1>{{ article.articleTitle }}</h1>
        <div class="article-meta">
          <span>作者ID: {{ article.userId }}</span>
          <span>阅读: {{ article.exposureCount }}</span>
          <span>点赞: {{ article.likesCount }}</span>
          <span>更新于: {{ formatDate(article.updatedAt) }}</span> <!-- Added formatDate -->
        </div>
      </template>
      <div class="article-content">
        <!-- 显示文章内容 -->
        <div v-if="article.articleContent" class="article-body" v-html="formatContent(article.articleContent)"></div>
        <div v-else class="no-content">
          <p>暂无文章内容</p>
        </div>
        
        <!-- 如果有外部链接，显示链接 -->
        <div v-if="article.articleLink" class="article-link">
          <p>相关链接:</p>
          <a :href="article.articleLink" target="_blank" rel="noopener noreferrer">{{ article.articleLink }}</a>
        </div>
      </div>
       <div class="article-actions">
        <el-button type="primary" @click="like"><el-icon><Plus /></el-icon>点赞</el-button> <!-- Like button with icon -->
      </div>
    </el-card>
  </div>
  <div v-else-if="loading" class="loading-placeholder"> <!-- Added loading state -->
    <p>正在加载文章...</p>
    <!-- Optional: <el-skeleton :rows="5" animated /> -->
  </div>
  <div v-else class="error-placeholder"> <!-- Added error/not found state -->
    <p>加载文章失败或文章不存在。</p>
    <el-button @click="goBack">返回</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router'; // Added useRouter
import { getArticleById, increaseReadCount, likeArticle } from '../api/modules/article';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue'; // Icon for like action

const route = useRoute();
const router = useRouter(); // Added
const article = ref(null);
const loading = ref(true); // Added loading state
const articleId = route.params.articleId;

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
  return content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')  // **粗体**
    .replace(/\*(.*?)\*/g, '<em>$1</em>')              // *斜体*
    .replace(/`(.*?)`/g, '<code>$1</code>')            // `代码`
    .replace(/#{3}\s+(.*)/g, '<h3>$1</h3>')            // ### 三级标题
    .replace(/#{2}\s+(.*)/g, '<h2>$1</h2>')            // ## 二级标题
    .replace(/#{1}\s+(.*)/g, '<h1>$1</h1>');           // # 一级标题
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
    try {
        const response = await likeArticle(articleId); // API returns {code, message, data (can be new like count or updated article)}
        if (response) {
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
    }
};

const goBack = () => { // Added goBack function
    router.go(-1);
};

onMounted(() => {
    if (articleId) {
        fetchArticle();
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
.article-meta {
  font-size: 0.85em; /* Slightly smaller */
  color: #909399;
  margin-top: 5px; /* Add some space from title */
  display: flex; /* Better alignment for multiple items */
  flex-wrap: wrap; /* Allow wrapping on small screens */
  gap: 15px; /* Space between items */
}
.article-meta span {
  /* margin-right: 15px; */ /* Replaced by gap */
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

.article-link p {
  margin-bottom: 10px;
  color: #606266;
  font-weight: 500;
}

.article-link a {
  color: #409EFF;
  text-decoration: none;
  word-break: break-all;
}

.article-link a:hover {
  text-decoration: underline;
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
</style>
