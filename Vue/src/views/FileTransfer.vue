<template>
  <div class="file-transfer-container">
    <div class="file-transfer-header">
      <h1 class="page-title">文件传输</h1>
      <p class="page-subtitle">安全、快速的文件分享与传输</p>
    </div>

    <div class="file-transfer-content">
      <!-- 文件上传区域 -->
      <div class="upload-section apple-card">
        <h2 class="section-title">上传文件</h2>
        <div class="upload-area" 
             @drop="handleDrop"
             @dragover.prevent
             @dragenter.prevent
             :class="{ dragging: isDragging }"
        >
          <div class="upload-icon">📁</div>
          <p class="upload-text">拖拽文件到此处或点击选择文件</p>
          <p class="upload-hint">支持多种文件格式，最大 100MB</p>
          <input
            ref="fileInput"
            type="file"
            multiple
            @change="handleFileSelect"
            class="file-input"
            accept="*/*"
          />
          <button 
            class="apple-button apple-button-outline"
            @click="triggerFileSelect"
          >
            选择文件
          </button>
        </div>
        
        <!-- 上传进度 -->
        <div v-if="uploadingFiles.length > 0" class="upload-progress">
          <h3 class="progress-title">上传进度</h3>
          <div class="progress-list">
            <div 
              v-for="file in uploadingFiles" 
              :key="file.id"
              class="progress-item"
            >
              <div class="file-info">
                <div class="file-icon">{{ getFileIcon(file.name) }}</div>
                <div class="file-details">
                  <div class="file-name">{{ file.name }}</div>
                  <div class="file-size">{{ formatFileSize(file.size) }}</div>
                </div>
              </div>
              
              <div class="progress-bar">
                <div 
                  class="progress-fill"
                  :style="{ width: file.progress + '%' }"
                  :class="{ error: file.error }"
                ></div>
              </div>
              
              <div class="progress-status">
                <span v-if="file.error" class="error-text">{{ file.error }}</span>
                <span v-else-if="file.progress === 100" class="success-text">完成</span>
                <span v-else class="progress-text">{{ file.progress }}%</span>
              </div>
              
              <button 
                v-if="file.progress < 100 && !file.error"
                class="cancel-button"
                @click="cancelUpload(file.id)"
              >
                ✕
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 文件传输历史 -->
      <div class="history-section apple-card">
        <h2 class="section-title">传输历史</h2>
        
        <!-- 筛选和搜索 -->
        <div class="filter-bar">
          <div class="filter-group">
            <button 
              v-for="filter in filters"
              :key="filter.value"
              class="filter-button"
              :class="{ active: currentFilter === filter.value }"
              @click="currentFilter = filter.value"
            >
              {{ filter.label }}
            </button>
          </div>
          
          <div class="search-box">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索文件名..."
              class="search-input"
            />
            <span class="search-icon">🔍</span>
          </div>
        </div>
        
        <!-- 文件列表 -->
        <div class="file-list">
          <div 
            v-for="file in filteredFiles" 
            :key="file.id"
            class="file-item"
          >
            <div class="file-icon">{{ getFileIcon(file.name) }}</div>
            
            <div class="file-info">
              <div class="file-name">{{ file.name }}</div>
              <div class="file-meta">
                <span class="file-size">{{ formatFileSize(file.size) }}</span>
                <span class="file-date">{{ formatDate(file.createdAt) }}</span>
                <span class="file-status" :class="file.status">
                  {{ getStatusText(file.status) }}
                </span>
              </div>
            </div>
            
            <div class="file-actions">
              <button 
                v-if="file.status === 'completed'"
                class="action-button"
                @click="downloadFile(file)"
                title="下载文件"
              >
                ⬇️
              </button>
              
              <button 
                v-if="file.status === 'completed'"
                class="action-button"
                @click="shareFile(file)"
                title="分享文件"
              >
                📤
              </button>
              
              <button 
                class="action-button"
                @click="deleteFile(file.id)"
                title="删除文件"
              >
                🗑️
              </button>
            </div>
          </div>
          
          <!-- 空状态 -->
          <div v-if="filteredFiles.length === 0" class="empty-state">
            <div class="empty-icon">📁</div>
            <p class="empty-text">暂无文件记录</p>
          </div>
        </div>
      </div>

      <!-- 文件分享弹窗 -->
      <div v-if="showShareModal" class="share-modal-overlay" @click="closeShareModal">
        <div class="share-modal apple-card" @click.stop>
          <h3 class="modal-title">分享文件</h3>
          
          <div class="share-options">
            <div class="share-option">
              <label class="share-label">分享链接</label>
              <div class="link-group">
                <input
                  :value="shareLink"
                  readonly
                  class="link-input"
                />
                <button 
                  class="copy-button"
                  @click="copyLink"
                >
                  复制
                </button>
              </div>
            </div>
            
            <div class="share-option">
              <label class="share-label">分享给用户</label>
              <div class="user-selector">
                <input
                  v-model="shareToUser"
                  type="text"
                  placeholder="输入用户名或邮箱"
                  class="user-input"
                />
                <button 
                  class="share-button"
                  @click="shareToUser"
                  :disabled="!shareToUser"
                >
                  分享
                </button>
              </div>
            </div>
            
            <div class="share-option">
              <label class="share-label">权限设置</label>
              <div class="permission-options">
                <label class="permission-item">
                  <input 
                    type="checkbox" 
                    v-model="sharePermissions.download"
                  />
                  <span>允许下载</span>
                </label>
                <label class="permission-item">
                  <input 
                    type="checkbox" 
                    v-model="sharePermissions.share"
                  />
                  <span>允许分享</span>
                </label>
              </div>
            </div>
          </div>
          
          <div class="modal-actions">
            <button 
              class="apple-button apple-button-outline"
              @click="closeShareModal"
            >
              取消
            </button>
            <button 
              class="apple-button apple-button-primary"
              @click="confirmShare"
            >
              确认分享
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

// 响应式数据
const fileInput = ref(null)
const isDragging = ref(false)
const uploadingFiles = ref([])
const files = ref([])
const currentFilter = ref('all')
const searchQuery = ref('')
const showShareModal = ref(false)
const selectedFile = ref(null)
const shareLink = ref('')
const shareToUser = ref('')
const sharePermissions = ref({
  download: true,
  share: false
})

// 筛选选项
const filters = [
  { label: '全部', value: 'all' },
  { label: '上传中', value: 'uploading' },
  { label: '已完成', value: 'completed' },
  { label: '已分享', value: 'shared' },
  { label: '已删除', value: 'deleted' }
]

// 计算属性
const filteredFiles = computed(() => {
  let result = files.value
  
  // 按状态筛选
  if (currentFilter.value !== 'all') {
    result = result.filter(file => file.status === currentFilter.value)
  }
  
  // 按搜索关键词筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(file => 
      file.name.toLowerCase().includes(query)
    )
  }
  
  return result
})

// 方法
const triggerFileSelect = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event) => {
  const selectedFiles = Array.from(event.target.files)
  handleFiles(selectedFiles)
}

const handleDrop = (event) => {
  isDragging.value = false
  const droppedFiles = Array.from(event.dataTransfer.files)
  handleFiles(droppedFiles)
}

const handleFiles = (files) => {
  files.forEach(file => {
    if (file.size > 100 * 1024 * 1024) { // 100MB限制
      ElMessage.error(`文件 ${file.name} 超过大小限制`)
      return
    }
    
    const fileObj = {
      id: Date.now() + Math.random(),
      name: file.name,
      size: file.size,
      type: file.type,
      progress: 0,
      error: null,
      file: file
    }
    
    uploadingFiles.value.push(fileObj)
    uploadFile(fileObj)
  })
}

const uploadFile = async (fileObj) => {
  try {
    const formData = new FormData()
    formData.append('file', fileObj.file)
    
    // 模拟上传进度
    const progressInterval = setInterval(() => {
      if (fileObj.progress < 90) {
        fileObj.progress += Math.random() * 10
      }
    }, 200)
    
    // 调用上传API
    const response = await api.file.upload(formData)
    
    clearInterval(progressInterval)
    fileObj.progress = 100
    
    if (response.code === 0) {
      ElMessage.success(`文件 ${fileObj.name} 上传成功`)
      // 添加到文件列表
      files.value.unshift({
        id: response.data.id,
        name: fileObj.name,
        size: fileObj.size,
        type: fileObj.type,
        status: 'completed',
        createdAt: new Date(),
        url: response.data.url
      })
    } else {
      throw new Error(response.message || '上传失败')
    }
    
  } catch (error) {
    console.error('文件上传失败:', error)
    fileObj.error = error.message || '上传失败'
    ElMessage.error(`文件 ${fileObj.name} 上传失败`)
  } finally {
    // 从上传列表中移除
    setTimeout(() => {
      const index = uploadingFiles.value.findIndex(f => f.id === fileObj.id)
      if (index > -1) {
        uploadingFiles.value.splice(index, 1)
      }
    }, 2000)
  }
}

const cancelUpload = (fileId) => {
  const index = uploadingFiles.value.findIndex(f => f.id === fileId)
  if (index > -1) {
    uploadingFiles.value.splice(index, 1)
    ElMessage.info('上传已取消')
  }
}

const downloadFile = async (file) => {
  try {
    // 创建下载链接
    const link = document.createElement('a')
    link.href = file.url
    link.download = file.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('开始下载文件')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  }
}

const shareFile = (file) => {
  selectedFile.value = file
  shareLink.value = `${window.location.origin}/file/${file.id}`
  showShareModal.value = true
}

const closeShareModal = () => {
  showShareModal.value = false
  selectedFile.value = null
  shareToUser.value = ''
  sharePermissions.value = { download: true, share: false }
}

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

const confirmShare = async () => {
  try {
    const response = await api.file.share({
      fileId: selectedFile.value.id,
      shareTo: shareToUser.value,
      permissions: sharePermissions.value
    })
    
    if (response.code === 0) {
      ElMessage.success('文件分享成功')
      closeShareModal()
    } else {
      throw new Error(response.message || '分享失败')
    }
  } catch (error) {
    console.error('分享失败:', error)
    ElMessage.error(error.message || '分享失败')
  }
}

const deleteFile = async (fileId) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个文件吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await api.file.delete(fileId)
    
    if (response.code === 0) {
      ElMessage.success('文件删除成功')
      const index = files.value.findIndex(f => f.id === fileId)
      if (index > -1) {
        files.value.splice(index, 1)
      }
    } else {
      throw new Error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const getFileIcon = (fileName) => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  const iconMap = {
    'pdf': '📄',
    'doc': '📝',
    'docx': '📝',
    'xls': '📊',
    'xlsx': '📊',
    'ppt': '📽️',
    'pptx': '📽️',
    'jpg': '🖼️',
    'jpeg': '🖼️',
    'png': '🖼️',
    'gif': '🖼️',
    'mp4': '🎥',
    'avi': '🎥',
    'mp3': '🎵',
    'wav': '🎵',
    'zip': '📦',
    'rar': '📦',
    'txt': '📄'
  }
  return iconMap[ext] || '📁'
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

const getStatusText = (status) => {
  const statusMap = {
    'uploading': '上传中',
    'completed': '已完成',
    'shared': '已分享',
    'deleted': '已删除'
  }
  return statusMap[status] || status
}

// 生命周期
onMounted(async () => {
  try {
    // 获取文件列表
    const response = await api.file.getFileList()
    if (response.code === 0) {
      files.value = response.data || []
    }
  } catch (error) {
    console.error('获取文件列表失败:', error)
  }
})
</script>

<style scoped>
.file-transfer-container {
  min-height: 100vh;
  padding: var(--apple-spacing-lg);
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-purple) 100%);
}

.file-transfer-header {
  text-align: center;
  margin-bottom: var(--apple-spacing-xl);
  color: var(--apple-white);
}

.page-title {
  font-size: var(--apple-font-title);
  font-weight: 700;
  margin: 0 0 var(--apple-spacing-sm) 0;
}

.page-subtitle {
  font-size: var(--apple-font-lg);
  margin: 0;
  opacity: 0.9;
}

.file-transfer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xl);
}

.upload-section,
.history-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--apple-border-radius-lg);
  padding: var(--apple-spacing-xl);
}

.section-title {
  font-size: var(--apple-font-lg);
  font-weight: 600;
  color: var(--apple-text-primary);
  margin: 0 0 var(--apple-spacing-lg) 0;
}

.upload-area {
  border: 2px dashed var(--apple-border-color);
  border-radius: var(--apple-border-radius-md);
  padding: var(--apple-spacing-xl);
  text-align: center;
  transition: all 0.3s ease;
  position: relative;
}

.upload-area:hover,
.upload-area.dragging {
  border-color: var(--apple-blue);
  background: rgba(0, 122, 255, 0.05);
}

.upload-icon {
  font-size: 48px;
  margin-bottom: var(--apple-spacing-md);
}

.upload-text {
  font-size: var(--apple-font-lg);
  font-weight: 500;
  color: var(--apple-text-primary);
  margin: 0 0 var(--apple-spacing-sm) 0;
}

.upload-hint {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  margin: 0 0 var(--apple-spacing-lg) 0;
}

.file-input {
  display: none;
}

.upload-progress {
  margin-top: var(--apple-spacing-lg);
}

.progress-title {
  font-size: var(--apple-font-md);
  font-weight: 500;
  color: var(--apple-text-primary);
  margin: 0 0 var(--apple-spacing-md) 0;
}

.progress-list {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-sm);
}

.progress-item {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-md);
  padding: var(--apple-spacing-md);
  background: var(--apple-background-secondary);
  border-radius: var(--apple-border-radius-sm);
}

.file-info {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
  min-width: 200px;
}

.file-icon {
  font-size: var(--apple-font-lg);
}

.file-details {
  display: flex;
  flex-direction: column;
}

.file-name {
  font-weight: 500;
  color: var(--apple-text-primary);
  font-size: var(--apple-font-sm);
}

.file-size {
  font-size: var(--apple-font-xs);
  color: var(--apple-text-secondary);
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: var(--apple-gray);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--apple-blue);
  transition: width 0.3s ease;
}

.progress-fill.error {
  background: var(--apple-red);
}

.progress-status {
  min-width: 80px;
  text-align: center;
  font-size: var(--apple-font-sm);
}

.success-text {
  color: var(--apple-green);
}

.error-text {
  color: var(--apple-red);
}

.progress-text {
  color: var(--apple-text-secondary);
}

.cancel-button {
  background: none;
  border: none;
  color: var(--apple-red);
  cursor: pointer;
  font-size: var(--apple-font-lg);
  padding: var(--apple-spacing-xs);
  border-radius: 50%;
  transition: background 0.3s ease;
}

.cancel-button:hover {
  background: rgba(255, 59, 48, 0.1);
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--apple-spacing-lg);
  gap: var(--apple-spacing-md);
}

.filter-group {
  display: flex;
  gap: var(--apple-spacing-sm);
}

.filter-button {
  padding: var(--apple-spacing-xs) var(--apple-spacing-md);
  border: 1px solid var(--apple-border-color);
  background: var(--apple-white);
  color: var(--apple-text-secondary);
  border-radius: var(--apple-border-radius-sm);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: var(--apple-font-sm);
}

.filter-button:hover {
  border-color: var(--apple-blue);
  color: var(--apple-blue);
}

.filter-button.active {
  background: var(--apple-blue);
  color: var(--apple-white);
  border-color: var(--apple-blue);
}

.search-box {
  position: relative;
  min-width: 250px;
}

.search-input {
  width: 100%;
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
  padding-right: 40px;
  border: 1px solid var(--apple-border-color);
  border-radius: var(--apple-border-radius-sm);
  font-size: var(--apple-font-sm);
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

.search-icon {
  position: absolute;
  right: var(--apple-spacing-sm);
  top: 50%;
  transform: translateY(-50%);
  color: var(--apple-text-secondary);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-sm);
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-md);
  padding: var(--apple-spacing-md);
  background: var(--apple-white);
  border-radius: var(--apple-border-radius-sm);
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.file-item:hover {
  border-color: var(--apple-blue);
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.1);
}

.file-item .file-icon {
  font-size: var(--apple-font-lg);
  min-width: 40px;
}

.file-item .file-info {
  flex: 1;
  min-width: 0;
}

.file-item .file-name {
  font-weight: 500;
  color: var(--apple-text-primary);
  margin-bottom: var(--apple-spacing-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-item .file-meta {
  display: flex;
  gap: var(--apple-spacing-md);
  font-size: var(--apple-font-xs);
  color: var(--apple-text-secondary);
}

.file-status {
  padding: 2px var(--apple-spacing-xs);
  border-radius: var(--apple-border-radius-xs);
  font-size: var(--apple-font-xs);
}

.file-status.completed {
  background: var(--apple-green);
  color: var(--apple-white);
}

.file-status.uploading {
  background: var(--apple-blue);
  color: var(--apple-white);
}

.file-status.shared {
  background: var(--apple-purple);
  color: var(--apple-white);
}

.file-status.deleted {
  background: var(--apple-red);
  color: var(--apple-white);
}

.file-actions {
  display: flex;
  gap: var(--apple-spacing-xs);
}

.action-button {
  background: none;
  border: none;
  font-size: var(--apple-font-lg);
  cursor: pointer;
  padding: var(--apple-spacing-xs);
  border-radius: var(--apple-border-radius-sm);
  transition: background 0.3s ease;
}

.action-button:hover {
  background: var(--apple-background-secondary);
}

.empty-state {
  text-align: center;
  padding: var(--apple-spacing-xl);
  color: var(--apple-text-secondary);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--apple-spacing-md);
}

.empty-text {
  font-size: var(--apple-font-md);
  margin: 0;
}

.share-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.share-modal {
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-title {
  font-size: var(--apple-font-lg);
  font-weight: 600;
  color: var(--apple-text-primary);
  margin: 0 0 var(--apple-spacing-lg) 0;
}

.share-options {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-lg);
  margin-bottom: var(--apple-spacing-xl);
}

.share-option {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-sm);
}

.share-label {
  font-weight: 500;
  color: var(--apple-text-primary);
  font-size: var(--apple-font-sm);
}

.link-group {
  display: flex;
  gap: var(--apple-spacing-sm);
}

.link-input {
  flex: 1;
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
  border: 1px solid var(--apple-border-color);
  border-radius: var(--apple-border-radius-sm);
  font-size: var(--apple-font-sm);
  background: var(--apple-background-secondary);
}

.copy-button {
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
  background: var(--apple-blue);
  color: var(--apple-white);
  border: none;
  border-radius: var(--apple-border-radius-sm);
  cursor: pointer;
  font-size: var(--apple-font-sm);
  white-space: nowrap;
}

.user-selector {
  display: flex;
  gap: var(--apple-spacing-sm);
}

.user-input {
  flex: 1;
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
  border: 1px solid var(--apple-border-color);
  border-radius: var(--apple-border-radius-sm);
  font-size: var(--apple-font-sm);
}

.share-button {
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
  background: var(--apple-green);
  color: var(--apple-white);
  border: none;
  border-radius: var(--apple-border-radius-sm);
  cursor: pointer;
  font-size: var(--apple-font-sm);
  white-space: nowrap;
}

.share-button:disabled {
  background: var(--apple-gray);
  cursor: not-allowed;
}

.permission-options {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-sm);
}

.permission-item {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
  cursor: pointer;
  font-size: var(--apple-font-sm);
  color: var(--apple-text-primary);
}

.modal-actions {
  display: flex;
  gap: var(--apple-spacing-md);
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .file-transfer-container {
    padding: var(--apple-spacing-md);
  }
  
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-box {
    min-width: auto;
  }
  
  .file-item {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--apple-spacing-sm);
  }
  
  .file-actions {
    align-self: flex-end;
  }
  
  .share-modal {
    width: 95%;
    margin: var(--apple-spacing-md);
  }
}
</style>
