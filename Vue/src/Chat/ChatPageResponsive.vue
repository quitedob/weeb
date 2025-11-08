<template>
  <div class="chat-page-responsive">
    <!-- 移动端顶部导航栏 -->
    <div class="mobile-header" v-if="isMobile">
      <el-button 
        v-if="!showLeftPanel && !showRightPanel"
        circle 
        @click="showLeftPanel = true"
      >
        <el-icon><Menu /></el-icon>
      </el-button>
      <h3 class="mobile-title">{{ currentChatTitle }}</h3>
      <el-button 
        v-if="!showLeftPanel && !showRightPanel"
        circle 
        @click="showRightPanel = true"
      >
        <el-icon><User /></el-icon>
      </el-button>
    </div>

    <!-- 主聊天容器 - 响应式三栏布局 -->
    <div class="chat-container">
      <!-- 左侧面板：聊天列表 -->
      <div 
        class="panel panel-left" 
        :class="{ 
          'mobile-show': showLeftPanel,
          'mobile-hide': !showLeftPanel && isMobile 
        }"
      >
        <div class="panel-header">
          <h3>消息列表</h3>
          <el-button 
            v-if="isMobile" 
            circle 
            size="small"
            @click="showLeftPanel = false"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="panel-content">
          <!-- 聊天列表内容 -->
          <div class="chat-list">
            <div 
              v-for="chat in chatList" 
              :key="chat.id"
              class="chat-item"
              :class="{ active: currentChatId === chat.id }"
              @click="selectChat(chat)"
            >
              <el-avatar :src="chat.avatar" :size="50" />
              <div class="chat-info">
                <div class="chat-name">{{ chat.name }}</div>
                <div class="chat-last-message">{{ chat.lastMessage }}</div>
              </div>
              <div class="chat-meta">
                <div class="chat-time">{{ chat.time }}</div>
                <el-badge 
                  v-if="chat.unreadCount > 0" 
                  :value="chat.unreadCount" 
                  class="chat-badge"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间面板：聊天内容 -->
      <div class="panel panel-center">
        <div class="panel-header">
          <el-button 
            v-if="isMobile" 
            circle 
            size="small"
            @click="showLeftPanel = true"
          >
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <h3>{{ currentChatTitle }}</h3>
          <el-button 
            v-if="isMobile" 
            circle 
            size="small"
            @click="showRightPanel = true"
          >
            <el-icon><MoreFilled /></el-icon>
          </el-button>
        </div>
        <div class="panel-content chat-messages">
          <!-- 消息列表 -->
          <div 
            v-for="message in messages" 
            :key="message.id"
            class="message-item"
            :class="{ 'message-self': message.isSelf }"
          >
            <el-avatar 
              v-if="!message.isSelf" 
              :src="message.avatar" 
              :size="40" 
            />
            <div class="message-content">
              <div class="message-bubble">{{ message.content }}</div>
              <div class="message-time">{{ message.time }}</div>
            </div>
            <el-avatar 
              v-if="message.isSelf" 
              :src="message.avatar" 
              :size="40" 
            />
          </div>
        </div>
        <div class="panel-footer">
          <!-- 输入框 -->
          <div class="input-toolbar">
            <el-button circle size="small">
              <el-icon><Picture /></el-icon>
            </el-button>
            <el-button circle size="small">
              <el-icon><Paperclip /></el-icon>
            </el-button>
          </div>
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="输入消息..."
            @keydown.enter.exact="sendMessage"
          />
          <el-button type="primary" @click="sendMessage">
            发送
          </el-button>
        </div>
      </div>

      <!-- 右侧面板：详情信息 -->
      <div 
        class="panel panel-right" 
        :class="{ 
          'mobile-show': showRightPanel,
          'mobile-hide': !showRightPanel && isMobile 
        }"
      >
        <div class="panel-header">
          <h3>详情</h3>
          <el-button 
            v-if="isMobile" 
            circle 
            size="small"
            @click="showRightPanel = false"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="panel-content">
          <!-- 详情内容 -->
          <div class="chat-detail">
            <el-avatar :src="currentChatAvatar" :size="80" />
            <h3>{{ currentChatTitle }}</h3>
            <div class="detail-section">
              <h4>聊天信息</h4>
              <div class="detail-item">
                <span>成员数量</span>
                <span>{{ memberCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端遮罩层 -->
    <div 
      v-if="isMobile && (showLeftPanel || showRightPanel)" 
      class="mobile-overlay"
      @click="closeAllPanels"
    ></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  Menu, User, Close, ArrowLeft, MoreFilled,
  Picture, Paperclip
} from '@element-plus/icons-vue'
import api from '@/api'
import { useAuthStore } from '@/stores/authStore'

// 响应式状态
const windowWidth = ref(window.innerWidth)
const isMobile = computed(() => windowWidth.value <= 768)
const authStore = useAuthStore()

// 聊天数据加载函数
const loadChatList = async () => {
  try {
    const response = await api.chat.getChatList();
    if (response.code === 0) {
      // 转换聊天列表格式
      // ✅ CRITICAL FIX: Include sharedChatId in the mapped data
      chatList.value = response.data.map(chat => ({
        id: chat.id,
        sharedChatId: chat.sharedChatId || chat.shared_chat_id, // ✅ Add sharedChatId
        name: typeof chat.targetInfo === 'object' ? chat.targetInfo.name || chat.targetInfo.username : chat.targetInfo,
        avatar: chat.avatar || '',
        lastMessage: formatLastMessage(chat.lastMessage),
        time: formatChatTime(chat.updateTime),
        unreadCount: chat.unreadCount || 0,
        type: chat.type,
        targetId: chat.targetId
      }));
    }
  } catch (error) {
    console.error('加载聊天列表失败:', error);
  }
}

// 格式化函数
const formatChatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  if (diff < 172800000) return '昨天';
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
};

const formatLastMessage = (lastMessage) => {
  if (!lastMessage) return '暂无消息';

  // 如果是字符串
  if (typeof lastMessage === 'string') {
    // 检查是否是Java对象的toString格式: TextMessageContent(content=xxx, ...)
    const match = lastMessage.match(/content=([^,)]+)/);
    if (match && match[1]) {
      const content = match[1].trim();
      return content.length > 15 ? content.substring(0, 15) + '...' : content;
    }

    // 普通字符串
    return lastMessage.length > 15 ? lastMessage.substring(0, 15) + '...' : lastMessage;
  }

  // 如果是对象，尝试提取content
  if (typeof lastMessage === 'object') {
    let content = lastMessage.content || lastMessage.msgContent || lastMessage.message || lastMessage.text;

    // 如果content还是对象，继续提取
    if (typeof content === 'object' && content) {
      content = content.content || content.text;
    }

    // 如果有内容，截取并返回
    if (content && typeof content === 'string') {
      return content.length > 15 ? content.substring(0, 15) + '...' : content;
    }
  }

  return '暂无消息';
}

// 面板显示状态
const showLeftPanel = ref(false)
const showRightPanel = ref(false)

// 聊天数据
const chatList = ref([
  { id: 1, name: '张三', avatar: '', lastMessage: '你好', time: '10:30', unreadCount: 2 },
  { id: 2, name: '李四', avatar: '', lastMessage: '在吗', time: '09:15', unreadCount: 0 }
])

const currentChatId = ref(1)
const currentChatTitle = computed(() => {
  const chat = chatList.value.find(c => c.id === currentChatId.value)
  return chat?.name || '选择聊天'
})
const currentChatAvatar = computed(() => {
  const chat = chatList.value.find(c => c.id === currentChatId.value)
  return chat?.avatar || ''
})
const memberCount = ref(5)

const messages = ref([
  { id: 1, content: '你好', isSelf: false, avatar: '', time: '10:30' },
  { id: 2, content: '你好啊', isSelf: true, avatar: '', time: '10:31' }
])

const inputMessage = ref('')

// 方法
const handleResize = () => {
  windowWidth.value = window.innerWidth
}

const selectChat = async (chat) => {
  currentChatId.value = chat.id
  
  // ✅ CRITICAL FIX: Use sharedChatId (Long) instead of id (VARCHAR UUID)
  const sharedId = chat.sharedChatId || chat.shared_chat_id
  if (sharedId) {
    await chatStore.markChatAsRead(sharedId)
  } else {
    console.error('CRITICAL: Chat object missing sharedChatId!', chat)
  }
  
  if (isMobile.value) {
    showLeftPanel.value = false
  }
}

const closeAllPanels = () => {
  showLeftPanel.value = false
  showRightPanel.value = false
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return

  try {
    // 构造符合后端TextMessageContent结构的消息内容
    const textMessageContent = {
      content: inputMessage.value,
      contentType: 1, // TextContentType.TEXT.getCode()
      url: null,
      atUidList: []
    };

    const messageData = {
      content: textMessageContent,
      messageType: 1
    };

    // 使用正确的API发送消息
    const response = await api.chat.sendMessage(currentChatId.value, messageData);

    if (response.code === 0) {
      // 重新加载消息列表
      await loadMessages();
    } else {
      throw new Error(response.message || '发送失败');
    }

    inputMessage.value = ''
  } catch (error) {
    console.error('发送消息失败:', error);
    alert('发送消息失败: ' + (error.message || '未知错误'));
  }
}

const loadMessages = async () => {
  try {
    const response = await api.chat.getChatMessages(currentChatId.value);
    if (response.code === 0) {
      // 转换消息格式以适应现有的模板
      messages.value = response.data.map(msg => ({
        id: msg.id,
        content: typeof msg.content === 'object' ? msg.content.content : msg.content,
        isSelf: msg.senderId === authStore.userId,
        avatar: msg.fromAvatar || '',
        time: new Date(msg.timestamp || msg.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }));
    }
  } catch (error) {
    console.error('加载消息失败:', error);
  }
}

// 生命周期
onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadChatList()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.chat-page-responsive {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--apple-bg-secondary);
}

/* 移动端顶部导航 */
.mobile-header {
  display: none;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-12) var(--space-16);
  background: var(--apple-bg-primary);
  border-bottom: 1px solid var(--apple-border-color);
  height: 56px;
}

.mobile-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-text-primary);
}

/* 主聊天容器 - Grid布局 */
.chat-container {
  flex: 1;
  display: grid;
  grid-template-columns: 300px 1fr 300px;
  gap: 1px;
  background: var(--apple-border-color);
  overflow: hidden;
}

/* 面板通用样式 */
.panel {
  display: flex;
  flex-direction: column;
  background: var(--apple-bg-primary);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-16);
  border-bottom: 1px solid var(--apple-border-color);
  background: var(--apple-bg-primary);
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text-primary);
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-16);
}

.panel-footer {
  display: flex;
  align-items: flex-end;
  gap: var(--space-8);
  padding: var(--space-16);
  border-top: 1px solid var(--apple-border-color);
  background: var(--apple-bg-primary);
}

/* 聊天列表 */
.chat-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

.chat-item {
  display: flex;
  align-items: center;
  gap: var(--space-12);
  padding: var(--space-12);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
}

.chat-item:hover {
  background: var(--apple-bg-hover);
}

.chat-item.active {
  background: var(--apple-blue-light);
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-name {
  font-weight: 500;
  color: var(--apple-text-primary);
  margin-bottom: var(--space-4);
}

.chat-last-message {
  font-size: 13px;
  color: var(--apple-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-4);
}

.chat-time {
  font-size: 12px;
  color: var(--apple-text-tertiary);
}

/* 消息列表 */
.chat-messages {
  display: flex;
  flex-direction: column;
  gap: var(--space-16);
}

.message-item {
  display: flex;
  gap: var(--space-12);
  align-items: flex-start;
}

.message-item.message-self {
  flex-direction: row-reverse;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  max-width: 60%;
}

.message-self .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: var(--space-12) var(--space-16);
  border-radius: var(--radius-lg);
  background: var(--apple-bg-secondary);
  color: var(--apple-text-primary);
  word-wrap: break-word;
}

.message-self .message-bubble {
  background: var(--apple-blue);
  color: white;
}

.message-time {
  font-size: 12px;
  color: var(--apple-text-tertiary);
}

/* 输入工具栏 */
.input-toolbar {
  display: flex;
  gap: var(--space-8);
}

/* 详情面板 */
.chat-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-16);
}

.detail-section {
  width: 100%;
  margin-top: var(--space-16);
}

.detail-section h4 {
  margin: 0 0 var(--space-12) 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-text-secondary);
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: var(--space-12);
  background: var(--apple-bg-secondary);
  border-radius: var(--radius-md);
}

/* 移动端遮罩 */
.mobile-overlay {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

/* 响应式设计 - 平板 */
@media (max-width: 1024px) {
  .chat-container {
    grid-template-columns: 280px 1fr 280px;
  }
}

/* 响应式设计 - 移动端 */
@media (max-width: 768px) {
  .mobile-header {
    display: flex;
  }

  .chat-container {
    grid-template-columns: 1fr;
  }

  .panel-left,
  .panel-right {
    display: none;
  }

  .panel-left.mobile-show,
  .panel-right.mobile-show {
    display: flex;
    position: fixed;
    top: 56px;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 1000;
    background: var(--apple-bg-primary);
  }

  .mobile-overlay {
    display: block;
  }

  .message-content {
    max-width: 80%;
  }

  .panel-footer {
    flex-wrap: wrap;
  }

  .input-toolbar {
    width: 100%;
    justify-content: flex-start;
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .chat-page-responsive {
    background: var(--apple-bg-secondary-dark);
  }

  .panel {
    background: var(--apple-bg-primary-dark);
  }

  .panel-header,
  .panel-footer {
    background: var(--apple-bg-primary-dark);
    border-color: var(--apple-border-color-dark);
  }

  .message-bubble {
    background: var(--apple-bg-secondary-dark);
  }

  .detail-item {
    background: var(--apple-bg-secondary-dark);
  }
}

/* 滚动条样式 */
.panel-content::-webkit-scrollbar {
  width: 6px;
}

.panel-content::-webkit-scrollbar-track {
  background: transparent;
}

.panel-content::-webkit-scrollbar-thumb {
  background: var(--apple-border-color);
  border-radius: 3px;
}

.panel-content::-webkit-scrollbar-thumb:hover {
  background: var(--apple-text-tertiary);
}
</style>
