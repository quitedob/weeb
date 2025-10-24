<template>
  <div class="message-thread" v-if="message">
    <div class="thread-header">
      <div class="thread-info">
        <span class="thread-count">{{ threadSummary.totalMessages }} 条回复</span>
        <span class="thread-participants">{{ threadSummary.participantCount }} 人参与</span>
      </div>
      <div class="thread-actions">
        <el-button
          size="small"
          type="primary"
          @click="toggleThreadExpanded"
          :icon="isExpanded ? 'ArrowUp' : 'ArrowDown'"
        >
          {{ isExpanded ? '收起' : '展开' }}
        </el-button>
        <el-button
          size="small"
          @click="showCreateThreadDialog"
          v-if="!message.threadId"
        >
          <el-icon><Plus /></el-icon>
          创建线程
        </el-button>
      </div>
    </div>

    <!-- 线程消息列表 -->
    <el-collapse-transition>
      <div v-show="isExpanded" class="thread-content">
        <!-- 父消息提示 -->
        <div class="parent-message-hint">
          <el-icon><ChatDotRound /></el-icon>
          <span>回复原消息</span>
        </div>

        <!-- 线程消息列表 -->
        <div class="thread-messages" ref="threadMessagesContainer">
          <div
            v-for="threadMessage in threadMessages"
            :key="threadMessage.id"
            class="thread-message-item"
            :class="{ 'own-message': isOwnMessage(threadMessage) }"
          >
            <div class="message-avatar">
              <el-avatar :size="32" :src="getUserAvatar(threadMessage.senderId)">
                {{ getInitial(threadMessage.senderName) }}
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="sender-name">{{ threadMessage.senderName }}</span>
                <span class="message-time">{{ formatTime(threadMessage.createdAt) }}</span>
              </div>
              <div class="message-text" v-html="formatMessageContent(threadMessage.content)"></div>

              <!-- 消息操作 -->
              <div class="message-actions">
                <el-button
                  size="small"
                  type="text"
                  @click="replyToThreadMessage(threadMessage)"
                  :disabled="!isOwnMessage(threadMessage)"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  回复
                </el-button>
                <el-dropdown @command="(command) => handleThreadMessageCommand(command, threadMessage)">
                  <el-button size="small" type="text">
                    <el-icon><More /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="copy">复制</el-dropdown-item>
                      <el-dropdown-item command="delete" v-if="isOwnMessage(threadMessage)">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMoreMessages" class="load-more">
          <el-button
            size="small"
            :loading="loading"
            @click="loadMoreMessages"
          >
            加载更多
          </el-button>
        </div>

        <!-- 无消息提示 -->
        <div v-if="!threadMessages.length && !loading" class="no-messages">
          <el-empty description="暂无回复" />
        </div>
      </div>
    </el-collapse-transition>

    <!-- 创建线程对话框 -->
    <el-dialog
      v-model="createThreadDialog.visible"
      title="创建消息线程"
      width="500px"
      destroy-on-close
    >
      <el-form :model="createThreadDialog.form" :rules="createThreadDialog.rules" ref="threadFormRef">
        <el-form-item label="回复内容" prop="content">
          <el-input
            v-model="createThreadDialog.form.content"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createThreadDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          @click="createThread"
          :loading="createThreadDialog.loading"
        >
          创建线程
        </el-button>
      </template>
    </el-dialog>

    <!-- 回复对话框 -->
    <el-dialog
      v-model="replyDialog.visible"
      title="回复消息"
      width="500px"
      destroy-on-close
    >
      <el-form :model="replyDialog.form" :rules="replyDialog.rules" ref="replyFormRef">
        <el-form-item label="回复内容" prop="content">
          <el-input
            v-model="replyDialog.form.content"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="replyDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          @click="submitReply"
          :loading="replyDialog.loading"
        >
          回复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowUp, ArrowDown, Plus, ChatDotRound, More
} from '@element-plus/icons-vue'
import { log } from '@/utils/logger'
import axiosInstance from '@/api/axiosInstance'
import { useAuthStore } from '@/stores/authStore'

// Props
const props = defineProps({
  message: {
    type: Object,
    required: true
  },
  autoExpand: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['thread-updated'])

// 响应式数据
const isExpanded = ref(props.autoExpand)
const threadMessages = ref([])
const threadSummary = ref({
  totalMessages: 0,
  participantCount: 0,
  lastReplyTime: null
})
const loading = ref(false)
const hasMoreMessages = ref(false)
const currentPage = ref(1)
const pageSize = 20

// 对话框状态
const createThreadDialog = reactive({
  visible: false,
  loading: false,
  form: {
    content: ''
  },
  rules: {
    content: [
      { required: true, message: '请输入回复内容', trigger: 'blur' },
      { min: 2, max: 500, message: '回复内容长度应在2-500个字符之间', trigger: 'blur' }
    ]
  }
})

const replyDialog = reactive({
  visible: false,
  loading: false,
  currentMessage: null,
  form: {
    content: ''
  },
  rules: {
    content: [
      { required: true, message: '请输入回复内容', trigger: 'blur' },
      { min: 2, max: 500, message: '回复内容长度应在2-500个字符之间', trigger: 'blur' }
    ]
  }
})

// 表单引用
const threadFormRef = ref(null)
const replyFormRef = ref(null)
const threadMessagesContainer = ref(null)

// 计算属性
const authStore = useAuthStore()

// 方法
const toggleThreadExpanded = () => {
  isExpanded.value = !isExpanded.value
  if (isExpanded.value && threadMessages.value.length === 0) {
    loadThreadMessages()
  }
}

const loadThreadMessages = async (resetPage = false) => {
  if (resetPage) {
    currentPage.value = 1
    threadMessages.value = []
    hasMoreMessages.value = true
  }

  if (loading.value || !hasMoreMessages.value) return

  loading.value = true
  try {
    const response = await axiosInstance.get(`/api/v1/message/thread/${props.message.id}`, {
      params: {
        page: currentPage.value,
        pageSize: pageSize
      }
    })

    if (response.data.code === 0) {
      const data = response.data.data
      if (resetPage) {
        threadMessages.value = data.list || []
      } else {
        threadMessages.value.push(...(data.list || []))
      }

      hasMoreMessages.value = data.list && data.list.length === pageSize
      currentPage.value++

      // 滚动到底部
      await nextTick()
      if (threadMessagesContainer.value) {
        threadMessagesContainer.value.scrollTop = threadMessagesContainer.value.scrollHeight
      }
    }
  } catch (error) {
    log.error('加载线程消息失败:', error)
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const loadMoreMessages = () => {
  loadThreadMessages(false)
}

const showCreateThreadDialog = () => {
  createThreadDialog.form.content = ''
  createThreadDialog.visible = true
}

const createThread = async () => {
  try {
    await threadFormRef.value.validate()

    createThreadDialog.loading = true
    const response = await axiosInstance.post('/api/v1/message/thread/create', {
      parentMessageId: props.message.id,
      content: createThreadDialog.form.content
    })

    if (response.data.code === 0) {
      ElMessage.success('线程创建成功')
      createThreadDialog.visible = false

      // 重新加载线程消息
      currentPage.value = 1
      threadMessages.value = []
      hasMoreMessages.value = true
      await loadThreadMessages(true)

      // 更新父消息
      emit('thread-updated', {
        messageId: props.message.id,
        threadId: response.data.data.threadId
      })
    } else {
      ElMessage.error(response.data.message || '创建线程失败')
    }
  } catch (error) {
    log.error('创建线程失败:', error)
    ElMessage.error('创建线程失败')
  } finally {
    createThreadDialog.loading = false
  }
}

const replyToThreadMessage = (threadMessage) => {
  replyDialog.currentMessage = threadMessage
  replyDialog.form.content = ''
  replyDialog.visible = true
}

const submitReply = async () => {
  try {
    await replyFormRef.value.validate()

    replyDialog.loading = true
    const response = await axiosInstance.post(
      `/api/v1/message/thread/${props.message.threadId}/reply`,
      {
        content: replyDialog.form.content
      }
    )

    if (response.data.code === 0) {
      ElMessage.success('回复成功')
      replyDialog.visible = false

      // 重新加载线程消息
      currentPage.value = 1
      threadMessages.value = []
      hasMoreMessages.value = true
      await loadThreadMessages(true)
    } else {
      ElMessage.error(response.data.message || '回复失败')
    }
  } catch (error) {
    log.error('回复失败:', error)
    ElMessage.error('回复失败')
  } finally {
    replyDialog.loading = false
  }
}

const handleThreadMessageCommand = (command, threadMessage) => {
  switch (command) {
    case 'copy':
      copyMessage(threadMessage)
      break
    case 'delete':
      deleteThreadMessage(threadMessage)
      break
  }
}

const copyMessage = (threadMessage) => {
  try {
    const content = threadMessage.content?.text || ''
    navigator.clipboard.writeText(content)
    ElMessage.success('消息已复制')
  } catch (error) {
    log.error('复制消息失败:', error)
    ElMessage.error('复制失败')
  }
}

const deleteThreadMessage = async (threadMessage) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条消息吗？此操作不可恢复。',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await axiosInstance.delete(`/api/v1/message/thread/${threadMessage.id}`)

    if (response.data.code === 0) {
      ElMessage.success('消息已删除')
      // 重新加载线程消息
      currentPage.value = 1
      threadMessages.value = []
      hasMoreMessages.value = true
      loadThreadMessages(true)
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      log.error('删除消息失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 工具方法
const isOwnMessage = (message) => {
  return message.senderId === authStore.userId
}

const getUserAvatar = (senderId) => {
  // 这里应该调用API获取用户头像
  return ''
}

const getInitial = (name) => {
  return name ? name.charAt(0).toUpperCase() : '?'
}

const formatTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatMessageContent = (content) => {
  if (!content) return ''

  // 简单的文本格式化，可以根据需要扩展
  let formattedText = content.text || ''

  // 处理@用户
  if (content.atUidList && content.atUidList.length > 0) {
    content.atUidList.forEach(userId => {
      formattedText = formattedText.replace(new RegExp(`@${userId}`, 'g'), `@用户${userId}`)
    })
  }

  return formattedText
}

// 监听props变化
watch(() => props.message.threadId, (newThreadId) => {
  if (newThreadId && isExpanded.value) {
    loadThreadMessages(true)
  }
}, { immediate: true })

// 生命周期
onMounted(() => {
  if (props.message.threadId && isExpanded.value) {
    loadThreadMessages(true)
  }

  // 加载线程摘要
  if (props.message.threadId) {
    loadThreadSummary()
  }
})

const loadThreadSummary = async () => {
  try {
    const response = await axiosInstance.get(`/api/v1/message/thread/${props.message.id}/summary`)
    if (response.data.code === 0) {
      threadSummary.value = response.data.data
    }
  } catch (error) {
    log.error('获取线程摘要失败:', error)
  }
}
</script>

<style scoped>
.message-thread {
  border-left: 3px solid var(--apple-blue);
  margin-left: var(--apple-spacing-lg);
  padding-left: var(--apple-spacing-lg);
  background-color: var(--apple-bg-secondary);
  border-radius: var(--apple-radius-medium);
  margin-top: var(--apple-spacing-sm);
}

.thread-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--apple-spacing-sm) 0;
  border-bottom: 1px solid var(--apple-border-secondary);
  margin-bottom: var(--apple-spacing-md);
}

.thread-info {
  display: flex;
  gap: var(--apple-spacing-lg);
}

.thread-count,
.thread-participants {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  padding: var(--apple-spacing-xs) var(--apple-spacing-sm);
  background-color: var(--apple-bg-tertiary);
  border-radius: var(--apple-radius-small);
}

.thread-actions {
  display: flex;
  gap: var(--apple-spacing-sm);
}

.thread-content {
  padding-top: var(--apple-spacing-md);
}

.parent-message-hint {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xs);
  margin-bottom: var(--apple-spacing-md);
  font-size: var(--apple-font-sm);
  color: var(--apple-text-tertiary);
}

.thread-messages {
  max-height: 400px;
  overflow-y: auto;
}

.thread-message-item {
  display: flex;
  gap: var(--apple-spacing-md);
  margin-bottom: var(--apple-spacing-md);
  padding: var(--apple-spacing-md);
  border-radius: var(--apple-radius-medium);
  transition: background-color 0.2s ease;
}

.thread-message-item:hover {
  background-color: var(--apple-bg-tertiary);
}

.thread-message-item.own-message {
  flex-direction: row-reverse;
}

.thread-message-item.own-message .message-content {
  background-color: var(--apple-blue);
  color: white;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--apple-spacing-xs);
}

.sender-name {
  font-weight: 600;
  color: var(--apple-text-primary);
  font-size: var(--apple-font-sm);
}

.message-time {
  font-size: var(--apple-font-xs);
  color: var(--apple-text-tertiary);
}

.own-message .sender-name,
.own-message .message-time {
  color: rgba(255, 255, 255, 0.8);
}

.message-text {
  margin-bottom: var(--apple-spacing-xs);
  line-height: 1.5;
  word-wrap: break-word;
  color: var(--apple-text-primary);
}

.own-message .message-text {
  color: white;
}

.message-actions {
  display: flex;
  gap: var(--apple-spacing-xs);
  margin-top: var(--apple-spacing-xs);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.thread-message-item:hover .message-actions {
  opacity: 1;
}

.load-more {
  text-align: center;
  padding: var(--apple-spacing-md) 0;
}

.no-messages {
  padding: var(--apple-spacing-xl) 0;
}

@media (max-width: 768px) {
  .message-thread {
    margin-left: var(--apple-spacing-md);
    padding-left: var(--apple-spacing-md);
  }

  .thread-header {
    flex-direction: column;
    gap: var(--apple-spacing-sm);
    align-items: flex-start;
  }

  .thread-actions {
    align-self: flex-end;
  }

  .thread-message-item {
    padding: var(--apple-spacing-sm);
  }
}
</style>