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

        <el-form-item label="文章分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择文章分类" clearable>
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.categoryName"
              :value="category.id"
            />
          </el-select>
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

        <el-form-item label="文章内容" prop="articleContent">
          <div class="editor-container">
            <div class="editor-toolbar">
              <el-button-group size="small">
                <el-button @click="insertMarkdown('**', '**')" title="粗体">
                  <strong>B</strong>
                </el-button>
                <el-button @click="insertMarkdown('*', '*')" title="斜体">
                  <em>I</em>
                </el-button>
                <el-button @click="insertMarkdown('`', '`')" title="代码">
                  Code
                </el-button>
                <el-button @click="insertMarkdown('### ', '')" title="标题">
                  H3
                </el-button>
                <el-button @click="insertMarkdown('\n- ', '')" title="列表">
                  List
                </el-button>
                <el-button @click="insertMarkdown('\n> ', '')" title="引用">
                  Quote
                </el-button>
                <el-button @click="insertMarkdown('\n\n```\n', '\n```\n')" title="代码块">
                  Code Block
                </el-button>
              </el-button-group>
              <el-button size="small" @click="previewArticle" type="info">预览</el-button>
            </div>
            <el-input
              v-model="form.articleContent"
              type="textarea"
              :rows="15"
              placeholder="请输入文章内容...支持Markdown格式"
              maxlength="10000"
              show-word-limit
              resize="vertical"
              class="content-editor"
            ></el-input>
          </div>
          <div class="form-help">
            支持Markdown格式：**粗体** *斜体* `代码` ### 标题 - 列表 > 引用 ```代码块```，最多10000字符
          </div>
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

    <!-- 预览模态框 -->
    <el-dialog v-model="previewVisible" title="文章预览" width="80%" :before-close="closePreview">
      <div class="preview-content">
        <h2>{{ form.articleTitle }}</h2>
        <div class="article-meta">
          <span>作者：{{ authStore.currentUser?.username || '未知' }}</span>
          <span>更新时间：{{ new Date().toLocaleString() }}</span>
        </div>
        <div class="article-body" v-html="previewContent"></div>
      </div>
      <template #footer>
        <el-button @click="closePreview">关闭预览</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getArticleById, updateArticle, getCategories } from '@/api/modules/article';
import { useAuthStore } from '@/stores/authStore';
import { ElMessage } from 'element-plus';
import { marked } from 'marked';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(true);
const error = ref('');
const isSaving = ref(false);
const articleFormRef = ref(null);
const previewVisible = ref(false); // 预览模态框显示状态
const categories = ref([]); // 分类列表

// 文章ID从路由参数获取
const articleId = route.params.articleId;

const form = reactive({
  articleTitle: '',
  articleLink: '',
  categoryId: null, // 新增分类ID字段
  status: 1,
  articleContent: '' // 新增文章内容字段
});

// 预览内容（使用marked库进行Markdown渲染）
const previewContent = computed(() => {
  if (!form.articleContent) return '';

  // 使用marked库进行专业的Markdown渲染
  return marked(form.articleContent);
});

const rules = {
  articleTitle: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 1, max: 200, message: '标题长度在1到200个字符之间', trigger: 'blur' }
  ],
  articleContent: [
    { required: true, message: '请输入文章内容', trigger: 'blur' },
    { min: 10, max: 10000, message: '内容长度在10到10000个字符之间', trigger: 'blur' }
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
      form.categoryId = article.categoryId || null; // 加载分类ID
      form.status = article.status !== undefined ? article.status : 1;
      form.articleContent = article.articleContent || ''; // 加载文章内容
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

// 加载分类数据
const loadCategories = async () => {
  try {
    const response = await getCategories();
    if (response.code === 200 && response.data) {
      categories.value = response.data;
    } else {
      ElMessage.error(response.message || '加载分类失败');
    }
  } catch (err) {
    console.error('加载分类失败:', err);
    ElMessage.error('加载分类失败，请稍后重试');
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

// 插入Markdown格式
const insertMarkdown = (prefix, suffix) => {
  const start = form.articleContent.length;
  const newContent = form.articleContent + prefix + suffix;
  form.articleContent = newContent;
  // 移动光标到插入位置
  const input = document.querySelector('.content-editor');
  if (input) {
    input.focus();
    input.setSelectionRange(start, start);
  }
};

// 预览文章
const previewArticle = () => {
  if (!form.articleTitle.trim() || !form.articleContent.trim()) {
    ElMessage.warning('请先输入标题和内容');
    return;
  }
  previewVisible.value = true;
};

// 关闭预览模态框
const closePreview = () => {
  previewVisible.value = false;
};

// 检查登录状态
onMounted(() => {
  if (!authStore.currentUser) {
    ElMessage.warning('请先登录');
    router.replace('/login');
    return;
  }

  loadArticle();
  loadCategories(); // 加载分类数据
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

.editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  margin-top: 10px;
}

.editor-toolbar {
  background-color: #f4f4f4;
  padding: 8px 10px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-editor {
  padding: 10px;
  min-height: 200px; /* Ensure enough height for content */
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  border: none;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
  font-family: 'Arial', 'Microsoft YaHei', 'SimSun', sans-serif;
}

.preview-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 20px;
}

.preview-content h2 {
  color: #303133;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.article-meta {
  color: #909399;
  font-size: 0.9em;
  margin-bottom: 20px;
  display: flex;
  gap: 20px;
}

.article-body {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  word-wrap: break-word;
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

.article-body blockquote {
  margin: 15px 0;
  padding: 10px 15px;
  border-left: 4px solid #409EFF;
  background-color: #f9f9f9;
  color: #666;
}

.article-body ul {
  margin: 10px 0;
  padding-left: 20px;
}

.article-body li {
  margin: 5px 0;
  list-style-type: disc;
}
</style>
