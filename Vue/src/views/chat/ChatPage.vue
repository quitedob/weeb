<template>
  <div class="chat-page">
    <!-- 左侧聊天列表 -->
    <div class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h2 v-if="!sidebarCollapsed">消息</h2>
        <div class="header-actions">
          <button @click="showNewChatDialog = true" class="icon-btn" title="新建聊天">
            <span>➕</span>
          </button>
          <button @click="sidebarCollapsed = !sidebarCollapsed" class="icon-btn" title="折叠">
            <span>{{ sidebarCollapsed ? '☰' : '✕' }}</span>
          </button>
        </div>
      </div>

      <div v-if="!sidebarCollapsed" class="search-box">
        <input v-model="searchQuery" type="text" placeholder="搜索聊天..." />
      </div>

      <div class="chat-list">
        <div v-if="filteredChatList.length === 0" class="empty-list">
          <p>暂无聊天</p>
          <button @click="showNewChatDialog = true" class="primary-btn">开始新聊天</button>
        </div>
        
        <div
          v-for="chat in filteredChatList"
          :key="chat.id"
          :class="['chat-item', { active: activeChatId === chat.id }]"
          @click="selectChat(chat)"
        >
          <div class="chat-avatar">
            <img :src="chat.targetInfo?.avatar || defaultAvatar" :alt="chat.targetInfo?.name" />
            <span v-if="isUserOnline(chat.targetInfo?.id)" class="online-indicator"></span>
          </div>
          <div v-if="!sidebarCollapsed" class="chat-info">
            <div class="chat-header-row">
              <div class="chat-name">{{ chat.targetInfo?.name }}</div>
              <div class="chat-time">{{ formatChatTime(chat.lastMessageTime) }}</div>
            </div>
            <div class="chat-preview-row">
              <div class="chat-last-msg">
                {{ formatLastMessage(chat.lastMessage) }}
              </div>
              <div v-if="chat.unreadCount > 0" class="unread-badge">{{ chat.unreadCount }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天窗口 -->
    <div class="chat-main">
      <div v-if="!activeChatId" class="empty-chat">
        <div class="empty-icon">💬</div>
        <h3>选择一个聊天开始对话</h3>
        <p>从左侧选择一个聊天，或创建新的聊天</p>
      </div>

      <template v-else>
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="header-left">
            <div class="chat-avatar-small">
              <img :src="currentChat?.targetInfo?.avatar || defaultAvatar" :alt="currentChat?.targetInfo?.name" />
              <span v-if="isUserOnline(currentChat?.targetInfo?.id)" class="online-indicator"></span>
            </div>
            <div class="chat-title-info">
              <div class="chat-title">{{ currentChat?.targetInfo?.name }}</div>
              <div class="chat-status">
                {{ isUserOnline(currentChat?.targetInfo?.id) ? '在线' : '离线' }}
              </div>
            </div>
          </div>
          <div class="header-right">
            <button @click="loadMoreMessages" :disabled="!canLoadMore" class="icon-btn" title="加载更多">
              <span>⬆️</span>
            </button>
            <button @click="showChatInfo = !showChatInfo" class="icon-btn" title="聊天信息">
              <span>ℹ️</span>
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="message-container" ref="messageContainerRef">
          <div v-if="isLoadingMessages" class="loading-indicator">
            <span>加载中...</span>
          </div>

          <div class="message-list">
            <div
              v-for="(msg, index) in messages"
              :key="msg.id || msg.tempId"
              :class="['message-item', { 'is-me': msg.isFromMe }]"
            >
              <!-- 时间分隔符 -->
              <div v-if="shouldShowTimeDivider(msg, index)" class="time-divider">
                {{ formatMessageDate(msg.timestamp) }}
              </div>

              <div class="message-wrapper">
                <div class="message-avatar">
                  <img :src="msg.fromAvatar || defaultAvatar" :alt="msg.fromName" />
                </div>

                <div class="message-content-wrapper">
                  <div class="message-sender">{{ msg.fromName }}</div>
                  
                  <div class="message-bubble-container">
                    <div :class="['message-bubble', { recalled: msg.isRecalled }]">
                      <div v-if="msg.isRecalled" class="recalled-text">
                        <span>🚫 消息已撤回</span>
                      </div>
                      <div v-else-if="msg.messageType === 2" class="file-message">
                        <div class="file-icon">📄</div>
                        <div class="file-info">
                          <div class="file-name">{{ msg.fileData?.fileName || '文件' }}</div>
                          <div class="file-size">{{ formatFileSize(msg.fileData?.fileSize) }}</div>
                        </div>
                        <button @click="downloadFile(msg.fileData)" class="download-btn">下载</button>
                      </div>
                      <div v-else class="text-message">
                        {{ typeof msg.content === 'object' ? msg.content.content : msg.content }}
                      </div>

                      <!-- 消息状态 -->
                      <div v-if="msg.isFromMe && !msg.isRecalled" class="message-status">
                        <span v-if="msg.status === 'sending'">⏳</span>
                        <span v-else-if="msg.status === 'sent'">✓</span>
                        <span v-else-if="msg.status === 'delivered'">✓✓</span>
                        <span v-else-if="msg.status === 'read'">✓✓</span>
                      </div>
                    </div>

                    <!-- 消息操作按钮 -->
                    <div v-if="!msg.isRecalled" class="message-actions">
                      <button @click="showReactionPicker(msg)" class="action-btn" title="添加反应">
                        <span>😊</span>
                      </button>
                      <button v-if="msg.isFromMe" @click="recallMessage(msg)" class="action-btn" title="撤回">
                        <span>↩️</span>
                      </button>
                    </div>
                  </div>

                  <!-- 消息反应 -->
                  <div v-if="msg.reactions && msg.reactions.length > 0" class="message-reactions">
                    <div
                      v-for="reaction in msg.reactions"
                      :key="reaction.emoji"
                      class="reaction-item"
                      @click="toggleReaction(msg, reaction.emoji)"
                    >
                      <span class="reaction-emoji">{{ reaction.emoji }}</span>
                      <span class="reaction-count">{{ reaction.count }}</span>
                    </div>
                  </div>

                  <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 正在输入指示器 -->
          <div v-if="isTyping" class="typing-indicator">
            <div class="typing-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <span class="typing-text">对方正在输入...</span>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-toolbar">
            <button @click="showEmojiPicker = !showEmojiPicker" class="toolbar-btn" title="表情">
              <span>😊</span>
            </button>
            <button @click="triggerFileUpload" class="toolbar-btn" title="发送文件">
              <span>📎</span>
            </button>
            <input
              ref="fileInputRef"
              type="file"
              style="display: none"
              @change="handleFileSelect"
            />
          </div>

          <!-- 文件预览 -->
          <div v-if="selectedFile" class="file-preview">
            <div class="file-preview-info">
              <span class="file-icon">📄</span>
              <span class="file-name">{{ selectedFile.name }}</span>
              <span class="file-size">({{ formatFileSize(selectedFile.size) }})</span>
            </div>
            <button @click="clearFileSelection" class="remove-file-btn">✕</button>
          </div>

          <div class="input-box">
            <textarea
              v-model="messageInput"
              ref="messageInputRef"
              placeholder="输入消息... (Enter发送，Shift+Enter换行)"
              @keydown="handleKeyDown"
              @input="handleTyping"
              rows="1"
            ></textarea>
            <button
              @click="sendMessage"
              :disabled="!canSendMessage"
              class="send-btn"
            >
              <span>发送</span>
            </button>
          </div>

          <!-- 表情选择器 -->
          <div v-if="showEmojiPicker" class="emoji-picker">
            <div class="emoji-grid">
              <span
                v-for="emoji in commonEmojis"
                :key="emoji"
                class="emoji-item"
                @click="insertEmoji(emoji)"
              >
                {{ emoji }}
              </span>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 聊天信息侧边栏 -->
    <div v-if="showChatInfo && activeChatId" class="chat-info-sidebar">
      <div class="info-header">
        <h3>聊天信息</h3>
        <button @click="showChatInfo = false" class="close-btn">✕</button>
      </div>
      <div class="info-content">
        <div class="info-avatar">
          <img :src="currentChat?.targetInfo?.avatar || defaultAvatar" :alt="currentChat?.targetInfo?.name" />
        </div>
        <div class="info-name">{{ currentChat?.targetInfo?.name }}</div>
        <div class="info-actions">
          <button @click="viewUserProfile" class="info-btn">查看资料</button>
          <button @click="confirmDeleteChat" class="info-btn danger">删除聊天</button>
        </div>
      </div>
    </div>

    <!-- 新建聊天对话框 -->
    <div v-if="showNewChatDialog" class="modal-overlay" @click="showNewChatDialog = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>新建聊天</h3>
          <button @click="showNewChatDialog = false" class="close-btn">✕</button>
        </div>
        
        <div class="modal-body">
          <div class="search-contacts">
            <input v-model="contactSearchQuery" type="text" placeholder="搜索联系人..." />
          </div>

          <div v-if="isLoadingContacts" class="loading">加载中...</div>
          
          <div v-else-if="filteredContacts.length === 0" class="empty-contacts">
            <p>暂无联系人</p>
            <button @click="goToAddFriend" class="primary-btn">添加好友</button>
          </div>

          <div v-else class="contact-list">
            <div
              v-for="contact in filteredContacts"
              :key="contact.id"
              class="contact-item"
              @click="createNewChat(contact.id)"
            >
              <img :src="contact.avatar || defaultAvatar" :alt="contact.username" />
              <div class="contact-info">
                <div class="contact-name">{{ contact.username }}</div>
                <div class="contact-status">{{ contact.bio || '这个人很懒，什么都没写' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 反应选择器 -->
    <div v-if="showReactionPickerDialog" class="reaction-picker-overlay" @click="showReactionPickerDialog = false">
      <div class="reaction-picker" @click.stop>
        <span
          v-for="emoji in reactionEmojis"
          :key="emoji"
          class="reaction-emoji-item"
          @click="addReaction(currentReactionMessage, emoji)"
        >
          {{ emoji }}
        </span>
      </div>
    </div>

    <!-- WebSocket连接状态 -->
    <div v-if="connectionStatus !== 'connected'" class="connection-status">
      <span v-if="connectionStatus === 'connecting'">🔄 连接中...</span>
      <span v-else-if="connectionStatus === 'disconnected'">
        ⚠️ 已断开连接
        <button @click="reconnectWebSocket" class="reconnect-btn">重新连接</button>
      </span>
      <span v-else-if="connectionStatus === 'error'">
        ❌ 连接失败
        <button @click="reconnectWebSocket" class="reconnect-btn">重试</button>
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useChatStore } from '@/stores/chatStore';
import { useAuthStore } from '@/stores/authStore';
import api from '@/api';

const router = useRouter();
const chatStore = useChatStore();
const authStore = useAuthStore();

// 引用
const messageContainerRef = ref(null);
const messageInputRef = ref(null);
const fileInputRef = ref(null);

// 数据
const chatList = ref([]);
const contacts = ref([]);
const activeChatId = ref(null);
const messageInput = ref('');
const selectedFile = ref(null);
const searchQuery = ref('');
const contactSearchQuery = ref('');
const sidebarCollapsed = ref(false);
const showNewChatDialog = ref(false);
const showChatInfo = ref(false);
const showEmojiPicker = ref(false);
const showReactionPickerDialog = ref(false);
const currentReactionMessage = ref(null);
const isLoadingMessages = ref(false);
const isLoadingContacts = ref(false);
const isTyping = ref(false);
const typingTimeout = ref(null);

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';

// 常用表情
const commonEmojis = ['😊', '😂', '❤️', '👍', '👎', '🎉', '😢', '😡', '🤔', '👏', '🙏', '💪', '🔥', '✨', '🎈'];
const reactionEmojis = ['👍', '❤️', '😂', '😮', '😢', '😡', '🎉', '🔥'];

// 计算属性
const currentChat = computed(() => {
  return chatList.value.find(chat => chat.id === activeChatId.value);
});

const messages = computed(() => {
  const msgs = chatStore.messagesForCurrentChat || [];
  if (msgs.length > 0) {
    console.log('💬 消息列表:', msgs);
    console.log('💬 第一条消息结构:', msgs[0]);
    console.log('💬 第一条消息字段:', Object.keys(msgs[0]));
  }
  return msgs;
});

const canLoadMore = computed(() => {
  return chatStore.canLoadMoreMessages;
});

const canSendMessage = computed(() => {
  return (messageInput.value.trim() || selectedFile.value) && activeChatId.value;
});

const filteredChatList = computed(() => {
  if (!searchQuery.value.trim()) return chatList.value;
  const query = searchQuery.value.toLowerCase();
  return chatList.value.filter(chat =>
    chat.targetInfo?.name?.toLowerCase().includes(query)
  );
});

const filteredContacts = computed(() => {
  if (!contactSearchQuery.value.trim()) return contacts.value;
  const query = contactSearchQuery.value.toLowerCase();
  return contacts.value.filter(contact =>
    contact.username?.toLowerCase().includes(query)
  );
});

const connectionStatus = computed(() => chatStore.connectionStatus);

// 方法
const loadChatList = async () => {
  try {
    const response = await api.chat.getChatList();
    if (response.code === 0) {
      chatList.value = response.data || [];
    }
  } catch (error) {
    console.error('加载聊天列表失败:', error);
  }
};

const loadContacts = async () => {
  isLoadingContacts.value = true;
  try {
    const response = await api.contact.getContacts('ACCEPTED');
    if (response.code === 0) {
      contacts.value = response.data || [];
    }
  } catch (error) {
    console.error('加载联系人失败:', error);
  } finally {
    isLoadingContacts.value = false;
  }
};

const selectChat = async (chat) => {
  if (activeChatId.value === chat.id) return;
  
  activeChatId.value = chat.id;
  chatStore.setActiveChat(chat);
  isLoadingMessages.value = true;

  try {
    await chatStore.fetchMessagesForChat(chat.id);
    await nextTick();
    scrollToBottom();
  } catch (error) {
    console.error('加载消息失败:', error);
  } finally {
    isLoadingMessages.value = false;
  }
};

const createNewChat = async (targetId) => {
  try {
    const response = await api.chat.createChat({ targetId });
    if (response.code === 0) {
      showNewChatDialog.value = false;
      await loadChatList();
      
      const newChat = chatList.value.find(c => c.targetId === targetId || c.id === response.data.id);
      if (newChat) {
        await selectChat(newChat);
      }
    }
  } catch (error) {
    console.error('创建聊天失败:', error);
    alert('创建聊天失败，请稍后重试');
  }
};

const sendMessage = async () => {
  if (!canSendMessage.value) return;

  const content = messageInput.value.trim();
  const file = selectedFile.value;
  
  messageInput.value = '';
  selectedFile.value = null;

  try {
    if (chatStore.isConnected) {
      console.log('📤 通过WebSocket发送消息...');
      // 通过WebSocket发送
      await chatStore.sendMessage(
        content || '[文件]',
        activeChatId.value,
        'PRIVATE',
        file ? 2 : 1
      );
    } else {
      console.log('📤 WebSocket未连接，使用HTTP发送消息...');
      // 降级到HTTP
      const messageData = {
        content: content || '[文件]',
        messageType: file ? 2 : 1
      };
      const response = await api.chat.sendMessage(activeChatId.value, messageData);
      console.log('📨 HTTP发送响应:', response);
      
      if (response.code === 0) {
        console.log('✅ 消息发送成功（HTTP）');
        console.log('📥 重新加载消息列表...');
        // 重新加载消息列表以显示新消息
        await chatStore.fetchMessagesForChat(activeChatId.value);
        console.log('📋 当前消息数量:', messages.value.length);
      } else {
        throw new Error(response.message || '发送失败');
      }
    }
    
    await nextTick();
    scrollToBottom();
  } catch (error) {
    console.error('❌ 发送消息失败:', error);
    alert('发送消息失败: ' + (error.message || '未知错误'));
    messageInput.value = content;
    selectedFile.value = file;
  }
};

const recallMessage = async (message) => {
  if (!confirm('确定要撤回这条消息吗？')) return;

  try {
    const response = await api.message.recall({ msgId: message.id });
    if (response.code === 0) {
      message.isRecalled = 1;
    }
  } catch (error) {
    console.error('撤回消息失败:', error);
    alert('撤回消息失败');
  }
};

const loadMoreMessages = async () => {
  if (!canLoadMore.value) return;
  
  try {
    await chatStore.loadMoreMessages();
  } catch (error) {
    console.error('加载更多消息失败:', error);
  }
};

const showReactionPicker = (message) => {
  currentReactionMessage.value = message;
  showReactionPickerDialog.value = true;
};

const addReaction = async (message, emoji) => {
  showReactionPickerDialog.value = false;
  
  try {
    await api.chat.addReaction(message.id, emoji);
    
    if (!message.reactions) {
      message.reactions = [];
    }
    
    const existingReaction = message.reactions.find(r => r.emoji === emoji);
    if (existingReaction) {
      existingReaction.count++;
    } else {
      message.reactions.push({ emoji, count: 1 });
    }
  } catch (error) {
    console.error('添加反应失败:', error);
  }
};

const toggleReaction = async (message, emoji) => {
  try {
    await api.chat.addReaction(message.id, emoji);
  } catch (error) {
    console.error('切换反应失败:', error);
  }
};

const handleKeyDown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    sendMessage();
  }
};

const handleTyping = () => {
  if (!activeChatId.value) return;
  
  chatStore.sendTypingIndicator(activeChatId.value, true);
  
  if (typingTimeout.value) {
    clearTimeout(typingTimeout.value);
  }
  
  typingTimeout.value = setTimeout(() => {
    chatStore.sendTypingIndicator(activeChatId.value, false);
  }, 3000);
};

const triggerFileUpload = () => {
  fileInputRef.value?.click();
};

const handleFileSelect = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  if (file.size > 10 * 1024 * 1024) {
    alert('文件大小不能超过10MB');
    return;
  }
  
  selectedFile.value = file;
};

const clearFileSelection = () => {
  selectedFile.value = null;
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const downloadFile = (fileData) => {
  if (!fileData?.fileUrl) return;
  window.open(fileData.fileUrl, '_blank');
};

const insertEmoji = (emoji) => {
  messageInput.value += emoji;
  showEmojiPicker.value = false;
  messageInputRef.value?.focus();
};

const scrollToBottom = () => {
  if (messageContainerRef.value) {
    messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight;
  }
};

const shouldShowTimeDivider = (message, index) => {
  if (index === 0) return true;
  
  const prevMessage = messages.value[index - 1];
  const currentTime = new Date(message.timestamp);
  const prevTime = new Date(prevMessage.timestamp);
  
  return currentTime - prevTime > 5 * 60 * 1000;
};

const isUserOnline = (userId) => {
  return chatStore.onlineUsers.has(userId);
};

const viewUserProfile = () => {
  if (currentChat.value?.targetInfo?.id) {
    router.push(`/user/${currentChat.value.targetInfo.id}`);
  }
};

const confirmDeleteChat = async () => {
  if (!confirm('确定要删除这个聊天吗？')) return;
  
  try {
    await api.chat.deleteChat(activeChatId.value);
    chatList.value = chatList.value.filter(c => c.id !== activeChatId.value);
    activeChatId.value = null;
    showChatInfo.value = false;
  } catch (error) {
    console.error('删除聊天失败:', error);
    alert('删除聊天失败');
  }
};

const goToAddFriend = () => {
  showNewChatDialog.value = false;
  router.push('/search?type=user');
};

// 格式化函数
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

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

const formatMessageDate = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const formatLastMessage = (lastMessage) => {
  if (!lastMessage) return '暂无消息';
  
  // 如果是字符串
  if (typeof lastMessage === 'string') {
    // 检查是否是Java对象的toString格式: TextMessageContent(content=xxx, ...)
    const match = lastMessage.match(/content=([^,)]+)/);
    if (match && match[1]) {
      const content = match[1].trim();
      return content.length > 10 ? content.substring(0, 10) + '...' : content;
    }
    
    // 普通字符串
    return lastMessage.length > 10 ? lastMessage.substring(0, 10) + '...' : lastMessage;
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
      return content.length > 10 ? content.substring(0, 10) + '...' : content;
    }
  }
  
  return '暂无消息';
};

// 重新连接WebSocket
const reconnectWebSocket = () => {
  console.log('🔄 手动重新连接WebSocket...');
  chatStore.disconnectWebSocket();
  setTimeout(() => {
    chatStore.connectWebSocket();
  }, 1000);
};

// 生命周期
onMounted(async () => {
  console.log('📱 ChatPage mounted');
  console.log('🔌 开始连接WebSocket...');
  chatStore.connectWebSocket();
  await loadChatList();
  await loadContacts();
});

onUnmounted(() => {
  chatStore.disconnectWebSocket();
  chatStore.clearActiveChat();
  
  if (typingTimeout.value) {
    clearTimeout(typingTimeout.value);
  }
});

// 监听消息更新
watch(() => chatStore.messagesForCurrentChat, () => {
  nextTick(() => scrollToBottom());
}, { deep: true });

// 监听打字状态
watch(() => chatStore.isTypingInCurrentChat, (newVal) => {
  isTyping.value = newVal;
});
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  background: var(--apple-bg-primary, #fff);
  position: relative;
}

/* 左侧聊天列表 */
.chat-sidebar {
  width: 320px;
  background: var(--apple-bg-secondary, #f5f5f7);
  border-right: 1px solid var(--apple-border, #e0e0e0);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.chat-sidebar.collapsed {
  width: 80px;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--apple-border, #e0e0e0);
}

.sidebar-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.icon-btn:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

.search-box {
  padding: 12px 16px;
}

.search-box input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--apple-border, #e0e0e0);
  border-radius: 8px;
  font-size: 14px;
  background: var(--apple-bg-primary, #fff);
}

.chat-list {
  flex: 1;
  overflow-y: auto;
}

.empty-list {
  padding: 40px 20px;
  text-align: center;
}

.empty-list p {
  color: var(--apple-text-tertiary, #999);
  margin-bottom: 16px;
}

.primary-btn {
  padding: 8px 16px;
  background: var(--apple-blue, #007aff);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.primary-btn:hover {
  background: var(--apple-blue-hover, #0051d5);
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.chat-item:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

.chat-item.active {
  background: var(--apple-blue-light, #e3f2fd);
}

.chat-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
  flex-shrink: 0;
}

.chat-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.online-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: #34c759;
  border: 2px solid white;
  border-radius: 50%;
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.chat-name {
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-time {
  font-size: 12px;
  color: var(--apple-text-tertiary, #999);
}

.chat-preview-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-last-msg {
  font-size: 13px;
  color: var(--apple-text-secondary, #666);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.unread-badge {
  background: var(--apple-red, #ff3b30);
  color: white;
  border-radius: 10px;
  padding: 2px 8px;
  font-size: 12px;
  min-width: 20px;
  text-align: center;
  margin-left: 8px;
}

/* 右侧聊天窗口 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--apple-bg-primary, #fff);
}

.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--apple-text-tertiary, #999);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-chat h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: var(--apple-text-primary, #000);
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--apple-border, #e0e0e0);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--apple-bg-secondary, #f5f5f7);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-avatar-small {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
}

.chat-avatar-small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.chat-title-info {
  display: flex;
  flex-direction: column;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
}

.chat-status {
  font-size: 12px;
  color: var(--apple-text-tertiary, #999);
}

.header-right {
  display: flex;
  gap: 8px;
}

/* 消息区域 */
.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
  background: var(--apple-bg-primary, #fff);
}

.loading-indicator {
  text-align: center;
  padding: 16px;
  color: var(--apple-text-tertiary, #999);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.time-divider {
  text-align: center;
  font-size: 12px;
  color: var(--apple-text-tertiary, #999);
  margin: 8px 0;
}

.message-item {
  display: flex;
  gap: 12px;
  width: 100%;
  /* 对方消息：左对齐 */
}

.message-item.is-me {
  /* 我的消息：右对齐 */
  justify-content: flex-end;
}

.message-wrapper {
  display: flex;
  gap: 12px;
  max-width: 70%;
  /* 对方消息：从左到右（头像-内容） */
}

.message-item.is-me .message-wrapper {
  /* 我的消息：从右到左（内容-头像） */
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item.is-me .message-content-wrapper {
  align-items: flex-end;
}

.message-sender {
  font-size: 12px;
  color: var(--apple-text-tertiary, #999);
}

.message-bubble-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.message-item.is-me .message-bubble-container {
  flex-direction: row-reverse;
}

.message-bubble {
  background: var(--apple-bg-secondary, #f5f5f7);
  padding: 10px 14px;
  border-radius: 18px;
  word-break: break-word;
  position: relative;
}

.message-item.is-me .message-bubble {
  background: var(--apple-blue, #007aff);
  color: white;
}

.message-bubble.recalled {
  background: transparent;
  border: 1px dashed var(--apple-border, #e0e0e0);
  color: var(--apple-text-tertiary, #999);
  font-style: italic;
}

.recalled-text {
  display: flex;
  align-items: center;
  gap: 4px;
}

.file-message {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px;
}

.file-icon {
  font-size: 32px;
}

.file-info {
  flex: 1;
}

.file-name {
  font-weight: 500;
  margin-bottom: 2px;
}

.file-size {
  font-size: 12px;
  opacity: 0.7;
}

.download-btn {
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.message-status {
  font-size: 12px;
  margin-top: 4px;
}

.message-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-bubble-container:hover .message-actions {
  opacity: 1;
}

.action-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: var(--apple-bg-tertiary, #e8e8ed);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.action-btn:hover {
  background: var(--apple-bg-quaternary, #d1d1d6);
}

.message-reactions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.reaction-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: var(--apple-bg-secondary, #f5f5f7);
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
}

.reaction-item:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

.reaction-count {
  font-size: 12px;
  color: var(--apple-text-secondary, #666);
}

.message-time {
  font-size: 11px;
  color: var(--apple-text-tertiary, #999);
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}

.typing-dots {
  display: flex;
  gap: 4px;
}

.typing-dots span {
  width: 8px;
  height: 8px;
  background: var(--apple-text-tertiary, #999);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

.typing-text {
  font-size: 13px;
  color: var(--apple-text-tertiary, #999);
}

/* 输入区域 */
.input-area {
  border-top: 1px solid var(--apple-border, #e0e0e0);
  background: var(--apple-bg-secondary, #f5f5f7);
  padding: 12px 24px;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.toolbar-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: background 0.2s;
}

.toolbar-btn:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--apple-bg-primary, #fff);
  border-radius: 8px;
  margin-bottom: 8px;
}

.file-preview-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.file-icon {
  font-size: 20px;
}

.file-size {
  color: var(--apple-text-tertiary, #999);
  font-size: 12px;
}

.remove-file-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-file-btn:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

.input-box {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.input-box textarea {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--apple-border, #e0e0e0);
  border-radius: 18px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  max-height: 120px;
  background: var(--apple-bg-primary, #fff);
}

.send-btn {
  padding: 10px 20px;
  background: var(--apple-blue, #007aff);
  color: white;
  border: none;
  border-radius: 18px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: var(--apple-blue-hover, #0051d5);
}

.send-btn:disabled {
  background: var(--apple-bg-quaternary, #d1d1d6);
  cursor: not-allowed;
}

.emoji-picker {
  margin-top: 8px;
  padding: 12px;
  background: var(--apple-bg-primary, #fff);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
}

.emoji-item {
  font-size: 24px;
  cursor: pointer;
  text-align: center;
  padding: 4px;
  border-radius: 6px;
  transition: background 0.2s;
}

.emoji-item:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

/* 聊天信息侧边栏 */
.chat-info-sidebar {
  width: 300px;
  background: var(--apple-bg-secondary, #f5f5f7);
  border-left: 1px solid var(--apple-border, #e0e0e0);
  display: flex;
  flex-direction: column;
}

.info-header {
  padding: 16px;
  border-bottom: 1px solid var(--apple-border, #e0e0e0);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  font-size: 20px;
}

.close-btn:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

.info-content {
  padding: 24px;
  text-align: center;
}

.info-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto 16px;
}

.info-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-name {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 24px;
}

.info-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-btn {
  padding: 10px 16px;
  border: 1px solid var(--apple-border, #e0e0e0);
  background: var(--apple-bg-primary, #fff);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.info-btn:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

.info-btn.danger {
  color: var(--apple-red, #ff3b30);
  border-color: var(--apple-red, #ff3b30);
}

.info-btn.danger:hover {
  background: rgba(255, 59, 48, 0.1);
}

/* 模态框 */
.modal-overlay {
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

.modal-content {
  background: var(--apple-bg-primary, #fff);
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--apple-border, #e0e0e0);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
}

.search-contacts {
  margin-bottom: 16px;
}

.search-contacts input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--apple-border, #e0e0e0);
  border-radius: 8px;
  font-size: 14px;
}

.loading,
.empty-contacts {
  text-align: center;
  padding: 40px 20px;
  color: var(--apple-text-tertiary, #999);
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.contact-item:hover {
  background: var(--apple-bg-secondary, #f5f5f7);
}

.contact-item img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.contact-info {
  flex: 1;
}

.contact-name {
  font-weight: 500;
  margin-bottom: 2px;
}

.contact-status {
  font-size: 13px;
  color: var(--apple-text-tertiary, #999);
}

/* 反应选择器 */
.reaction-picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.reaction-picker {
  background: var(--apple-bg-primary, #fff);
  border-radius: 24px;
  padding: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  display: flex;
  gap: 8px;
}

.reaction-emoji-item {
  font-size: 28px;
  cursor: pointer;
  padding: 8px;
  border-radius: 12px;
  transition: transform 0.2s, background 0.2s;
}

.reaction-emoji-item:hover {
  background: var(--apple-bg-secondary, #f5f5f7);
  transform: scale(1.2);
}

/* 连接状态 */
.connection-status {
  position: fixed;
  top: 16px;
  right: 16px;
  padding: 8px 16px;
  background: var(--apple-bg-primary, #fff);
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  font-size: 13px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 8px;
}

.reconnect-btn {
  padding: 4px 12px;
  background: var(--apple-blue, #007aff);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.2s;
}

.reconnect-btn:hover {
  background: var(--apple-blue-hover, #0051d5);
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 100;
    transform: translateX(-100%);
    transition: transform 0.3s;
  }

  .chat-sidebar.show {
    transform: translateX(0);
  }

  .chat-info-sidebar {
    position: fixed;
    right: 0;
    top: 0;
    height: 100vh;
    z-index: 100;
  }
}

/* 滚动条样式 */
.chat-list::-webkit-scrollbar,
.message-container::-webkit-scrollbar {
  width: 6px;
}

.chat-list::-webkit-scrollbar-track,
.message-container::-webkit-scrollbar-track {
  background: transparent;
}

.chat-list::-webkit-scrollbar-thumb,
.message-container::-webkit-scrollbar-thumb {
  background: var(--apple-bg-quaternary, #d1d1d6);
  border-radius: 3px;
}

.chat-list::-webkit-scrollbar-thumb:hover,
.message-container::-webkit-scrollbar-thumb:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}
</style>
