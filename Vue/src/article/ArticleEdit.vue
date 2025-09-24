<template>
  <div class="article-edit-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>编辑文章</h2>
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
            <el-button type="primary" @click="saveArticle" :loading="isSaving">
              {{ isSaving ? '保存中...' : '保存' }}
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="articleFormRef" label-width="120px">
        <el-form-item label="文章标题" prop="articleTitle">
          <el-input v-model="form.articleTitle" placeholder="请输入文章标题" maxlength="200"></el-input>
        </el-form-item>

        <el-form-item label="文章链接" prop="articleLink">
          <el-input v-model="form.articleLink" placeholder="请输入文章链接 (可选)" clearable></el-input>
          <div class="form-help">如果没有文章链接，可以留空</div>
        </el-form-item>

        <el-form-item label="文章状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="0">草稿</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveArticle" :loading="isSaving">
            {{ isSaving ? '保存中...' : '保存文章' }}
          </el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <el-alert :title="error" type="error" :closable="false"></el-alert>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getArticleById, updateArticle } from '@/api/modules/article';
import { useAuthStore } from '@/stores/authStore';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(true);
const error = ref('');
const isSaving = ref(false);
const articleFormRef = ref(null);

// 文章ID从路由参数获取
const articleId = route.params.articleId;

const form = reactive({
  articleTitle: '',
  articleLink: '',
  status: 1
});

const rules = {
  articleTitle: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 1, max: 200, message: '标题长度在1到200个字符之间', trigger: 'blur' }
  ]
};

// 加载文章数据
const loadArticle = async () => {
  if (!articleId) {
    error.value = '未提供文章ID';
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = '';

  try {
    const response = await getArticleById(articleId);
    if (response.code === 200 && response.data) {
      const article = response.data;
      form.articleTitle = article.articleTitle || '';
      form.articleLink = article.articleLink || '';
      form.status = article.status !== undefined ? article.status : 1;
    } else {
      error.value = response.message || '加载文章失败';
    }
  } catch (err) {
    console.error('加载文章失败:', err);
    error.value = '加载文章失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

// 保存文章
const saveArticle = async () => {
  if (!articleFormRef.value) return;

  await articleFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请检查表单输入是否正确');
      return;
    }

    isSaving.value = true;
    try {
      const response = await updateArticle(articleId, form);
      if (response.code === 200) {
        ElMessage.success('文章保存成功');
        router.push({ name: 'ArticleManage' });
      } else {
        ElMessage.error(response.message || '保存失败');
      }
    } catch (err) {
      console.error('保存文章失败:', err);
      ElMessage.error('保存失败，请稍后重试');
    } finally {
      isSaving.value = false;
    }
  });
};

// 重置表单
const resetForm = () => {
  if (articleFormRef.value) {
    articleFormRef.value.resetFields();
  }
};

// 返回上一页
const goBack = () => {
  router.go(-1);
};

// 检查登录状态
onMounted(() => {
  if (!authStore.currentUser) {
    ElMessage.warning('请先登录');
    router.replace('/login');
    return;
  }

  loadArticle();
});
</script>


<style scoped>
.article-edit-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.form-help {
  font-size: 0.85em;
  color: #909399;
  margin-top: 5px;
}

.loading-container {
  padding: 40px;
}

.error-container {
  padding: 20px;
}

.el-card {
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}
</style>
