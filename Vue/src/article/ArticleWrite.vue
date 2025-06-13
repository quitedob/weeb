<template>
  <div class="article-write-container">
    <el-card>
      <template #header>
        <h2>发布新文章</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="articleFormRef" label-width="120px" @submit.prevent="submitArticle">
        <el-form-item label="文章标题" prop="articleTitle">
          <el-input v-model="form.articleTitle" placeholder="请输入文章标题"></el-input>
        </el-form-item>
        <el-form-item label="文章链接" prop="articleLink">
          <el-input v-model="form.articleLink" placeholder="请输入内容的链接 (例如 https://...)"></el-input>
        </el-form-item>
        <!-- Placeholder for a rich text editor in the future -->
        <!-- <el-form-item label="文章内容" prop="articleContent">
          <RichTextEditor v-model="form.articleContent" />
        </el-form-item> -->
        <el-form-item>
          <el-button type="primary" @click="submitArticle" :loading="isSubmitting">
            {{ isSubmitting ? '发布中...' : '发布' }}
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { createArticle } from '@/api/modules/article'; // Using @ alias for api
import { useAuthStore } from '@/stores/authStore';    // Using @ alias for stores
import { ElMessage } from 'element-plus';

const router = useRouter();
const authStore = useAuthStore();
const articleFormRef = ref(null); // Ref for the form instance
const isSubmitting = ref(false);  // Loading state for submit button

const form = reactive({
  articleTitle: '',
  articleLink: '',
  userId: authStore.currentUser ? authStore.currentUser.id : null, // Get current user's ID (assuming currentUser.id)
  // articleContent: '', // For rich text editor content
});

// Basic validation rules
const rules = reactive({
  articleTitle: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 3, max: 100, message: '长度在 3 到 100 个字符', trigger: 'blur' }
  ],
  articleLink: [
    { required: true, message: '请输入文章链接', trigger: 'blur' },
    { type: 'url', message: '请输入有效的链接地址', trigger: ['blur', 'change'] }
  ],
  // articleContent: [
  //   { required: true, message: '请输入文章内容', trigger: 'blur' }
  // ]
});

// Submit article
const submitArticle = async () => {
  if (!articleFormRef.value) return;
  if (!authStore.currentUser || !authStore.currentUser.id) { // Assuming currentUser.id
      ElMessage.error('用户未登录或无法获取用户信息，请重新登录。');
      router.push('/login'); // Redirect to login if user info is missing
      return;
  }
  // Update userId in form just before submit, in case it wasn't set initially
  form.userId = authStore.currentUser.id; // Assuming currentUser.id

  await articleFormRef.value.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {
        // The backend expects 'userId' in the articleData payload.
        // The 'createArticle' in api/modules/article.js takes 'articleData'.
        // The form object already includes userId.
        const response = await createArticle(form);
        if (response.code === 200) {
            ElMessage.success('文章发布成功！');
            router.push({ name: 'ArticleMain' }); // Navigate after successful submission
        } else {
            ElMessage.error(response.message || '发布失败，请稍后重试。');
        }
      } catch (error) {
        console.error('发布文章失败:', error);
        ElMessage.error('发布失败，请稍后重试。');
      } finally {
        isSubmitting.value = false;
      }
    } else {
      ElMessage.warning('请检查表单输入是否正确！');
      return false;
    }
  });
};

// Reset form
const resetForm = () => {
  if (articleFormRef.value) {
    articleFormRef.value.resetFields();
  }
  // Re-initialize form if needed, especially if userId might have become null
  form.articleTitle = '';
  form.articleLink = '';
  // form.articleContent = '';
  form.userId = authStore.currentUser ? authStore.currentUser.id : null; // Assuming currentUser.id
};

// Check user login status on component setup - if no user, redirect.
// This is an additional local check, route guards are primary.
if (!authStore.currentUser) {
  ElMessage.warning('请先登录再发布文章。');
  router.replace('/login'); // Use replace to not add to history
}

</script>

<style scoped>
.article-write-container {
  padding: 20px;
  max-width: 800px; /* Limit width for better form readability */
  margin: 0 auto; /* Center the card */
}
.el-card {
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); /* Add some shadow for better appearance */
}
</style>
