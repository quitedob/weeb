<template>
  <div class="article-edit-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <el-button
            circle
            @click="goBack"
            class="back-btn"
            :icon="ArrowLeft"
          >
          </el-button>
          <div class="header-info">
            <h1 class="page-title">编辑文章</h1>
            <p class="page-subtitle">精心创作，分享你的见解</p>
          </div>
        </div>
        <div class="header-actions">
          <el-button
            @click="previewArticle"
            class="preview-btn"
            :icon="View"
          >
            预览
          </el-button>
          <el-button
            type="primary"
            @click="saveArticle"
            :loading="isSaving"
            class="save-btn"
            :icon="Check"
          >
            {{ isSaving ? '保存中...' : '保存文章' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <div class="content-wrapper">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <div class="loading-card">
            <el-skeleton :rows="8" animated />
          </div>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="error" class="error-state">
          <el-alert
            :title="error"
            type="error"
            :closable="false"
            show-icon
            class="error-alert"
          >
            <template #title>
              <div class="error-title">
                <el-icon class="error-icon"><Warning /></el-icon>
                <span>{{ error }}</span>
              </div>
            </template>
          </el-alert>
        </div>

        <!-- 编辑表单 -->
        <div v-else class="edit-form">
          <el-form
            :model="form"
            :rules="rules"
            ref="articleFormRef"
            label-position="top"
            class="article-form"
          >
            <!-- 标题输入 -->
            <div class="form-section">
              <el-form-item label="文章标题" prop="articleTitle" class="title-item">
                <el-input
                  v-model="form.articleTitle"
                  placeholder="为你的文章起一个吸引人的标题..."
                  maxlength="200"
                  show-word-limit
                  class="title-input"
                  size="large"
                >
                  <template #prefix>
                    <el-icon><DocumentAdd /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </div>

            <!-- 基本信息卡片 -->
            <div class="form-section">
              <el-card class="info-card" shadow="never">
                <template #header>
                  <div class="card-header">
                    <el-icon><InfoFilled /></el-icon>
                    <span>基本信息</span>
                  </div>
                </template>

                <div class="info-grid">
                  <el-form-item label="文章分类" prop="categoryId">
                    <el-select
                      v-model="form.categoryId"
                      placeholder="选择合适的分类"
                      clearable
                      class="category-select"
                    >
                      <el-option
                        v-for="category in categories"
                        :key="category.id"
                        :label="category.categoryName"
                        :value="category.id"
                      >
                        <div class="category-option">
                          <span>{{ category.categoryName }}</span>
                        </div>
                      </el-option>
                    </el-select>
                  </el-form-item>

                  <el-form-item label="发布状态" prop="status">
                    <el-radio-group v-model="form.status" class="status-radio">
                      <el-radio :value="1" class="status-published">
                        <el-icon><VideoPlay /></el-icon>
                        立即发布
                      </el-radio>
                      <el-radio :value="0" class="status-draft">
                        <el-icon><Edit /></el-icon>
                        保存草稿
                      </el-radio>
                    </el-radio-group>
                  </el-form-item>
                </div>

                <el-form-item label="外部链接" prop="articleLink" class="link-item">
                  <el-input
                    v-model="form.articleLink"
                    placeholder="如果有相关外部链接，请填写..."
                    clearable
                  >
                    <template #prefix>
                      <el-icon><Link /></el-icon>
                    </template>
                  </el-input>
                  <div class="form-tip">
                    <el-icon><InfoFilled /></el-icon>
                    <span>可选字段，用于补充文章相关资源</span>
                  </div>
                </el-form-item>
              </el-card>
            </div>

            <!-- 内容编辑器 -->
            <div class="form-section">
              <el-form-item label="文章内容" prop="articleContent" class="content-item">
                <div class="editor-wrapper">
                  <!-- 工具栏 -->
                  <div class="editor-toolbar">
                    <div class="toolbar-left">
                      <div class="toolbar-group">
                        <el-tooltip content="粗体" placement="top">
                          <el-button
                            @click="insertMarkdown('**', '**')"
                            class="toolbar-btn"
                            size="small"
                          >
                            <el-icon><Bold /></el-icon>
                          </el-button>
                        </el-tooltip>

                        <el-tooltip content="斜体" placement="top">
                          <el-button
                            @click="insertMarkdown('*', '*')"
                            class="toolbar-btn"
                            size="small"
                          >
                            <el-icon><Italic /></el-icon>
                          </el-button>
                        </el-tooltip>

                        <el-tooltip content="行内代码" placement="top">
                          <el-button
                            @click="insertMarkdown('`', '`')"
                            class="toolbar-btn"
                            size="small"
                          >
                            <el-icon><Code /></el-icon>
                          </el-button>
                        </el-tooltip>
                      </div>

                      <div class="toolbar-divider"></div>

                      <div class="toolbar-group">
                        <el-tooltip content="标题" placement="top">
                          <el-button
                            @click="insertMarkdown('### ', '')"
                            class="toolbar-btn"
                            size="small"
                          >
                            <el-icon><Title /></el-icon>
                          </el-button>
                        </el-tooltip>

                        <el-tooltip content="列表" placement="top">
                          <el-button
                            @click="insertMarkdown('\n- ', '')"
                            class="toolbar-btn"
                            size="small"
                          >
                            <el-icon><List /></el-icon>
                          </el-button>
                        </el-tooltip>

                        <el-tooltip content="引用" placement="top">
                          <el-button
                            @click="insertMarkdown('\n> ', '')"
                            class="toolbar-btn"
                            size="small"
                          >
                            <el-icon><Quote /></el-icon>
                          </el-button>
                        </el-tooltip>
                      </div>

                      <div class="toolbar-divider"></div>

                      <div class="toolbar-group">
                        <el-tooltip content="代码块" placement="top">
                          <el-button
                            @click="insertMarkdown('\n\n```\n', '\n```\n')"
                            class="toolbar-btn"
                            size="small"
                          >
                            <el-icon><CodeBlock /></el-icon>
                          </el-button>
                        </el-tooltip>

                        <el-tooltip content="链接" placement="top">
                          <el-button
                            @click="insertMarkdown('[', '](url)')"
                            class="toolbar-btn"
                            size="small"
                          >
                            <el-icon><Link /></el-icon>
                          </el-button>
                        </el-tooltip>

                        <el-tooltip content="图片" placement="top">
                          <el-button
                            @click="insertMarkdown('![', '](image-url)')"
                            class="toolbar-btn"
                            size="small"
                          >
                            <el-icon><Picture /></el-icon>
                          </el-button>
                        </el-tooltip>
                      </div>
                    </div>

                    <div class="toolbar-right">
                      <el-button
                        @click="previewArticle"
                        type="info"
                        size="small"
                        class="preview-toolbar-btn"
                      >
                        <el-icon><View /></el-icon>
                        预览
                      </el-button>
                    </div>
                  </div>

                  <!-- 编辑器 -->
                  <div class="editor-container">
                    <el-input
                      v-model="form.articleContent"
                      type="textarea"
                      :rows="20"
                      :placeholder="editorPlaceholder"
                      maxlength="10000"
                      show-word-limit
                      resize="vertical"
                      class="content-editor"
                    ></el-input>
                  </div>

                  <!-- 格式提示 -->
                  <div class="format-tips">
                    <div class="tips-header">
                      <el-icon><InfoFilled /></el-icon>
                      <span>支持的 Markdown 格式</span>
                    </div>
                    <div class="tips-content">
                      <span class="tip-item">**粗体**</span>
                      <span class="tip-item">*斜体*</span>
                      <span class="tip-item">`行内代码`</span>
                      <span class="tip-item">### 标题</span>
                      <span class="tip-item">- 列表</span>
                      <span class="tip-item">> 引用</span>
                      <span class="tip-item">[链接](url)</span>
                    </div>
                  </div>
                </div>
              </el-form-item>
            </div>

            <!-- 操作按钮 -->
            <div class="form-actions">
              <el-space>
                <el-button
                  type="primary"
                  @click="saveArticle"
                  :loading="isSaving"
                  size="large"
                  class="action-btn primary"
                >
                  <el-icon><Check /></el-icon>
                  {{ isSaving ? '保存中...' : '保存文章' }}
                </el-button>

                <el-button
                  @click="resetForm"
                  size="large"
                  class="action-btn secondary"
                >
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>

                <el-button
                  @click="goBack"
                  size="large"
                  class="action-btn ghost"
                >
                  <el-icon><ArrowLeft /></el-icon>
                  返回
                </el-button>
              </el-space>
            </div>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 预览模态框 -->
    <el-dialog
      v-model="previewVisible"
      title="文章预览"
      width="90%"
      :before-close="closePreview"
      class="preview-dialog"
      top="5vh"
    >
      <div class="preview-container">
        <div class="preview-header">
          <h2 class="preview-title">{{ form.articleTitle || '无标题' }}</h2>
          <div class="preview-meta">
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>{{ authStore.currentUser?.username || '未知作者' }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Clock /></el-icon>
              <span>{{ new Date().toLocaleString() }}</span>
            </div>
            <div class="meta-item" v-if="form.categoryId">
              <el-icon><FolderOpened /></el-icon>
              <span>{{ getCategoryName(form.categoryId) }}</span>
            </div>
          </div>
        </div>

        <div class="preview-content">
          <div
            class="article-body"
            v-html="previewContent"
          ></div>
        </div>
      </div>

      <template #footer>
        <div class="preview-footer">
          <el-space>
            <el-button @click="closePreview" size="large">
              <el-icon><Close /></el-icon>
              关闭预览
            </el-button>
            <el-button
              type="primary"
              @click="saveArticle"
              :loading="isSaving"
              size="large"
            >
              <el-icon><Check /></el-icon>
              保存文章
            </el-button>
          </el-space>
        </div>
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
import {
  ArrowLeft,
  View,
  Check,
  Bold,
  Italic,
  Code,
  Title,
  List,
  Quote,
  CodeBlock,
  Link,
  Picture,
  Refresh,
  Close,
  User,
  Clock,
  FolderOpened,
  DocumentAdd,
  InfoFilled,
  VideoPlay,
  Edit,
  Warning
} from '@element-plus/icons-vue';

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

// 编辑器占位符文本
const editorPlaceholder = `开始创作你的精彩内容...

支持 Markdown 格式：
**粗体** *斜体* \`代码\`
### 标题
- 列表项
> 引用内容
\`\`\`代码块\`\`\``;

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
    if (response.code === 0 && response.data) {
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
    if (response.code === 0 && response.data) {
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
      if (response.code === 0) {
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

// 获取分类名称
const getCategoryName = (categoryId) => {
  const category = categories.value.find(cat => cat.id === categoryId);
  return category ? category.categoryName : '未分类';
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
