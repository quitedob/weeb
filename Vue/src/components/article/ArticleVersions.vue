<template>
  <div class="article-versions">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文章版本历史</span>
          <el-button size="small" @click="loadVersions" :icon="Refresh">刷新</el-button>
        </div>
      </template>

      <!-- 版本列表 -->
      <el-timeline v-loading="loading">
        <el-timeline-item
          v-for="version in versions"
          :key="version.id"
          :timestamp="formatDate(version.createdAt)"
          placement="top"
        >
          <el-card class="version-card">
            <div class="version-header">
              <div class="version-info">
                <el-tag :type="version.isCurrent ? 'success' : 'info'" size="small">
                  {{ version.isCurrent ? '当前版本' : `版本 ${version.versionNumber}` }}
                </el-tag>
                <span class="version-author">作者: {{ version.authorName }}</span>
              </div>
              <div class="version-actions">
                <el-button size="small" @click="viewVersion(version)" :icon="View">
                  查看
                </el-button>
                <el-button
                  v-if="!version.isCurrent"
                  size="small"
                  type="primary"
                  @click="compareVersion(version)"
                  :icon="DocumentCopy"
                >
                  对比
                </el-button>
                <el-button
                  v-if="!version.isCurrent && canRestore"
                  size="small"
                  type="warning"
                  @click="restoreVersion(version)"
                  :icon="RefreshLeft"
                >
                  恢复
                </el-button>
              </div>
            </div>
            <div class="version-content">
              <p class="version-title">{{ version.title }}</p>
              <p class="version-summary">{{ version.summary || '无摘要' }}</p>
              <div class="version-meta">
                <span>字数: {{ version.wordCount || 0 }}</span>
                <span>修改: {{ version.changeDescription || '无说明' }}</span>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <!-- 空状态 -->
      <el-empty v-if="!loading && versions.length === 0" description="暂无版本历史" />
    </el-card>

    <!-- 版本查看对话框 -->
    <el-dialog v-model="viewDialogVisible" title="查看版本" width="80%" top="5vh">
      <div v-if="currentVersion" class="version-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="版本号">
            {{ currentVersion.versionNumber }}
          </el-descriptions-item>
          <el-descriptions-item label="作者">
            {{ currentVersion.authorName }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ formatDate(currentVersion.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="修改说明" :span="2">
            {{ currentVersion.changeDescription || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <h3>{{ currentVersion.title }}</h3>
        <div class="version-content-view" v-html="renderMarkdown(currentVersion.content)"></div>
      </div>
    </el-dialog>

    <!-- 版本对比对话框 -->
    <el-dialog v-model="compareDialogVisible" title="版本对比" width="90%" top="5vh">
      <div v-if="compareData" class="version-compare">
        <el-row :gutter="20">
          <el-col :span="12">
            <h4>当前版本</h4>
            <div class="compare-content" v-html="renderMarkdown(compareData.current)"></div>
          </el-col>
          <el-col :span="12">
            <h4>历史版本 {{ compareData.versionNumber }}</h4>
            <div class="compare-content" v-html="renderMarkdown(compareData.history)"></div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, View, DocumentCopy, RefreshLeft } from '@element-plus/icons-vue'
import { marked } from 'marked'

const props = defineProps({
  articleId: {
    type: [Number, String],
    required: true
  },
  canRestore: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['restore'])

// 数据
const versions = ref([])
const loading = ref(false)
const viewDialogVisible = ref(false)
const compareDialogVisible = ref(false)
const currentVersion = ref(null)
const compareData = ref(null)

// 方法
const loadVersions = async () => {
  loading.value = true
  try {
    // TODO: 实现API调用
    // const response = await articleApi.getVersions(props.articleId)
    // versions.value = response.data.data || []
    
    // 模拟数据
    versions.value = [
      {
        id: 1,
        versionNumber: 3,
        title: '文章标题 v3',
        content: '这是第三版的内容...',
        summary: '修复了一些错别字',
        authorName: '张三',
        wordCount: 1500,
        changeDescription: '修复错别字，优化排版',
        createdAt: new Date().toISOString(),
        isCurrent: true
      },
      {
        id: 2,
        versionNumber: 2,
        title: '文章标题 v2',
        content: '这是第二版的内容...',
        summary: '添加了新的章节',
        authorName: '张三',
        wordCount: 1200,
        changeDescription: '添加第三章节',
        createdAt: new Date(Date.now() - 86400000).toISOString(),
        isCurrent: false
      }
    ]
  } catch (error) {
    console.error('加载版本历史失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const viewVersion = (version) => {
  currentVersion.value = version
  viewDialogVisible.value = true
}

const compareVersion = (version) => {
  const currentVer = versions.value.find(v => v.isCurrent)
  if (!currentVer) {
    ElMessage.warning('无法找到当前版本')
    return
  }

  compareData.value = {
    current: currentVer.content,
    history: version.content,
    versionNumber: version.versionNumber
  }
  compareDialogVisible.value = true
}

const restoreVersion = async (version) => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复到版本 ${version.versionNumber} 吗？当前版本将被保存为历史版本。`,
      '确认恢复',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // TODO: 实现API调用
    // await articleApi.restoreVersion(props.articleId, version.id)
    
    ElMessage.success('版本已恢复')
    emit('restore', version)
    loadVersions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('恢复版本失败:', error)
      ElMessage.error('恢复失败')
    }
  }
}

const renderMarkdown = (content) => {
  if (!content) return ''
  return marked(content)
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 初始化
onMounted(() => {
  loadVersions()
})
</script>

<style scoped>
.article-versions {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.version-card {
  margin-bottom: 10px;
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.version-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.version-author {
  font-size: 14px;
  color: #606266;
}

.version-actions {
  display: flex;
  gap: 5px;
}

.version-content {
  padding: 10px 0;
}

.version-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 8px 0;
}

.version-summary {
  font-size: 14px;
  color: #606266;
  margin: 0 0 8px 0;
}

.version-meta {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #909399;
}

.version-detail {
  padding: 20px;
}

.version-content-view {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
  min-height: 300px;
}

.version-compare {
  padding: 20px;
}

.compare-content {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
}
</style>
