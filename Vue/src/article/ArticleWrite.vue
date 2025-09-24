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
          <el-input
            v-model="form.articleContent"
            type="textarea"
            :rows="15"
            placeholder="请输入文章内容..."
            maxlength="10000"
            show-word-limit
            resize="vertical"
          ></el-input>
          <div class="form-help">支持Markdown格式，最多10000字符</div>
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
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 1, max: 200, message: '标题长度在1到200个字符之间', trigger: 'blur' }
  ],
  articleContent: [
    { required: true, message: '请输入文章内容', trigger: 'blur' },
    { min: 10, max: 10000, message: '内容长度在10到10000个字符之间', trigger: 'blur' }
  ]
};

// 预览内容（简单的换行处理）
const previewContent = computed(() => {
  return form.articleContent.replace(/\n/g, '<br>');
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
    if (response.code === 200) {
      ElMessage.success('草稿保存成功');
      router.push({ name: 'ArticleManage' });
    } else {
      ElMessage.error(response.message || '保存草稿失败');
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
      if (response.code === 200) {
        ElMessage.success('文章发布成功！');
        router.push({ name: 'ArticleMain' });
      } else {
        ElMessage.error(response.message || '发布失败');
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