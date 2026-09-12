<template>
  <div class="chat-container">
    <!-- 聊天列表侧边栏 -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">聊天</h2>
        <button class="apple-button apple-button-primary new-chat-btn" @click="startNewChat">
          <span class="btn-icon">✏️</span>
          <span class="btn-text">新聊天</span>
        </button>
      </div>

      <div class="chat-list">
        <div v-if="loading" class="loading-container">
          <div class="apple-loading"></div>
          <span class="loading-text">加载中...</span>
        </div>

        <div v-else-if="chatList.length === 0" class="empty-state">
          <div class="empty-icon">💬</div>
          <p class="empty-text">暂无聊天记录</p>
          <p class="empty-subtext">开始新的对话吧</p>
        </div>

        <div v-else class="chat-items">
          <div
            v-for="chat in chatList"
            :key="chat.id"
            class="chat-item"
            :class="{ active: currentChat?.id === chat.id }"
            @click="selectChat(chat)"
          >
            <div class="chat-avatar">
              <div class="avatar-placeholder">
                {{ chat.name?.charAt(0)?.toUpperCase() || 'C' }}
              </div>
            </div>
            <div class="chat-info">
              <div class="chat-name">{{ chat.name }}</div>
              <div class="chat-preview">{{ chat.lastMessage || '暂无消息' }}</div>
            </div>
            <div class="chat-meta">
              <div class="chat-time">{{ formatTime(chat.lastTime) }}</div>
              <div v-if="chat.unreadCount > 0" class="unread-badge apple-badge">
                {{ chat.unreadCount > 99 ? '99+' : chat.unreadCount }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 聊天主区域 -->
    <div class="chat-main">
      <div v-if="!currentChat" class="welcome-screen">
        <div class="welcome-content">
          <div class="welcome-icon">💬</div>
          <h1 class="welcome-title">欢迎使用 Weeb IM</h1>
          <p class="welcome-subtitle">选择一个聊天开始对话，或创建新的聊天</p>
          <button class="apple-button apple-button-primary welcome-btn" @click="startNewChat">
            开始新聊天
          </button>
        </div>
      </div>

      <div v-else class="chat-window">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="chat-user-info">
            <div class="user-avatar">
              <div class="avatar-placeholder">
                {{ currentChat.name?.charAt(0)?.toUpperCase() || 'U' }}
              </div>
            </div>
            <div class="user-details">
              <div class="user-name">{{ currentChat.name }}</div>
              <div class="user-status">在线</div>
            </div>
          </div>
          <div class="chat-actions">
            <button class="action-btn" @click="showChatInfo">
              <span class="action-icon">ℹ️</span>
            </button>
            <button class="action-btn" @click="showChatMenu">
              <span class="action-icon">⋮</span>
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="messages-container" ref="messagesContainer">
          <div v-if="messagesLoading" class="loading-container">
            <div class="apple-loading"></div>
            <span class="loading-text">加载消息中...</span>
          </div>

          <div v-else class="messages-list">
            <div
              v-for="message in messages"
              :key="message.id"
              class="message-item"
              :class="{ 'message-own': message.senderId === currentUser?.id }"
            >
              <div class="message-avatar" v-if="message.senderId !== currentUser?.id">
                <div class="avatar-placeholder small">
                  {{ getSenderName(message.senderId)?.charAt(0)?.toUpperCase() || 'U' }}
                </div>
              </div>
              <div class="message-content">
                <div class="message-bubble">
                  <div class="message-text">{{ message.content }}</div>
                  <div class="message-time">{{ formatMessageTime(message.createdAt) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 消息输入区域 -->
        <div class="message-input-container">
          <div class="input-wrapper">
            <textarea
              v-model="newMessage"
              class="message-input apple-input"
              placeholder="输入消息..."
              rows="1"
              @keydown.enter.prevent="sendMessage"
              @input="autoResize"
              ref="messageInput"
            ></textarea>
            <button
              class="send-button apple-button apple-button-primary"
              :disabled="!newMessage.trim()"
              @click="sendMessage"
            >
              <span class="send-icon">📤</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { ElMessage } from 'element-plus'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const loading = ref(false)
const messagesLoading = ref(false)
const currentChat = ref(null)
const chatList = ref([])
const messages = ref([])
const newMessage = ref('')
const messagesContainer = ref(null)
const messageInput = ref(null)

// 计算属性
const currentUser = computed(() => authStore.currentUser)

// 初始化
onMounted(async () => {
  await loadChatList()
  
  // 如果有路由参数，选择对应的聊天
  if (route.params.id) {
    const chat = chatList.value.find(c => c.id === route.params.id)
    if (chat) {
      selectChat(chat)
    }
  }
})

// 加载聊天列表
const loadChatList = async () => {
  loading.value = true
  try {
    const response = await api.chat.getChatList()
    chatList.value = response.data || []
  } catch (error) {
    console.error('加载聊天列表失败:', error)
    ElMessage.error('加载聊天列表失败')
  } finally {
    loading.value = false
  }
}

// 选择聊天
const selectChat = async (chat) => {
  currentChat.value = chat
  await loadMessages(chat.id)
  
  // 更新路由
  router.push(`/chat/${chat.type}/${chat.id}`)
}

// 加载消息
const loadMessages = async (chatId) => {
  messagesLoading.value = true
  try {
    const response = await api.chat.getChatMessages(chatId)
    messages.value = response.data || []

    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败:', error)
    ElMessage.error('加载消息失败')
  } finally {
    messagesLoading.value = false
  }
}

// 发送消息
const sendMessage = async () => {
  if (!newMessage.value.trim() || !currentChat.value) return

  const messageData = {
    content: newMessage.value.trim(),
    messageType: 0 // 修复：使用正确的消息类型字段
  }

  try {
    const response = await api.chat.sendMessage(currentChat.value.id, messageData)
    if (response.code === 0 || response.success) {
      // 添加消息到列表
      messages.value.push(response.data)
      newMessage.value = ''

      // 滚动到底部
      await nextTick()
      scrollToBottom()
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
  }
}

// 开始新聊天
const startNewChat = () => {
  // 这里可以实现新聊天的逻辑
  ElMessage.info('新聊天功能开发中...')
}

// 显示聊天信息
const showChatInfo = () => {
  ElMessage.info('聊天信息功能开发中...')
}

// 显示聊天菜单
const showChatMenu = () => {
  ElMessage.info('聊天菜单功能开发中...')
}

// 获取发送者姓名
const getSenderName = (senderId) => {
  if (senderId === currentUser.value?.id) {
    return currentUser.value.username
  }
  return currentChat.value?.name || '用户'
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  
  const now = new Date()
  const time = new Date(timestamp)
  const diff = now - time
  
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return time.toLocaleDateString()
}

// 格式化消息时间
const formatMessageTime = (timestamp) => {
  if (!timestamp) return ''
  
  const time = new Date(timestamp)
  const now = new Date()
  
  // 如果是今天，只显示时间
  if (time.toDateString() === now.toDateString()) {
    return time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  // 否则显示日期和时间
  return time.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + 
         ' ' + time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 自动调整输入框高度
const autoResize = () => {
  const textarea = messageInput.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px'
  }
}

// 监听消息变化，自动滚动
watch(messages, () => {
  nextTick(() => {
    scrollToBottom()
  })
}, { deep: true })
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100%;
  background-color: var(--apple-bg-primary);
}

.chat-sidebar {
  width: 320px;
  border-right: 1px solid var(--apple-border-secondary);
  display: flex;
  flex-direction: column;
  background-color: var(--apple-bg-secondary);
}

.sidebar-header {
  padding: var(--apple-spacing-lg);
  border-bottom: 1px solid var(--apple-border-secondary);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-title {
  margin: 0;
  font-size: var(--apple-font-xl);
  font-weight: 600;
  color: var(--apple-text-primary);
}

.new-chat-btn {
  height: 32px;
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xs);
  font-size: var(--apple-font-sm);
}

.btn-icon {
  font-size: var(--apple-font-sm);
}

.btn-text {
  font-size: var(--apple-font-sm);
}

.chat-list {
  flex: 1;
  overflow-y: auto;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--apple-spacing-xl);
  gap: var(--apple-spacing-md);
}

.loading-text {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-tertiary);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--apple-spacing-xl);
  gap: var(--apple-spacing-md);
}

.empty-icon {
  font-size: 48px;
  color: var(--apple-text-quaternary);
}

.empty-text {
  font-size: var(--apple-font-lg);
  color: var(--apple-text-secondary);
  margin: 0;
}

.empty-subtext {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-tertiary);
  margin: 0;
}

.chat-items {
  padding: var(--apple-spacing-sm);
}

.chat-item {
  display: flex;
  align-items: center;
  padding: var(--apple-spacing-md);
  border-radius: var(--apple-radius-medium);
  cursor: pointer;
  transition: all 0.2s ease;
  gap: var(--apple-spacing-md);
  margin-bottom: var(--apple-spacing-xs);
}

.chat-item:hover {
  background-color: var(--apple-bg-tertiary);
}

.chat-item.active {
  background-color: var(--apple-blue);
  color: white;
}

.chat-item.active .chat-name {
  color: white;
}

.chat-item.active .chat-preview {
  color: rgba(255, 255, 255, 0.8);
}

.chat-item.active .chat-time {
  color: rgba(255, 255, 255, 0.6);
}

.chat-avatar {
  flex-shrink: 0;
}

.avatar-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--apple-accent-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: var(--apple-font-lg);
  font-weight: 600;
}

.avatar-placeholder.small {
  width: 32px;
  height: 32px;
  font-size: var(--apple-font-sm);
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-name {
  font-size: var(--apple-font-md);
  font-weight: 600;
  color: var(--apple-text-primary);
  margin-bottom: var(--apple-spacing-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-preview {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--apple-spacing-xs);
  flex-shrink: 0;
}

.chat-time {
  font-size: var(--apple-font-xs);
  color: var(--apple-text-quaternary);
}

.unread-badge {
  min-width: 18px;
  height: 18px;
  font-size: var(--apple-font-xs);
  font-weight: 600;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.welcome-screen {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--apple-bg-secondary);
}

.welcome-content {
  text-align: center;
  max-width: 400px;
  padding: var(--apple-spacing-xl);
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: var(--apple-spacing-lg);
}

.welcome-title {
  font-size: var(--apple-font-title);
  font-weight: 700;
  color: var(--apple-text-primary);
  margin: 0 0 var(--apple-spacing-md) 0;
}

.welcome-subtitle {
  font-size: var(--apple-font-md);
  color: var(--apple-text-tertiary);
  margin: 0 0 var(--apple-spacing-xl) 0;
  line-height: 1.5;
}

.welcome-btn {
  height: 48px;
  font-size: var(--apple-font-lg);
  font-weight: 600;
}

.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  height: 64px;
  padding: 0 var(--apple-spacing-lg);
  border-bottom: 1px solid var(--apple-border-secondary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: var(--apple-bg-primary);
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-md);
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xs);
}

.user-name {
  font-size: var(--apple-font-lg);
  font-weight: 600;
  color: var(--apple-text-primary);
}

.user-status {
  font-size: var(--apple-font-sm);
  color: var(--apple-green);
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xs);
}

.user-status::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--apple-green);
}

.chat-actions {
  display: flex;
  gap: var(--apple-spacing-sm);
}

.action-btn {
  background: none;
  border: none;
  padding: var(--apple-spacing-sm);
  border-radius: var(--apple-radius-medium);
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--apple-text-secondary);
}

.action-btn:hover {
  background-color: var(--apple-bg-secondary);
  color: var(--apple-text-primary);
}

.action-icon {
  font-size: var(--apple-font-lg);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: var(--apple-spacing-lg);
  background-color: var(--apple-bg-secondary);
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-md);
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: var(--apple-spacing-sm);
}

.message-item.message-own {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message-bubble {
  background-color: var(--apple-bg-primary);
  border-radius: var(--apple-radius-large);
  padding: var(--apple-spacing-md);
  box-shadow: var(--apple-shadow-light);
  border: 1px solid var(--apple-border-secondary);
}

.message-item.message-own .message-bubble {
  background-color: var(--apple-blue);
  color: white;
  border-color: var(--apple-blue);
}

.message-text {
  font-size: var(--apple-font-md);
  line-height: 1.4;
  margin-bottom: var(--apple-spacing-xs);
  word-break: break-word;
}

.message-time {
  font-size: var(--apple-font-xs);
  color: var(--apple-text-quaternary);
}

.message-item.message-own .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.message-input-container {
  padding: var(--apple-spacing-lg);
  border-top: 1px solid var(--apple-border-secondary);
  background-color: var(--apple-bg-primary);
}

.input-wrapper {
  display: flex;
  gap: var(--apple-spacing-md);
  align-items: flex-end;
}

.message-input {
  flex: 1;
  resize: none;
  min-height: 40px;
  max-height: 120px;
  border-radius: var(--apple-radius-large);
  padding: var(--apple-spacing-md);
  font-family: inherit;
  line-height: 1.4;
}

.send-button {
  height: 40px;
  width: 40px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.send-icon {
  font-size: var(--apple-font-md);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-sidebar {
    width: 100%;
    position: absolute;
    left: 0;
    top: 0;
    height: 100%;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  
  .chat-sidebar.show {
    transform: translateX(0);
  }
  
  .chat-main {
    width: 100%;
  }
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .chat-sidebar {
    background-color: var(--apple-bg-secondary);
    border-right-color: var(--apple-border-primary);
  }
  
  .chat-header {
    background-color: var(--apple-bg-primary);
    border-bottom-color: var(--apple-border-primary);
  }
  
  .message-input-container {
    background-color: var(--apple-bg-primary);
    border-top-color: var(--apple-border-primary);
  }
}
</style>
