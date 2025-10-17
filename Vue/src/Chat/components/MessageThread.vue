<template>
  <div class="message-thread">
    <!-- 线程头部信息 -->
    <div class="thread-header">
      <div class="thread-info">
        <div class="thread-title">
          <span class="thread-icon">💬</span>
          消息线索
        </div>
        <div class="thread-stats">
          <span class="reply-count">{{ replies.length }} 条回复</span>
          <span class="last-reply">最后回复: {{ formatTime(lastReplyTime) }}</span>
        </div>
      </div>
      <div class="thread-actions">
        <el-button size="small" @click="expandThread">
          {{ isExpanded ? '收起' : '展开' }}
        </el-button>
        <el-button size="small" type="primary" @click="replyToThread">
          回复线索
        </el-button>
      </div>
    </div>

    <!-- 主消息（折叠状态显示） -->
    <div v-if="!isExpanded" class="main-message-collapsed">
      <div class="message-preview">
        <div class="message-author">{{ mainMessage.fromUserName }}</div>
        <div class="message-content">{{ truncateMessage(mainMessage.msgContent, 100) }}</div>
        <div class="message-time">{{ formatTime(mainMessage.timestamp) }}</div>
      </div>
    </div>

    <!-- 线程详情（展开状态显示） -->
    <div v-if="isExpanded" class="thread-content">
      <!-- 主消息 -->
      <div class="main-message">
        <div class="message-avatar">
          <img :src="mainMessage.fromUserAvatar" :alt="mainMessage.fromUserName" />
        </div>
        <div class="message-body">
          <div class="message-header">
            <span class="message-author">{{ mainMessage.fromUserName }}</span>
            <span class="message-time">{{ formatTime(mainMessage.timestamp) }}</span>
          </div>
          <div class="message-content">{{ mainMessage.msgContent }}</div>
          <div class="message-actions">
            <el-button size="small" @click="replyToMessage(mainMessage)">回复</el-button>
            <el-button size="small" @click="quoteMessage(mainMessage)">引用</el-button>
          </div>
        </div>
      </div>

      <!-- 回复列表 -->
      <div class="thread-replies">
        <div
          v-for="reply in replies"
          :key="reply.id"
          class="reply-item"
        >
          <div class="reply-avatar">
            <img :src="reply.fromUserAvatar" :alt="reply.fromUserName" />
          </div>
          <div class="reply-body">
            <div class="reply-header">
              <span class="reply-author">{{ reply.fromUserName }}</span>
              <span class="reply-time">{{ formatTime(reply.timestamp) }}</span>
            </div>
            <div class="reply-content">{{ reply.msgContent }}</div>
            <div class="reply-actions">
              <el-button size="mini" @click="replyToMessage(reply)">回复</el-button>
              <el-button size="mini" @click="quoteMessage(reply)">引用</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 回复输入框 -->
      <div v-if="showReplyInput" class="thread-reply-input">
        <div class="reply-input-header">
          <span>回复给 {{ replyTarget?.fromUserName }}</span>
          <el-button size="mini" @click="cancelReply">取消</el-button>
        </div>
        <div class="reply-input-body">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="3"
            placeholder="请输入回复内容..."
            @keyup.enter="submitReply"
          />
          <div class="reply-input-actions">
            <el-button size="small" @click="insertEmoji">表情</el-button>
            <el-button type="primary" size="small" @click="submitReply" :disabled="!replyContent.trim()">
              发送回复
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 表情选择器弹窗 -->
    <div v-if="showEmojiPicker" class="emoji-picker-modal">
      <div class="emoji-picker">
        <div class="emoji-tabs">
          <button
            v-for="category in emojiCategories"
            :key="category.name"
            :class="{ active: activeEmojiCategory === category.name }"
            @click="activeEmojiCategory = category.name"
          >
            {{ category.icon }}
          </button>
        </div>
        <div class="emoji-grid">
          <button
            v-for="emoji in currentEmojis"
            :key="emoji"
            class="emoji-btn"
            @click="insertEmojiToReply(emoji)"
          >
            {{ emoji }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  mainMessage: {
    type: Object,
    required: true
  },
  replies: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['reply', 'quote', 'expand'])

const isExpanded = ref(false)
const showReplyInput = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')
const showEmojiPicker = ref(false)
const activeEmojiCategory = ref('recent')

// 表情分类数据
const emojiCategories = ref([
  { name: 'recent', icon: '🕐', emojis: ['😀', '😂', '😊', '👍', '❤️', '🎉'] },
  { name: 'smileys', icon: '😀', emojis: ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇'] },
  { name: 'gestures', icon: '👍', emojis: ['👍', '👎', '👌', '✌️', '🤞', '👏', '🙌', '🤝', '🙏'] },
  { name: 'hearts', icon: '❤️', emojis: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎'] }
])

const currentEmojis = computed(() => {
  const category = emojiCategories.value.find(c => c.name === activeEmojiCategory.value)
  return category ? category.emojis : []
})

const lastReplyTime = computed(() => {
  if (replies.value.length === 0) return props.mainMessage.timestamp
  return Math.max(...replies.value.map(r => new Date(r.timestamp)))
})

// 展开/收起线程
const expandThread = () => {
  isExpanded.value = !isExpanded.value
  emit('expand', { messageId: props.mainMessage.id, expanded: isExpanded.value })
}

// 回复线索
const replyToThread = () => {
  showReplyInput.value = true
  replyTarget.value = props.mainMessage
}

// 回复特定消息
const replyToMessage = (message) => {
  showReplyInput.value = true
  replyTarget.value = message
}

// 引用消息
const quoteMessage = (message) => {
  emit('quote', message)
}

// 取消回复
const cancelReply = () => {
  showReplyInput.value = false
  replyTarget.value = null
  replyContent.value = ''
}

// 插入表情到回复
const insertEmoji = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

// 插入表情到回复内容
const insertEmojiToReply = (emoji) => {
  replyContent.value += emoji
  showEmojiPicker.value = false
}

// 提交回复
const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  try {
    const replyData = {
      content: replyContent.value,
      replyToId: replyTarget.value.id,
      threadId: props.mainMessage.id
    }

    // 这里应该调用后端API发送回复
    // const response = await sendThreadReply(replyData)

    ElMessage.success('回复发送成功')
    replyContent.value = ''
    showReplyInput.value = false

    // 刷新线程数据
    // emit('refresh')
  } catch (error) {
    ElMessage.error('回复发送失败')
  }
}

// 截断消息内容
const truncateMessage = (content, maxLength) => {
  if (!content) return ''
  return content.length > maxLength ? content.substring(0, maxLength) + '...' : content
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

onMounted(() => {
  // 初始化时可能需要加载线程数据
})
</script>

<style scoped>
.message-thread {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 16px;
  overflow: hidden;
}

.thread-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.thread-info {
  flex: 1;
}

.thread-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 4px;
}

.thread-icon {
  font-size: 16px;
}

.thread-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #6c757d;
}

.thread-actions {
  display: flex;
  gap: 8px;
}

/* 主消息折叠状态 */
.main-message-collapsed {
  padding: 12px 16px;
  border-bottom: 1px solid #e9ecef;
}

.message-preview {
  display: flex;
  gap: 12px;
  align-items: center;
}

.message-author {
  font-weight: 500;
  color: #2c3e50;
  min-width: 80px;
}

.message-content {
  flex: 1;
  color: #495057;
}

.message-time {
  font-size: 12px;
  color: #6c757d;
  min-width: 80px;
  text-align: right;
}

/* 线程展开内容 */
.thread-content {
  padding: 0;
}

.main-message {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #e9ecef;
  background: #fafbfc;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-body {
  flex: 1;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-author {
  font-weight: 500;
  color: #2c3e50;
}

.message-time {
  font-size: 12px;
  color: #6c757d;
}

.message-content {
  color: #495057;
  line-height: 1.5;
  margin-bottom: 8px;
}

.message-actions {
  display: flex;
  gap: 8px;
}

/* 回复列表 */
.thread-replies {
  background: white;
}

.reply-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f3f4;
  transition: background-color 0.2s ease;
}

.reply-item:hover {
  background: #f8f9fa;
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.reply-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.reply-body {
  flex: 1;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.reply-author {
  font-weight: 500;
  color: #2c3e50;
  font-size: 14px;
}

.reply-time {
  font-size: 12px;
  color: #6c757d;
}

.reply-content {
  color: #495057;
  line-height: 1.4;
  font-size: 14px;
}

.reply-actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

/* 回复输入框 */
.thread-reply-input {
  padding: 16px;
  border-top: 1px solid #e9ecef;
  background: #fafbfc;
}

.reply-input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: #495057;
}

.reply-input-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 表情选择器 */
.emoji-picker-modal {
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

.emoji-picker {
  background: white;
  border-radius: 8px;
  padding: 16px;
  max-width: 300px;
  max-height: 400px;
  overflow: hidden;
}

.emoji-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  border-bottom: 1px solid #e9ecef;
  padding-bottom: 8px;
}

.emoji-tabs button {
  background: none;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 20px;
  transition: background-color 0.2s ease;
}

.emoji-tabs button.active {
  background: #f0f9ff;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.emoji-btn {
  background: none;
  border: none;
  padding: 6px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 20px;
  transition: background-color 0.2s ease;
}

.emoji-btn:hover {
  background: #f0f0f0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .message-thread {
    margin: 0 -12px 16px;
    border-radius: 0;
  }

  .thread-header {
    padding: 12px;
  }

  .main-message {
    padding: 12px;
  }

  .reply-item {
    padding: 10px 12px;
  }

  .thread-reply-input {
    padding: 12px;
  }
}
</style>
