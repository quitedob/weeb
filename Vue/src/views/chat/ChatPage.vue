<template>
  <div class="chat-page">
    <!-- 左侧聊天列表 -->
    <div class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h2 v-if="!sidebarCollapsed">消息</h2>
        <div class="header-actions">
          <!-- ✅ 新建聊天按钮 - 主要操作 -->
          <button 
            @click="showNewChatDialog = true" 
            class="icon-btn new-chat-btn" 
            title="新建聊天"
            v-if="!sidebarCollapsed"
          >
            <span>➕</span>
          </button>
          <!-- ✅ 折叠/展开按钮 - 次要操作，分开放置 -->
          <button 
            @click="toggleSidebar" 
            class="icon-btn toggle-btn" 
            :title="sidebarCollapsed ? '展开侧边栏' : '折叠侧边栏'"
          >
            <span>{{ sidebarCollapsed ? '☰' : '◀' }}</span>
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
          :key="chat.sharedChatId || chat.shared_chat_id || chat.id"
          :class="['chat-item', { active: Number(activeChatId) === Number(chat.sharedChatId || chat.shared_chat_id) }]"
          @click="handleChatItemClick(chat)"
        >
          <div class="chat-avatar">
            <!-- 群聊显示群组图标 -->
            <img 
              :src="chat.type === 'GROUP' ? defaultGroupAvatar : defaultAvatar" 
              :alt="getChatName(chat)" 
            />
            <!-- 私聊显示在线状态 -->
            <span 
              v-if="chat.type === 'PRIVATE' && isUserOnline(chat.targetId)" 
              class="online-indicator"
            ></span>
            <!-- 群聊显示群组标识 -->
            <span v-if="chat.type === 'GROUP'" class="group-indicator">👥</span>
          </div>
          <div v-if="!sidebarCollapsed" class="chat-info">
            <div class="chat-header-row">
              <div class="chat-name">
                {{ getChatName(chat) }}
                <span v-if="chat.type === 'GROUP'" class="chat-type-badge">群聊</span>
              </div>
              <div class="chat-time">{{ formatChatTime(chat.updateTime) }}</div>
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
              <img 
                :src="currentChat?.type === 'GROUP' ? defaultGroupAvatar : defaultAvatar" 
                :alt="getChatName(currentChat)" 
              />
              <span 
                v-if="currentChat?.type === 'PRIVATE' && isUserOnline(currentChat?.targetId)" 
                class="online-indicator"
              ></span>
              <span v-if="currentChat?.type === 'GROUP'" class="group-indicator-small">👥</span>
            </div>
            <div class="chat-title-info">
              <div class="chat-title">
                {{ getChatName(currentChat) }}
                <span v-if="currentChat?.type === 'GROUP'" class="member-count">
                  ({{ groupMemberCount }}人)
                </span>
              </div>
              <div class="chat-status">
                <template v-if="currentChat?.type === 'GROUP'">
                  <span v-if="onlineGroupMembers > 0">{{ onlineGroupMembers }}人在线</span>
                  <span v-else>群聊</span>
                </template>
                <template v-else>
                  {{ isUserOnline(currentChat?.targetId) ? '🟢 在线' : '⚪ 离线' }}
                </template>
              </div>
            </div>
          </div>
          <div class="header-right">
            <button 
              v-if="currentChat?.type === 'GROUP'" 
              @click="showGroupMembers = !showGroupMembers" 
              class="icon-btn" 
              title="群成员"
            >
              <span>👥</span>
            </button>
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
          <img :src="currentChat?.type === 'GROUP' ? defaultGroupAvatar : defaultAvatar" :alt="getChatName(currentChat)" />
        </div>
        <div class="info-name">{{ getChatName(currentChat) }}</div>
        <div class="info-actions">
          <button @click="viewUserProfile" class="info-btn">查看资料</button>
          <button @click="confirmDeleteChat" class="info-btn danger">删除聊天</button>
        </div>
      </div>
    </div>

    <!-- ✅ 群成员侧边栏 -->
    <div v-if="showGroupMembers && currentChat?.type === 'GROUP'" class="group-members-sidebar">
      <div class="info-header">
        <h3>群成员 ({{ groupMemberCount }})</h3>
        <button @click="showGroupMembers = false" class="close-btn">✕</button>
      </div>
      <div class="members-content">
        <div v-if="groupMembers.length === 0" class="empty-members">
          <p>暂无群成员</p>
        </div>
        <div v-else class="member-list">
          <div
            v-for="member in groupMembers"
            :key="member.userId"
            class="member-item"
          >
            <div class="member-avatar">
              <img :src="member.avatar || defaultAvatar" :alt="member.username" />
              <span 
                v-if="chatStore.onlineUsers.has(member.userId)" 
                class="online-indicator"
              ></span>
            </div>
            <div class="member-info">
              <div class="member-name">
                {{ member.username }}
                <span v-if="member.role === 1" class="role-badge owner">群主</span>
                <span v-else-if="member.role === 2" class="role-badge admin">管理员</span>
              </div>
              <div class="member-status">
                <span v-if="chatStore.onlineUsers.has(member.userId)" class="status-online">🟢 在线</span>
                <span v-else class="status-offline">⚪ 离线</span>
              </div>
            </div>
          </div>
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
              :class="{ disabled: !contact.id || isNaN(Number(contact.id)) }"
            >
              <img :src="contact.avatar || defaultAvatar" :alt="contact.username" />
              <div class="contact-info">
                <div class="contact-name">{{ contact.username }}</div>
                <div class="contact-status">{{ contact.bio || '这个人很懒，什么都没写' }}</div>
                <div v-if="!contact.id || isNaN(Number(contact.id))" class="contact-warning">
                  ⚠️ 数据异常，无法创建聊天
                </div>
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
import { useRouter, useRoute } from 'vue-router';
import { useChatStore } from '@/stores/chatStore';
import { useAuthStore } from '@/stores/authStore';
import { ElMessage } from 'element-plus';
import api from '@/api';

const router = useRouter();
const route = useRoute();
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
const defaultGroupAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';

// ✅ 群聊相关数据
const showGroupMembers = ref(false);
const groupMembers = ref([]);
const groupMemberCount = computed(() => {
  if (currentChat.value?.type === 'GROUP') {
    return groupMembers.value.length || 0;
  }
  return 0;
});

const onlineGroupMembers = computed(() => {
  if (currentChat.value?.type === 'GROUP') {
    return groupMembers.value.filter(member => 
      chatStore.onlineUsers.has(member.userId)
    ).length;
  }
  return 0;
});

// 常用表情
const commonEmojis = ['😊', '😂', '❤️', '👍', '👎', '🎉', '😢', '😡', '🤔', '👏', '🙏', '💪', '🔥', '✨', '🎈'];
const reactionEmojis = ['👍', '❤️', '😂', '😮', '😢', '😡', '🎉', '🔥'];

// 计算属性
const currentChat = computed(() => {
  // ✅ 修复：使用sharedChatId匹配，而不是UUID
  return chatList.value.find(chat => {
    const chatSharedId = chat.sharedChatId || chat.shared_chat_id;
    return chatSharedId && Number(chatSharedId) === Number(activeChatId.value);
  });
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
  return chatList.value.filter(chat => {
    const chatName = getChatName(chat);
    return chatName?.toLowerCase().includes(query);
  });
});

const filteredContacts = computed(() => {
  if (!contactSearchQuery.value.trim()) return contacts.value;
  const query = contactSearchQuery.value.toLowerCase();
  return contacts.value.filter(contact =>
    contact.username?.toLowerCase().includes(query)
  );
});

const connectionStatus = computed(() => chatStore.connectionStatus);

// ✅ 处理聊天项点击事件（修复：使用sharedChatId）
const handleChatItemClick = (chat) => {
  console.log('🖱️ ChatItem被点击:', chat);
  console.log('🆔 当前activeChatId:', activeChatId.value);
  console.log('🔍 点击的chat对象字段:', Object.keys(chat));
  console.log('📋 当前聊天列表长度:', chatList.value.length);

  // ✅ 修复：使用sharedChatId而不是UUID
  const chatId = chat.sharedChatId || chat.shared_chat_id;
  console.log('🎯 提取的sharedChatId:', chatId);
  console.log('🔍 Chat对象完整信息:', {
    id: chat.id,
    sharedChatId: chat.sharedChatId,
    targetId: chat.targetId,
    type: chat.type
  });

  if (!chatId) {
    console.error('❌ 聊天对象缺少sharedChatId字段:', chat);
    console.error('🐛 BUG REPORT: Chat object missing sharedChatId', {
      chatObject: chat,
      availableFields: Object.keys(chat),
      timestamp: new Date().toISOString()
    });
    ElMessage.error('聊天对象缺少sharedChatId字段，请检查后端API');
    return;
  }

  // ✅ 修复：确保chatId是Number类型
  const normalizedChatId = Number(chatId);
  if (isNaN(normalizedChatId)) {
    console.error('❌ sharedChatId不是有效的数字:', chatId);
    ElMessage.error('无效的聊天ID');
    return;
  }

  // 调用selectChat方法
  try {
    selectChat(chat);
  } catch (error) {
    console.error('❌ selectChat方法执行失败:', error);
    console.error('🐛 BUG REPORT: selectChat failed', {
      error: error.message,
      stack: error.stack,
      chat: chat,
      timestamp: new Date().toISOString()
    });
    ElMessage.error('切换聊天失败: ' + error.message);
  }
};

// ✅ 测试方法：手动触发聊天切换
const testChatSwitch = () => {
  console.log('🧪 测试聊天切换功能');
  console.log('📋 当前聊天列表:', chatList.value);
  console.log('🔍 过滤后的聊天列表:', filteredChatList.value);

  if (chatList.value.length > 0) {
    const firstChat = chatList.value[0];
    console.log('🎯 选择第一个聊天:', firstChat);
    handleChatItemClick(firstChat);
  } else {
    console.log('❌ 聊天列表为空，无法测试');
    ElMessage.warning('聊天列表为空，请先加载聊天列表');
  }
};

// ✅ 检查页面状态
const checkPageStatus = () => {
  console.log('🔍 页面状态检查:');
  console.log('  - authStore.currentUser:', authStore.currentUser);
  console.log('  - chatStore.connectionStatus:', chatStore.connectionStatus);
  console.log('  - chatList.length:', chatList.value.length);
  console.log('  - activeChatId:', activeChatId.value);
  console.log('  - isLoadingMessages:', isLoadingMessages.value);
  console.log('  - searchQuery:', searchQuery.value);

  // 检查是否有聊天数据
  if (chatList.value.length === 0) {
    console.log('⚠️ 聊天列表为空，尝试重新加载...');
    loadChatList();
  }
};

// 方法
const loadChatList = async () => {
  try {
    console.log('📥 开始加载聊天列表...');
    const response = await api.chat.getChatList();
    console.log('📨 聊天列表API响应:', response);

    if (response.code === 0) {
      // ✅ 处理不同的响应结构
      let list = [];
      if (Array.isArray(response.data)) {
        list = response.data;
      } else if (response.data?.data && Array.isArray(response.data.data)) {
        list = response.data.data;
      } else if (response.data?.list && Array.isArray(response.data.list)) {
        list = response.data.list;
      }

      // ✅ 标准化聊天对象，确保必要字段存在
      chatList.value = list.map(chat => {
        console.log('🔄 标准化聊天对象 - 原始:', chat);
        const normalized = {
          ...chat,
          // ✅ 确保sharedChatId存在（这是最重要的字段）
          sharedChatId: chat.sharedChatId || chat.shared_chat_id,
          // 确保ID存在（ChatList模型中id是String类型）
          id: chat.id || chat.chatId || chat.chat_id,
          type: chat.type || (chat.groupId ? 'GROUP' : 'PRIVATE'),
          targetId: chat.targetId || chat.target_user_id,
          name: chat.name || chat.groupName || chat.targetInfo || '未知聊天',
          lastMessage: chat.lastMessage || chat.latest_message,
          updateTime: chat.updateTime || chat.last_message_time || chat.createTime,
          unreadCount: chat.unreadCount || 0
        };
        console.log('🔄 标准化后:', normalized);
        return normalized;
      });

      console.log('✅ 聊天列表加载成功:', chatList.value.length, '个会话');

      // 打印第一个会话的结构以便调试
      if (chatList.value.length > 0) {
        console.log('📋 第一个会话原始结构:', list[0]);
        console.log('📋 第一个会话标准化结构:', chatList.value[0]);
      }
    } else {
      console.error('❌ 聊天列表API返回错误:', response);
      ElMessage.error(response.message || '获取聊天列表失败');
    }
  } catch (error) {
    console.error('❌ 加载聊天列表失败:', error);
    ElMessage.error('加载聊天列表失败: ' + (error.message || '网络错误'));
  }
};

const loadContacts = async () => {
  isLoadingContacts.value = true;
  try {
    const response = await api.contact.getContacts('ACCEPTED');
    if (response.code === 0) {
      // 过滤掉无效的联系人数据
      const validContacts = (response.data || []).filter(contact => {
        const isValid = contact && contact.id && !isNaN(Number(contact.id)) && String(contact.id).indexOf('_') === -1;
        if (!isValid) {
          console.warn('⚠️ 发现无效联系人数据，已过滤:', contact);
        }
        return isValid;
      });

      console.log('✅ 联系人加载成功，有效联系人数量:', validContacts.length);
      contacts.value = validContacts;
    }
  } catch (error) {
    console.error('加载联系人失败:', error);
  } finally {
    isLoadingContacts.value = false;
  }
};

const selectChat = async (chat) => {
  console.log('🎯 选择聊天:', chat);

  if (!chat) {
    console.error('❌ 聊天对象为空:', chat);
    console.error('🐛 BUG REPORT: selectChat called with null/undefined', {
      timestamp: new Date().toISOString()
    });
    ElMessage.error('聊天对象为空');
    return;
  }

  // ✅ 修复：使用sharedChatId作为主键
  const chatId = chat.sharedChatId || chat.shared_chat_id;
  if (!chatId) {
    console.error('❌ 聊天sharedChatId不存在:', chat);
    console.error('🐛 BUG REPORT: Chat missing sharedChatId', {
      chat: chat,
      availableFields: Object.keys(chat),
      timestamp: new Date().toISOString(),
      suggestion: 'Check backend API - GET /api/chats should return sharedChatId field'
    });
    ElMessage.error('聊天sharedChatId不存在，请检查后端API');
    return;
  }

  // ✅ 修复：标准化为Number (Long类型)
  const normalizedChatId = Number(chatId);
  const currentActiveId = Number(activeChatId.value || 0);

  if (currentActiveId === normalizedChatId) {
    console.log('⚠️ 已经在当前聊天中，跳过切换');
    return;
  }

  console.log('📝 切换聊天 (使用sharedChatId):', currentActiveId, '->', normalizedChatId);
  console.log('🔍 Chat详细信息:', {
    uuid: chat.id,
    sharedChatId: normalizedChatId,
    targetId: chat.targetId,
    type: chat.type
  });

  // ✅ 修复：更新活跃聊天ID为sharedChatId (Number)
  activeChatId.value = normalizedChatId;

  // ✅ 标准化chat对象，确保必要字段存在
  const normalizedChat = {
    ...chat,
    id: normalizedChatId, // ✅ 使用sharedChatId作为id
    sharedChatId: normalizedChatId, // ✅ 确保sharedChatId存在
    type: chat.type || 'PRIVATE', // 默认为私聊
    targetId: chat.targetId || chat.target_user_id,
    name: chat.name || chat.groupName || getChatName(chat) || '未知聊天'
  };

  console.log('📦 标准化后的聊天对象:', normalizedChat);

  // 设置活跃聊天
  chatStore.setActiveChat(normalizedChat);
  isLoadingMessages.value = true;

  try {
    console.log('📥 开始加载消息: sharedChatId=', normalizedChatId);
    await chatStore.fetchMessagesForChat(normalizedChatId);
    console.log('✅ 消息加载完成，消息数量:', messages.value.length);

    // ✅ 如果是群聊，加载群成员
    if (normalizedChat.type === 'GROUP') {
      const groupId = chat.groupId || chat.group_id || normalizedChatId;
      console.log('👥 加载群成员: groupId=', groupId);
      await loadGroupMembers(groupId);
    }

    await nextTick();
    scrollToBottom();
    console.log('✅ 聊天切换完成');
    ElMessage.success('聊天切换成功');
  } catch (error) {
    console.error('❌ 加载消息失败:', error);
    console.error('🐛 BUG REPORT: fetchMessagesForChat failed', {
      error: error.message,
      stack: error.stack,
      sharedChatId: normalizedChatId,
      chat: chat,
      timestamp: new Date().toISOString()
    });
    ElMessage.error('加载消息失败: ' + (error.message || '未知错误'));
    // 如果失败，重置活跃聊天
    activeChatId.value = null;
    chatStore.clearActiveChat();
  } finally {
    isLoadingMessages.value = false;
  }
};

// ✅ 加载群成员列表
const loadGroupMembers = async (groupId) => {
  try {
    // 调用群组成员API
    const response = await api.group.getMembers(groupId);
    if (response.code === 0) {
      groupMembers.value = response.data || [];
      console.log('✅ 群成员加载成功:', groupMembers.value.length);
    }
  } catch (error) {
    console.error('❌ 加载群成员失败:', error);
    groupMembers.value = [];
  }
};

const createNewChat = async (targetId) => {
  try {
    // 确保targetId是有效的数字
    if (!targetId || targetId === '' || isNaN(Number(targetId))) {
      console.error('❌ 无效的targetId:', targetId);
      ElMessage.error('无效的联系人ID，无法创建聊天');
      return;
    }

    console.log('🚀 创建聊天，targetId:', targetId, 'type:', typeof targetId);

    const response = await api.chat.createChat({ targetId: String(targetId) });
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
    ElMessage.error('创建聊天失败: ' + (error.message || '未知错误'));
  }
};

const sendMessage = async () => {
  if (!canSendMessage.value) return;

  const content = messageInput.value.trim();
  const file = selectedFile.value;

  messageInput.value = '';
  selectedFile.value = null;

  try {
    // 构造符合后端TextMessageContent结构的消息内容
    const textMessageContent = {
      content: content || '[文件]',
      contentType: 1, // TextContentType.TEXT.getCode()
      url: file ? file.name : null,
      atUidList: []
    };

    const messageData = {
      content: textMessageContent,
      messageType: file ? 2 : 1
    };

    if (chatStore.isConnected) {
      console.log('📤 通过WebSocket发送消息...');
      // 通过WebSocket发送
      // 获取当前聊天的类型
      const currentChatType = chatStore.activeChatSession?.type || 'PRIVATE';

      await chatStore.sendMessage(
        content || '[文件]',
        activeChatId.value,
        currentChatType,
        file ? 2 : 1
      );
    } else {
      console.log('📤 WebSocket未连接，使用HTTP发送消息...');
      // 降级到HTTP - 使用正确的消息结构
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
    const response = await api.chat.recallMessage(message.id);
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
  // 优先使用chatStore中的在线用户缓存
  if (chatStore.onlineUsers.has(userId)) {
    return true;
  }

  // 如果缓存中没有，可以通过API检查（可选）
  // api.chat.checkUserOnline(userId).then(response => {
  //   if (response.code === 0 && response.data) {
  //     chatStore.addOnlineUser(userId);
  //   }
  // });

  return chatStore.onlineUsers.has(userId);
};

const viewUserProfile = () => {
  if (currentChat.value?.targetId) {
    router.push(`/user/${currentChat.value.targetId}`);
  }
};

// ✅ 获取聊天名称的辅助函数
const getChatName = (chat) => {
  if (!chat) return '未知';
  
  // 如果targetInfo是字符串，直接返回
  if (typeof chat.targetInfo === 'string') {
    return chat.targetInfo || '未知';
  }
  
  // 如果targetInfo是对象，提取name
  if (typeof chat.targetInfo === 'object' && chat.targetInfo) {
    return chat.targetInfo.name || chat.targetInfo.username || '未知';
  }
  
  return '未知';
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
// ✅ 切换侧边栏显示/隐藏
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value;
};

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
  console.log('🔍 用户状态:', authStore.currentUser);
  console.log('🔌 开始连接WebSocket...');

  // 先检查用户是否已登录
  if (!authStore.currentUser || !authStore.currentUser.id) {
    console.error('❌ 用户未登录，无法加载聊天列表');
    ElMessage.error('请先登录');
    router.push('/login');
    return;
  }

  chatStore.connectWebSocket();

  // 等待聊天列表加载
  console.log('📥 开始加载聊天列表...');
  await loadChatList();
  await loadContacts();

  // 延迟检查页面状态，确保数据已加载
  setTimeout(() => {
    console.log('🔍 检查页面状态...');
    checkPageStatus();
  }, 1000);

  // ✅ 处理路由参数，自动打开指定聊天
  if (route.params.type && route.params.id) {
    const chatType = route.params.type.toUpperCase(); // PRIVATE or GROUP
    const chatId = route.params.id;

    console.log('🔗 从路由参数打开聊天:', chatType, chatId);

    // 查找对应的聊天会话
    const targetChat = chatList.value.find(chat =>
      chat.id === chatId || chat.id === String(chatId)
    );

    if (targetChat) {
      console.log('✅ 找到目标聊天，自动打开:', targetChat);
      await selectChat(targetChat);
    } else {
      console.warn('⚠️ 聊天会话不在列表中，尝试创建或加载');
      // 如果是私聊且聊天列表中没有，尝试创建新会话
      if (chatType === 'PRIVATE') {
        try {
          const response = await api.chat.createChat({ targetId: chatId });
          if (response.code === 0 && response.data) {
            chatList.value.unshift(response.data);
            await selectChat(response.data);
          }
        } catch (error) {
          console.error('❌ 创建聊天会话失败:', error);
        }
      }
    }
  } else {
    console.log('ℹ️ 没有路由参数，不自动打开聊天');
  }
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

// ✅ 监听路由参数变化，支持聊天切换
watch(() => route.params, async (newParams) => {
  if (newParams.type && newParams.id) {
    const chatType = newParams.type.toUpperCase();
    const chatId = newParams.id;
    
    console.log('🔄 路由参数变化，切换聊天:', chatType, chatId);
    
    // 如果已经是当前聊天，不重复加载
    if (activeChatId.value === chatId || activeChatId.value === String(chatId)) {
      console.log('⚠️ 已经在当前聊天中，跳过');
      return;
    }
    
    const targetChat = chatList.value.find(chat => 
      chat.id === chatId || chat.id === String(chatId)
    );
    
    if (targetChat) {
      await selectChat(targetChat);
    } else if (chatType === 'PRIVATE') {
      // 尝试创建新的私聊会话
      try {
        const response = await api.chat.createChat({ targetId: chatId });
        if (response.code === 0 && response.data) {
          chatList.value.unshift(response.data);
          await selectChat(response.data);
        }
      } catch (error) {
        console.error('❌ 创建聊天会话失败:', error);
      }
    }
  }
}, { deep: true });

// ✅ 将调试方法暴露到全局，便于在控制台中测试
if (typeof window !== 'undefined') {
  window.debugChat = {
    checkStatus: checkPageStatus,
    testSwitch: testChatSwitch,
    selectFirstChat: () => {
      if (chatList.value.length > 0) {
        handleChatItemClick(chatList.value[0]);
      }
    },
    showChatList: () => {
      console.log('📋 当前聊天列表:', chatList.value);
      console.log('🔍 过滤后的列表:', filteredChatList.value);
    },
    getActiveChat: () => {
      console.log('🎯 当前活跃聊天:', activeChatId.value);
      console.log('📦 当前聊天对象:', chatStore.activeChatSession);
    }
  };

  console.log('🔧 调试方法已暴露到 window.debugChat，可在控制台中使用');
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  background: var(--apple-bg-primary, #fff);
  position: relative;
  overflow: hidden;
}

/* 左侧聊天列表 */
.chat-sidebar {
  width: 320px;
  min-width: 320px;
  background: var(--apple-bg-secondary, #f5f5f7);
  border-right: 1px solid var(--apple-border, #e0e0e0);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.chat-sidebar.collapsed {
  width: 80px;
  min-width: 80px;
}

/* ✅ 右侧聊天主区域 - 自动填充剩余空间 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0; /* 防止flex子元素溢出 */
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
  align-items: center;
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
  transition: all 0.2s;
  font-size: 18px;
}

.icon-btn:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
  transform: scale(1.05);
}

/* ✅ 新建聊天按钮 - 主要操作，使用蓝色 */
.icon-btn.new-chat-btn {
  background: var(--apple-blue, #007aff);
  color: white;
}

.icon-btn.new-chat-btn:hover {
  background: var(--apple-blue-hover, #0051d5);
}

/* ✅ 折叠按钮 - 次要操作，灰色 */
.icon-btn.toggle-btn {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

.icon-btn.toggle-btn:hover {
  background: var(--apple-border, #d0d0d0);
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

.contact-warning {
  font-size: 12px;
  color: var(--apple-red, #ff3b30);
  margin-top: 4px;
}

.contact-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.contact-item.disabled:hover {
  background: transparent;
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


/* ✅ 群聊相关样式 */
.group-indicator {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 18px;
  height: 18px;
  background: var(--apple-blue, #007aff);
  border: 2px solid white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}

.group-indicator-small {
  position: absolute;
  bottom: 0;
  right: 0;
  font-size: 12px;
}

.chat-type-badge {
  display: inline-block;
  padding: 2px 6px;
  background: var(--apple-blue-light, #e3f2fd);
  color: var(--apple-blue, #007aff);
  border-radius: 4px;
  font-size: 10px;
  margin-left: 6px;
  font-weight: normal;
}

.member-count {
  font-size: 12px;
  color: var(--apple-text-tertiary, #999);
  font-weight: normal;
  margin-left: 4px;
}

/* 群成员侧边栏 */
.group-members-sidebar {
  width: 300px;
  background: var(--apple-bg-secondary, #f5f5f7);
  border-left: 1px solid var(--apple-border, #e0e0e0);
  display: flex;
  flex-direction: column;
  max-height: 100vh;
  overflow: hidden;
}

.members-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.empty-members {
  text-align: center;
  padding: 40px 20px;
  color: var(--apple-text-tertiary, #999);
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--apple-bg-primary, #fff);
  border-radius: 8px;
  transition: background 0.2s;
}

.member-item:hover {
  background: var(--apple-bg-tertiary, #e8e8ed);
}

.member-avatar {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.member-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-weight: 500;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.role-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: normal;
}

.role-badge.owner {
  background: var(--apple-red-light, #ffebee);
  color: var(--apple-red, #ff3b30);
}

.role-badge.admin {
  background: var(--apple-blue-light, #e3f2fd);
  color: var(--apple-blue, #007aff);
}

.member-status {
  font-size: 12px;
}

.status-online {
  color: #34c759;
}

.status-offline {
  color: var(--apple-text-tertiary, #999);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .group-members-sidebar {
    position: fixed;
    right: 0;
    top: 0;
    height: 100vh;
    z-index: 100;
  }
}
