<template>
  <div class="message-thread-display">
    <!-- Parent Message -->
    <div v-if="parentMessage" class="parent-message apple-card" @click="toggleThread">
      <div class="thread-header">
        <div class="thread-info">
          <span class="thread-icon">💬</span>
          <span class="thread-count">{{ threadInfo.totalReplies }} 条回复</span>
          <span class="thread-time">{{ formatTime(parentMessage.timestamp) }}</span>
        </div>
        <button class="toggle-button" :class="{ 'expanded': isExpanded }">
          <span v-html="getIcon('ArrowDown')"></span>
        </button>
      </div>

      <div class="parent-content">
        <div class="message-author">{{ parentMessage.fromName || 'Unknown' }}</div>
        <div class="message-text">{{ parentMessage.content }}</div>
      </div>
    </div>

    <!-- Thread Messages -->
    <div v-if="isExpanded && threadMessages.length > 0" class="thread-messages">
      <div class="thread-divider">
        <span>{{ threadInfo.totalReplies }} 条回复</span>
      </div>

      <div
        v-for="message in threadMessages"
        :key="message.id"
        class="thread-message"
        :class="{ 'own-message': message.isFromMe }"
      >
        <div class="message-avatar">
          {{ getInitials(message.fromName || 'User') }}
        </div>

        <div class="message-content-wrapper">
          <div class="message-header">
            <span class="message-author">{{ message.fromName || 'Unknown' }}</span>
            <span class="message-time">{{ formatTime(message.timestamp) }}</span>
          </div>

          <div class="message-text">{{ message.content }}</div>

          <!-- Message Actions -->
          <div class="message-actions">
            <button class="action-button" @click="replyToMessage(message)">
              <span v-html="getIcon('Reply')"></span>
              回复
            </button>
            <button class="action-button" @click="reactToMessage(message)">
              <span v-html="getIcon('Star')"></span>
              反应
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Load More -->
    <div v-if="hasMoreMessages && isExpanded" class="load-more">
      <button class="apple-button apple-button-secondary" @click="loadMoreMessages" :disabled="loading">
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
    </div>

    <!-- Reply Input -->
    <div v-if="isExpanded && showReplyInput" class="reply-input-section">
      <div class="reply-to-info">
        回复给 {{ replyingToMessage?.fromName }}
      </div>
      <div class="reply-input-wrapper">
        <textarea
          v-model="replyText"
          class="apple-textarea reply-textarea"
          placeholder="输入回复内容..."
          @keydown.ctrl.enter="sendReply"
          @keydown.meta.enter="sendReply"
        ></textarea>
        <div class="reply-actions">
          <button class="apple-button apple-button-secondary" @click="cancelReply">
            取消
          </button>
          <button class="apple-button apple-button-primary" @click="sendReply" :disabled="!replyText.trim()">
            发送回复
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import Icons from '@/utils/appleIcons.js'
import appleMessage from '@/utils/appleMessage.js'

const props = defineProps({
  parentMessage: {
    type: Object,
    required: true
  },
  threadId: {
    type: [String, Number],
    required: true
  },
  chatId: {
    type: [String, Number],
    required: true
  }
})

const emit = defineEmits(['message-sent', 'reply-clicked'])

const chatStore = useChatStore()
const isExpanded = ref(false)
const threadMessages = ref([])
const loading = ref(false)
const currentPage = ref(1)
const hasMoreMessages = ref(true)
const showReplyInput = ref(false)
const replyingToMessage = ref(null)
const replyText = ref('')

const threadInfo = computed(() => {
  return {
    totalReplies: threadMessages.value.length,
    lastReply: threadMessages.value[threadMessages.value.length - 1],
    hasUnread: false // TODO: Implement unread logic
  }
})

const getIcon = (iconName) => {
  return Icons[iconName]?.render({ size: 14 }) || ''
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`

  return date.toLocaleDateString()
}

const getInitials = (name) => {
  if (!name) return '?'
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
}

const toggleThread = () => {
  isExpanded.value = !isExpanded.value
  if (isExpanded.value && threadMessages.value.length === 0) {
    loadThreadMessages()
  }
}

const loadThreadMessages = async () => {
  if (loading.value) return

  loading.value = true
  try {
    // Call backend API to get thread messages
    const response = await api.message.getThreadMessages(props.threadId, currentPage.value, 10)

    if (response.code === 200) {
      const newMessages = response.data.messages || []
      threadMessages.value.push(...newMessages)
      hasMoreMessages.value = response.data.hasMore || false
      currentPage.value++
    } else {
      appleMessage.error(response.message || '加载回复失败')
    }
  } catch (error) {
    console.error('Failed to load thread messages:', error)
    appleMessage.error('加载回复失败')
  } finally {
    loading.value = false
  }
}

const loadMoreMessages = () => {
  loadThreadMessages()
}

const replyToMessage = (message) => {
  replyingToMessage.value = message
  showReplyInput.value = true
  replyText.value = ''
}

const reactToMessage = (message) => {
  // TODO: Implement reaction functionality
  appleMessage.info('反应功能开发中')
}

const cancelReply = () => {
  showReplyInput.value = false
  replyingToMessage.value = null
  replyText.value = ''
}

const sendReply = async () => {
  if (!replyText.value.trim()) return

  try {
    const messageData = {
      content: replyText.value.trim(),
      threadId: props.threadId,
      replyToMessageId: replyingToMessage.value?.id || props.parentMessage.id,
      chatId: props.chatId,
      chatType: 'THREAD'
    }

    // Send message through WebSocket
    await chatStore.sendMessage(
      messageData.content,
      messageData.chatId,
      messageData.chatType,
      1
    )

    // Add to local thread messages immediately for better UX
    const newMessage = {
      id: Date.now(), // Temporary ID, will be updated by backend
      content: messageData.content,
      fromName: '我',
      fromId: chatStore.currentUser?.id,
      isFromMe: true,
      timestamp: new Date(),
      threadId: props.threadId,
      replyToMessageId: messageData.replyToMessageId
    }

    threadMessages.value.push(newMessage)

    // Clear input
    cancelReply()

    // Emit event for parent component
    emit('message-sent', newMessage)

    appleMessage.success('回复发送成功')
  } catch (error) {
    console.error('Failed to send reply:', error)
    appleMessage.error('回复发送失败')
  }
}

// Auto-expand if thread has new messages
watch(() => props.threadId, (newThreadId) => {
  if (newThreadId && threadMessages.value.length === 0) {
    isExpanded.value = true
    loadThreadMessages()
  }
}, { immediate: true })
</script>

<style scoped>
.message-thread-display {
  margin: 8px 0;
}

.parent-message {
  background: rgba(255, 255, 255, 0.9);
  border-left: 4px solid #007AFF;
  cursor: pointer;
  transition: all 0.2s ease;
}

.parent-message:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-1px);
}

.thread-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.thread-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.thread-icon {
  font-size: 14px;
}

.thread-count {
  font-weight: 500;
  color: #007AFF;
}

.thread-time {
  color: #999;
}

.toggle-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: transform 0.2s ease;
}

.toggle-button.expanded {
  transform: rotate(180deg);
}

.parent-content {
  margin-left: 8px;
}

.message-author {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.message-text {
  color: #666;
  line-height: 1.4;
}

.thread-messages {
  margin-left: 20px;
  border-left: 2px solid #e5e5e7;
  padding-left: 16px;
}

.thread-divider {
  text-align: center;
  margin: 16px 0;
  position: relative;
}

.thread-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: -16px;
  right: -16px;
  height: 1px;
  background: #e5e5e7;
}

.thread-divider span {
  background: rgba(255, 255, 255, 0.9);
  padding: 0 8px;
  font-size: 12px;
  color: #666;
}

.thread-message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.thread-message.own-message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #007AFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.message-content-wrapper {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.action-button {
  background: none;
  border: 1px solid #e5e5e7;
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s ease;
}

.action-button:hover {
  background: rgba(0, 122, 255, 0.1);
  border-color: #007AFF;
  color: #007AFF;
}

.load-more {
  text-align: center;
  margin: 16px 0;
}

.reply-input-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e5e7;
}

.reply-to-info {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.reply-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reply-textarea {
  min-height: 60px;
  resize: vertical;
  font-family: inherit;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.apple-textarea {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.4;
  transition: all 0.2s ease;
  width: 100%;
  box-sizing: border-box;
}

.apple-textarea:focus {
  outline: none;
  border-color: #007AFF;
  background: rgba(255, 255, 255, 0.95);
}

.apple-button {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.apple-button-primary {
  background: #007AFF;
  color: white;
  border-color: #007AFF;
}

.apple-button-primary:hover:not(:disabled) {
  background: #0056CC;
  border-color: #0056CC;
}

.apple-button-secondary {
  background: rgba(255, 255, 255, 0.9);
  color: #333;
  border-color: rgba(0, 0, 0, 0.1);
}

.apple-button-secondary:hover:not(:disabled) {
  background: rgba(0, 122, 255, 0.1);
  border-color: #007AFF;
  color: #007AFF;
}

.apple-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>