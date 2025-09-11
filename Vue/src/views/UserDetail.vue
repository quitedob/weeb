<template>
  <div class="user-detail-container">
    <div class="user-detail-card apple-card">
      <!-- 用户头像和基本信息 -->
      <div class="user-header">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img 
              :src="userInfo.avatar || '/public/badge/default-avatar.png'" 
              :alt="userInfo.username"
              class="user-avatar"
              @error="handleAvatarError"
            />
            <div class="online-status" :class="{ online: isOnline }"></div>
          </div>
        </div>
        
        <div class="user-info">
          <h1 class="username">{{ userInfo.username || '加载中...' }}</h1>
          <p class="user-id">ID: {{ userId }}</p>
          <div class="user-badges" v-if="userInfo.badges && userInfo.badges.length">
            <span 
              v-for="badge in userInfo.badges" 
              :key="badge.id"
              class="badge"
              :class="badge.type"
            >
              {{ badge.name }}
            </span>
          </div>
        </div>
      </div>

      <!-- 用户详细信息 -->
      <div class="user-details">
        <div class="detail-section">
          <h3 class="section-title">基本信息</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="label">用户名</span>
              <span class="value">{{ userInfo.username || '未知' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">邮箱</span>
              <span class="value">{{ userInfo.userEmail || '未设置' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">手机号</span>
              <span class="value">{{ userInfo.phoneNumber || '未设置' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">性别</span>
              <span class="value">{{ formatGender(userInfo.sex) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">注册时间</span>
              <span class="value">{{ formatDate(userInfo.registrationDate) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">用户类型</span>
              <span class="value">{{ formatUserType(userInfo.type) }}</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-section">
          <h3 class="section-title">操作</h3>
          <div class="action-buttons">
            <button 
              class="apple-button apple-button-primary"
              @click="startChat"
              :disabled="!canChat"
            >
              <span class="button-icon">💬</span>
              发送消息
            </button>
            
            <button 
              class="apple-button apple-button-secondary"
              @click="addContact"
              :disabled="isContact || isCurrentUser"
            >
              <span class="button-icon">👥</span>
              {{ isContact ? '已是联系人' : '添加联系人' }}
            </button>
            
            <button 
              v-if="!isCurrentUser"
              class="apple-button apple-button-outline"
              @click="viewProfile"
            >
              <span class="button-icon">👁️</span>
              查看资料
            </button>
          </div>
        </div>

        <!-- 统计信息 -->
        <div class="stats-section" v-if="userStats">
          <h3 class="section-title">统计信息</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-number">{{ userStats.messageCount || 0 }}</div>
              <div class="stat-label">消息数</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ userStats.onlineTime || 0 }}</div>
              <div class="stat-label">在线时长(小时)</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ userStats.lastSeen || '未知' }}</div>
              <div class="stat-label">最后在线</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="apple-loading"></div>
      <p>加载中...</p>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message apple-slide-in">
      <div class="error-icon">⚠️</div>
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { ElMessage } from 'element-plus'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const loading = ref(true)
const error = ref('')
const userInfo = ref({})
const userStats = ref(null)
const isOnline = ref(false)
const isContact = ref(false)

// 计算属性
const userId = computed(() => route.params.userId)
const isCurrentUser = computed(() => userId.value == authStore.currentUser?.id)

const canChat = computed(() => {
  return !isCurrentUser.value && userInfo.value.id
})

// 方法
const fetchUserInfo = async () => {
  try {
    loading.value = true
    error.value = ''
    
    // 获取用户基本信息
    const response = await api.user.getUserInfoById(userId.value)
    if (response.code === 0 && response.data) {
      userInfo.value = response.data
    } else {
      throw new Error(response.message || '获取用户信息失败')
    }
    
    // 获取用户统计信息
    try {
      const statsResponse = await api.user.getUserStats(userId.value)
      if (statsResponse.code === 0) {
        userStats.value = statsResponse.data
      }
    } catch (e) {
      console.warn('获取用户统计信息失败:', e)
    }
    
    // 检查是否在线
    checkOnlineStatus()
    
    // 检查是否已是联系人
    checkContactStatus()
    
  } catch (err) {
    console.error('获取用户信息失败:', err)
    error.value = err.message || '获取用户信息失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const checkOnlineStatus = async () => {
  try {
    const onlineUsers = await api.auth.getOnlineUsers()
    if (onlineUsers.code === 0 && onlineUsers.data) {
      isOnline.value = onlineUsers.data.includes(userId.value.toString())
    }
  } catch (e) {
    console.warn('检查在线状态失败:', e)
  }
}

const checkContactStatus = async () => {
  try {
    const contacts = await api.contact.getContacts()
    if (contacts.code === 0 && contacts.data) {
      isContact.value = contacts.data.some(contact => contact.id == userId.value)
    }
  } catch (e) {
    console.warn('检查联系人状态失败:', e)
  }
}

const startChat = () => {
  if (!canChat.value) return
  
  router.push({
    name: 'SpecificChat',
    params: { type: 'private', id: userId.value }
  })
}

const addContact = async () => {
  try {
    const response = await api.contact.apply({
      friendId: userId.value,
      remarks: `来自用户详情页的申请`
    })
    
    if (response.code === 0) {
      ElMessage.success('好友申请已发送')
      isContact.value = true
    } else {
      throw new Error(response.message || '发送申请失败')
    }
  } catch (err) {
    console.error('发送好友申请失败:', err)
    ElMessage.error(err.message || '发送申请失败')
  }
}

const viewProfile = () => {
  // 可以跳转到更详细的资料页面
  ElMessage.info('功能开发中...')
}

const handleAvatarError = (e) => {
  e.target.src = '/public/badge/default-avatar.png'
}

const formatGender = (sex) => {
  if (sex === 1) return '男'
  if (sex === 0) return '女'
  return '未知'
}

const formatUserType = (type) => {
  const typeMap = {
    'user': '普通用户',
    'admin': '管理员',
    'vip': 'VIP用户'
  }
  return typeMap[type] || type || '未知'
}

const formatDate = (date) => {
  if (!date) return '未知'
  return new Date(date).toLocaleDateString('zh-CN')
}

// 生命周期
onMounted(() => {
  if (userId.value) {
    fetchUserInfo()
  } else {
    error.value = '用户ID无效'
    loading.value = false
  }
})
</script>

<style scoped>
.user-detail-container {
  min-height: 100vh;
  padding: var(--apple-spacing-lg);
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-purple) 100%);
}

.user-detail-card {
  max-width: 800px;
  margin: 0 auto;
  padding: var(--apple-spacing-xl);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--apple-border-radius-lg);
}

.user-header {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xl);
  margin-bottom: var(--apple-spacing-xl);
  padding-bottom: var(--apple-spacing-lg);
  border-bottom: 1px solid var(--apple-border-color);
}

.avatar-section {
  position: relative;
}

.avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
}

.user-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid var(--apple-white);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.online-status {
  position: absolute;
  bottom: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--apple-gray);
  border: 3px solid var(--apple-white);
  transition: all 0.3s ease;
}

.online-status.online {
  background: var(--apple-green);
  box-shadow: 0 0 10px var(--apple-green);
}

.user-info {
  flex: 1;
}

.username {
  font-size: var(--apple-font-title);
  font-weight: 700;
  color: var(--apple-text-primary);
  margin: 0 0 var(--apple-spacing-sm) 0;
}

.user-id {
  font-size: var(--apple-font-md);
  color: var(--apple-text-secondary);
  margin: 0 0 var(--apple-spacing-md) 0;
}

.user-badges {
  display: flex;
  gap: var(--apple-spacing-sm);
  flex-wrap: wrap;
}

.badge {
  padding: var(--apple-spacing-xs) var(--apple-spacing-sm);
  border-radius: var(--apple-border-radius-sm);
  font-size: var(--apple-font-sm);
  font-weight: 500;
  color: var(--apple-white);
  background: var(--apple-blue);
}

.badge.crown {
  background: var(--apple-yellow);
  color: var(--apple-text-primary);
}

.badge.diamond {
  background: linear-gradient(45deg, var(--apple-blue), var(--apple-purple));
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xl);
}

.detail-section {
  background: var(--apple-background-secondary);
  padding: var(--apple-spacing-lg);
  border-radius: var(--apple-border-radius-md);
}

.section-title {
  font-size: var(--apple-font-lg);
  font-weight: 600;
  color: var(--apple-text-primary);
  margin: 0 0 var(--apple-spacing-md) 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--apple-spacing-md);
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--apple-spacing-sm) 0;
  border-bottom: 1px solid var(--apple-border-color);
}

.detail-item:last-child {
  border-bottom: none;
}

.label {
  font-weight: 500;
  color: var(--apple-text-secondary);
}

.value {
  color: var(--apple-text-primary);
  text-align: right;
}

.action-section {
  background: var(--apple-background-secondary);
  padding: var(--apple-spacing-lg);
  border-radius: var(--apple-border-radius-md);
}

.action-buttons {
  display: flex;
  gap: var(--apple-spacing-md);
  flex-wrap: wrap;
}

.button-icon {
  margin-right: var(--apple-spacing-xs);
}

.stats-section {
  background: var(--apple-background-secondary);
  padding: var(--apple-spacing-lg);
  border-radius: var(--apple-border-radius-md);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--apple-spacing-lg);
}

.stat-item {
  text-align: center;
  padding: var(--apple-spacing-md);
  background: var(--apple-white);
  border-radius: var(--apple-border-radius-sm);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-number {
  font-size: var(--apple-font-title);
  font-weight: 700;
  color: var(--apple-blue);
  margin-bottom: var(--apple-spacing-xs);
}

.stat-label {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.error-message {
  position: fixed;
  top: var(--apple-spacing-lg);
  right: var(--apple-spacing-lg);
  background: var(--apple-red);
  color: var(--apple-white);
  padding: var(--apple-spacing-md);
  border-radius: var(--apple-border-radius-md);
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
  z-index: 1000;
}

.error-icon {
  font-size: var(--apple-font-lg);
}

@media (max-width: 768px) {
  .user-detail-container {
    padding: var(--apple-spacing-md);
  }
  
  .user-header {
    flex-direction: column;
    text-align: center;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
