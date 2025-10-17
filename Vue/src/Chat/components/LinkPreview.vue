<template>
  <div class="link-preview" v-if="previewData" @click="handlePreviewClick">
    <div class="preview-card">
      <!-- 预览图片 -->
      <div v-if="previewData.image" class="preview-image">
        <img :src="previewData.image" :alt="previewData.title" />
        <div class="image-overlay">
          <span class="link-icon">🔗</span>
        </div>
      </div>

      <!-- 预览内容 -->
      <div class="preview-content">
        <div class="preview-title">{{ previewData.title }}</div>
        <div class="preview-description">{{ previewData.description }}</div>
        <div class="preview-url">{{ formatUrl(previewData.url) }}</div>

        <!-- 元数据 -->
        <div class="preview-meta">
          <span v-if="previewData.siteName" class="site-name">{{ previewData.siteName }}</span>
          <span v-if="previewData.author" class="author">{{ previewData.author }}</span>
          <span v-if="previewData.publishTime" class="publish-time">{{ formatTime(previewData.publishTime) }}</span>
        </div>

        <!-- 标签 -->
        <div v-if="previewData.tags && previewData.tags.length > 0" class="preview-tags">
          <el-tag
            v-for="tag in previewData.tags.slice(0, 3)"
            :key="tag"
            size="small"
            class="preview-tag"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="preview-actions">
        <el-button size="small" text @click.stop="copyLink">
          <el-icon><CopyDocument /></el-icon>
        </el-button>
        <el-button size="small" text @click.stop="openLink">
          <el-icon><Open /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="preview-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在获取链接预览...</span>
    </div>

    <!-- 错误状态 -->
    <div v-if="error" class="preview-error">
      <el-icon><Warning /></el-icon>
      <span>无法获取链接预览</span>
      <el-button size="small" text @click="retryPreview">重试</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Open, Loading, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  url: {
    type: String,
    required: true
  },
  autoLoad: {
    type: Boolean,
    default: true
  },
  showActions: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['preview-click', 'url-click'])

const previewData = ref(null)
const loading = ref(false)
const error = ref(false)

// 监听URL变化
watch(() => props.url, (newUrl) => {
  if (newUrl && props.autoLoad) {
    loadPreview()
  } else {
    resetPreview()
  }
})

// 加载链接预览
const loadPreview = async () => {
  if (!props.url) return

  try {
    loading.value = true
    error.value = false

    // 这里应该调用后端API获取链接预览数据
    // const response = await axiosInstance.post('/api/links/preview', { url: props.url })

    // 模拟预览数据
    previewData.value = {
      url: props.url,
      title: '示例链接标题',
      description: '这是一个示例链接的描述信息，展示了如何获取网页的元数据。',
      image: 'https://via.placeholder.com/300x200',
      siteName: '示例网站',
      author: '作者名称',
      publishTime: new Date().toISOString(),
      tags: ['示例', '链接', '预览']
    }
  } catch (err) {
    console.error('获取链接预览失败:', err)
    error.value = true
  } finally {
    loading.value = false
  }
}

// 重试预览
const retryPreview = () => {
  loadPreview()
}

// 重置预览
const resetPreview = () => {
  previewData.value = null
  loading.value = false
  error.value = false
}

// 处理预览点击
const handlePreviewClick = () => {
  emit('preview-click', previewData.value)
}

// 打开链接
const openLink = () => {
  window.open(props.url, '_blank')
  emit('url-click', props.url)
}

// 复制链接
const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(props.url)
    ElMessage.success('链接已复制到剪贴板')
  } catch (err) {
    // 降级方案：使用textarea临时元素
    const textarea = document.createElement('textarea')
    textarea.value = props.url
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('链接已复制到剪贴板')
  }
}

// 格式化URL显示
const formatUrl = (url) => {
  try {
    const urlObj = new URL(url)
    return urlObj.hostname
  } catch {
    return url.length > 50 ? url.substring(0, 50) + '...' : url
  }
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) { // 1分钟内
    return '刚刚'
  } else if (diff < 3600000) { // 1小时内
    return Math.floor(diff / 60000) + '分钟前'
  } else if (diff < 86400000) { // 24小时内
    return Math.floor(diff / 3600000) + '小时前'
  } else {
    return date.toLocaleDateString()
  }
}

// 检查是否为有效URL
const isValidUrl = (string) => {
  try {
    new URL(string)
    return true
  } catch {
    return false
  }
}

onMounted(() => {
  if (props.url && props.autoLoad && isValidUrl(props.url)) {
    loadPreview()
  }
})
</script>

<style scoped>
.link-preview {
  margin: 8px 0;
  max-width: 400px;
}

.preview-card {
  display: flex;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.preview-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.preview-image {
  width: 120px;
  height: 90px;
  position: relative;
  flex-shrink: 0;
  overflow: hidden;
}

.preview-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.preview-card:hover .image-overlay {
  opacity: 1;
}

.link-icon {
  color: white;
  font-size: 20px;
}

.preview-content {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preview-title {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview-description {
  font-size: 12px;
  color: #666;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview-url {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.preview-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.site-name {
  font-weight: 500;
}

.author {
  color: #666;
}

.publish-time {
  color: #999;
}

.preview-tags {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.preview-tag {
  font-size: 10px;
}

.preview-actions {
  padding: 8px;
  display: flex;
  gap: 4px;
  background: #f8f9fa;
  border-top: 1px solid #e0e0e0;
}

.preview-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  color: #666;
  font-size: 12px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.preview-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fef0f0;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .link-preview {
    max-width: 100%;
  }

  .preview-card {
    flex-direction: column;
  }

  .preview-image {
    width: 100%;
    height: 150px;
  }

  .preview-content {
    padding: 12px;
  }

  .preview-meta {
    flex-direction: column;
    gap: 2px;
  }
}
</style>
