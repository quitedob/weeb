<template>
  <div class="article-write-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>发布新文章</h2>
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
            <el-button @click="saveDraft" :loading="isSavingDraft">
              {{ isSavingDraft ? '保存中...' : '保存草稿' }}
            </el-button>
            <el-button type="primary" @click="publishArticle" :loading="isPublishing">
              {{ isPublishing ? '发布中...' : '发布文章' }}
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="articleFormRef" label-width="120px">
        <el-form-item label="文章标题" prop="articleTitle">
          <el-input 
            v-model="form.articleTitle" 
            placeholder="请输入文章标题" 
            maxlength="200"
            show-word-limit
            clearable
          ></el-input>
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
            支持Markdown格式：**粗体** *斜体* `代码` ### 标题 - 列表，最多10000字符
          </div>
        </el-form-item>

        <el-form-item label="文章链接" prop="articleLink">
          <el-input 
            v-model="form.articleLink" 
            placeholder="请输入文章外部链接 (可选)" 
            clearable
          ></el-input>
          <div class="form-help">如果文章发布在其他平台，可以填写链接</div>
        </el-form-item>

        <el-form-item label="文章标签" prop="tags">
          <el-input 
            v-model="form.tags" 
            placeholder="请输入标签，用逗号分隔 (可选)" 
            clearable
          ></el-input>
          <div class="form-help">例如：技术,前端,Vue.js</div>
        </el-form-item>

        <el-form-item>
          <el-button @click="saveDraft" :loading="isSavingDraft">
            {{ isSavingDraft ? '保存中...' : '保存草稿' }}
          </el-button>
          <el-button type="primary" @click="publishArticle" :loading="isPublishing">
            {{ isPublishing ? '发布中...' : '发布文章' }}
          </el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预览模态框 -->
    <el-dialog v-model="previewVisible" title="文章预览" width="80%" :before-close="closePreview">
      <div class="preview-content">
        <h2>{{ form.articleTitle }}</h2>
        <div class="article-meta">
          <span>作者：{{ authStore.currentUser?.username || '未知' }}</span>
          <span>创建时间：{{ new Date().toLocaleString() }}</span>
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
import { reactive, ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { createArticle } from '@/api/modules/article';
import { useAuthStore } from '@/stores/authStore';
import { ElMessage, ElMessageBox } from 'element-plus';
import { marked } from 'marked';

const router = useRouter();
const authStore = useAuthStore();

const isSavingDraft = ref(false);
const isPublishing = ref(false);
const previewVisible = ref(false);
const articleFormRef = ref(null);

const form = reactive({
  articleTitle: '',
  articleContent: '',
  articleLink: '',
  tags: '',
  status: 1 // 1: 发布, 0: 草稿
});

const rules = {
  articleTitle: [
    { required: true, message: '请输入文章标题', trigger: 'blur' }
  ],
  articleContent: [
    { required: true, message: '请输入文章内容', trigger: 'blur' }
  ]
};

// 预览内容（使用marked库进行Markdown渲染）
const previewContent = computed(() => {
  if (!form.articleContent) return '';

  // 使用marked库进行专业的Markdown渲染
  return marked(form.articleContent);
});

// 保存草稿
const saveDraft = async () => {
  if (!form.articleTitle.trim()) {
    ElMessage.warning('请至少输入文章标题');
    return;
  }

  isSavingDraft.value = true;
  try {
    const articleData = {
      ...form,
      status: 0 // 草稿状态
    };
    
    const response = await createArticle(articleData);
    if (response.code === 0) {
      ElMessage.success('草稿保存成功');
      router.push({ name: 'ArticleManage' });
    } else {
      ElMessage.error(response?.message || '保存草稿失败');
    }
  } catch (err) {
    console.error('保存草稿失败:', err);
    ElMessage.error('保存草稿失败，请稍后重试');
  } finally {
    isSavingDraft.value = false;
  }
};

// 发布文章
const publishArticle = async () => {
  if (!articleFormRef.value) return;

  await articleFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请检查表单输入是否正确');
      return;
    }

    // 确认发布
    try {
      await ElMessageBox.confirm(
        '确定要发布这篇文章吗？发布后其他用户将能够看到。',
        '确认发布',
        {
          confirmButtonText: '确定发布',
          cancelButtonText: '取消',
          type: 'info'
        }
      );
    } catch {
      return; // 用户取消
    }

    isPublishing.value = true;
    try {
      const articleData = {
        ...form,
        status: 1 // 发布状态
      };
      
      const response = await createArticle(articleData);
      if (response.code === 0) {
        ElMessage.success('文章发布成功！');
        router.push({ name: 'ArticleMain' });
      } else {
        ElMessage.error(response?.message || '发布失败');
      }
    } catch (err) {
      console.error('发布文章失败:', err);
      ElMessage.error('发布失败，请稍后重试');
    } finally {
      isPublishing.value = false;
    }
  });
};

// 重置表单
const resetForm = () => {
  if (articleFormRef.value) {
    articleFormRef.value.resetFields();
  }
  form.articleTitle = '';
  form.articleContent = '';
  form.articleLink = '';
  form.tags = '';
};

// 插入Markdown格式
const insertMarkdown = (before, after) => {
  const textarea = document.querySelector('.content-editor textarea');
  if (!textarea) return;
  
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const selectedText = form.articleContent.substring(start, end);
  
  const newText = before + selectedText + after;
  form.articleContent = form.articleContent.substring(0, start) + newText + form.articleContent.substring(end);
  
  // 重新设置光标位置
  setTimeout(() => {
    textarea.focus();
    textarea.setSelectionRange(start + before.length, start + before.length + selectedText.length);
  }, 0);
};

// 预览文章
const previewArticle = () => {
  if (!form.articleTitle.trim() || !form.articleContent.trim()) {
    ElMessage.warning('请先输入标题和内容');
    return;
  }
  previewVisible.value = true;
};

// 关闭预览
const closePreview = () => {
  previewVisible.value = false;
};

// 返回上一页
const goBack = () => {
  if (form.articleTitle.trim() || form.articleContent.trim()) {
    ElMessageBox.confirm(
      '当前有未保存的内容，确定要离开吗？',
      '确认离开',
      {
        confirmButtonText: '确定离开',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      router.go(-1);
    }).catch(() => {
      // 用户取消，不做任何操作
    });
  } else {
    router.go(-1);
  }
};

// 检查登录状态
onMounted(() => {
  if (!authStore.currentUser) {
    ElMessage.warning('请先登录');
    router.replace('/login');
    return;
  }
});

// 页面离开前的确认
window.addEventListener('beforeunload', (e) => {
  if (form.articleTitle.trim() || form.articleContent.trim()) {
    e.preventDefault();
    e.returnValue = '当前有未保存的内容，确定要离开吗？';
  }
});
</script>

<style scoped>
.article-write-container {
  padding: 20px;
  max-width: 1000px;
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

.editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.editor-toolbar {
  background: #f5f7fa;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-editor {
  border: none !important;
}

.content-editor :deep(.el-textarea__inner) {
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  line-height: 1.6;
}

.el-card {
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.preview-content {
  max-height: 60vh;
  overflow-y: auto;
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

.article-body ul {
  margin: 10px 0;
  padding-left: 20px;
}

.article-body li {
  margin: 5px 0;
  list-style-type: disc;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .article-write-container {
    padding: 10px;
  }
  
  .card-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>