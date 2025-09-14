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
        <p>文章内容链接 (用于演示):</p>
        <!-- It's safer to use a regular <a> tag for external links if articleLink is an absolute URL -->
        <!-- If it's an internal route, <router-link> would be appropriate -->
        <a :href="article.articleLink" target="_blank" rel="noopener noreferrer">{{ article.articleLink }}</a>
        <!-- Placeholder for actual article content if not just a link -->
        <!-- <div v-html="article.content"></div> -->
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

const fetchArticle = async () => {
  loading.value = true;
  article.value = null; // Reset article on fetch
  try {
    const response = await getArticleById(articleId);
    if (response.code === 200 && response.data) {
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
        if (response.code === 200) {
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
.article-content a { /* Style for the article link */
  color: #409EFF;
  text-decoration: none;
}
.article-content a:hover {
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
