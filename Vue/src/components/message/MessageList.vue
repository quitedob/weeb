<template>
  <div class="message-list-container" ref="messageListRef">
    <!-- 消息列表 -->
    <div
      v-for="message in messages"
      :key="message.id"
      class="message-item"
      :class="{ 'message-sent': isSentByMe(message), 'message-received': !isSentByMe(message) }"
    >
      <!-- 发送者信息（群聊时显示） -->
      <div
        v-if="showSenderInfo && !isSentByMe(message)"
        class="message-sender"
      >
        <strong>{{ message.senderName }}</strong>
        <span class="message-time">{{ formatTime(message.timestamp) }}</span>
      </div>

      <!-- 消息内容 -->
      <div class="message-content">
        <!-- 文本消息 -->
        <div v-if="message.type === 'text'" class="text-message">
          {{ message.content }}
        </div>

        <!-- 系统消息 -->
        <div v-else-if="message.type === 'system'" class="system-message">
          {{ message.content }}
        </div>

        <!-- 消息状态指示器 -->
        <div v-if="isSentByMe(message)" class="message-status">
          <span :class="getStatusClass(message.status)">
            {{ getStatusText(message.status) }}
          </span>
        </div>
      </div>

      <!-- 时间戳（私聊时显示在右侧） -->
      <div
        v-if="!showSenderInfo && isSentByMe(message)"
        class="message-time-right"
      >
        {{ formatTime(message.timestamp) }}
      </div>
    </div>

    <!-- 滚动到底部按钮 -->
    <div
      v-if="showScrollButton"
      class="scroll-to-bottom"
      @click="scrollToBottom"
    >
      ⬇️ 新消息
    </div>

    <!-- 加载更多按钮 -->
    <div
      v-if="hasMoreMessages && !loadingMore"
      class="load-more"
      @click="loadMoreMessages"
    >
      加载更多消息
    </div>

    <!-- 加载中提示 -->
    <div v-if="loadingMore" class="loading-more">
      加载中...
    </div>
  </div>
</template>

<script>
import { ref, watch, nextTick, onMounted } from 'vue'

export default {
  name: 'MessageList',
  props: {
    messages: {
      type: Array,
      default: () => []
    },
    currentUserId: {
      type: [String, Number],
      required: true
    },
    isGroupChat: {
      type: Boolean,
      default: false
    },
    loadingMore: {
      type: Boolean,
      default: false
    },
    hasMoreMessages: {
      type: Boolean,
      default: false
    }
  },
  emits: ['load-more'],
  setup(props, { emit }) {
    const messageListRef = ref(null)
    const showScrollButton = ref(false)
    let lastScrollHeight = 0

    // 判断消息是否由当前用户发送
    const isSentByMe = (message) => {
      return message.senderId === props.currentUserId
    }

    // 是否显示发送者信息（群聊时显示其他用户的名称）
    const showSenderInfo = computed(() => {
      return props.isGroupChat
    })

    // 格式化时间
    const formatTime = (timestamp) => {
      const date = new Date(timestamp)
      const now = new Date()

      // 今天
      if (date.toDateString() === now.toDateString()) {
        return date.toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit'
        })
      }

      // 昨天
      const yesterday = new Date(now)
      yesterday.setDate(yesterday.getDate() - 1)
      if (date.toDateString() === yesterday.toDateString()) {
        return '昨天 ' + date.toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit'
        })
      }

      // 更早
      return date.toLocaleDateString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }

    // 获取消息状态样式类
    const getStatusClass = (status) => {
      const statusMap = {
        'sending': 'status-sending',
        'sent': 'status-sent',
        'delivered': 'status-delivered',
        'read': 'status-read',
        'failed': 'status-failed'
      }
      return statusMap[status] || 'status-sent'
    }

    // 获取消息状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'sending': '发送中...',
        'sent': '已发送',
        'delivered': '已送达',
        'read': '已读',
        'failed': '发送失败'
      }
      return statusMap[status] || ''
    }

    // 滚动到底部
    const scrollToBottom = () => {
      nextTick(() => {
        if (messageListRef.value) {
          messageListRef.value.scrollTop = messageListRef.value.scrollHeight
          showScrollButton.value = false
        }
      })
    }

    // 检查是否显示滚动到底部按钮
    const checkScrollButton = () => {
      if (!messageListRef.value) return

      const { scrollTop, scrollHeight, clientHeight } = messageListRef.value
      const isAtBottom = scrollTop + clientHeight >= scrollHeight - 50
      showScrollButton.value = !isAtBottom
    }

    // 加载更多消息
    const loadMoreMessages = () => {
      // 保存当前滚动位置
      lastScrollHeight = messageListRef.value.scrollHeight
      emit('load-more')
    }

    // 恢复滚动位置
    const restoreScrollPosition = () => {
      nextTick(() => {
        if (messageListRef.value && lastScrollHeight > 0) {
          const newScrollHeight = messageListRef.value.scrollHeight
          const scrollDifference = newScrollHeight - lastScrollHeight
          messageListRef.value.scrollTop = scrollDifference
        }
      })
    }

    // 监听消息变化，自动滚动到底部
    watch(() => props.messages, () => {
      if (messageListRef.value) {
        const wasAtBottom = messageListRef.value.scrollTop +
                           messageListRef.value.clientHeight >=
                           messageListRef.value.scrollHeight - 50

        nextTick(() => {
          if (wasAtBottom || props.messages.length === 0) {
            scrollToBottom()
          } else {
            checkScrollButton()
          }
        })
      }
    }, { deep: true })

    // 监听加载更多完成，恢复滚动位置
    watch(() => props.loadingMore, (newVal) => {
      if (!newVal && lastScrollHeight > 0) {
        restoreScrollPosition()
      }
    })

    onMounted(() => {
      // 添加滚动监听
      if (messageListRef.value) {
        messageListRef.value.addEventListener('scroll', checkScrollButton)
      }

      // 初始滚动到底部
      scrollToBottom()
    })

    return {
      messageListRef,
      showScrollButton,
      isSentByMe,
      showSenderInfo,
      formatTime,
      getStatusClass,
      getStatusText,
      scrollToBottom,
      loadMoreMessages
    }
  }
}
</script>

<style scoped>
.message-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fa;
  position: relative;
}

.message-item {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
}

.message-sent {
  align-items: flex-end;
}

.message-received {
  align-items: flex-start;
}

.message-sender {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
  font-size: 12px;
  color: #666;
}

.message-time {
  font-size: 11px;
  color: #999;
}

.message-content {
  max-width: 70%;
  position: relative;
}

.text-message {
  background: white;
  padding: 10px 15px;
  border-radius: 18px;
  word-wrap: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message-sent .text-message {
  background: #007bff;
  color: white;
}

.message-received .text-message {
  background: white;
  color: #333;
}

.system-message {
  text-align: center;
  color: #666;
  font-size: 12px;
  margin: 10px 0;
  font-style: italic;
}

.message-status {
  font-size: 10px;
  color: #999;
  margin-top: 2px;
  text-align: right;
}

.status-sending { color: #ffc107; }
.status-sent { color: #6c757d; }
.status-delivered { color: #28a745; }
.status-read { color: #007bff; }
.status-failed { color: #dc3545; }

.message-time-right {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.scroll-to-bottom {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: #007bff;
  color: white;
  padding: 8px 15px;
  border-radius: 20px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  font-size: 12px;
  transition: background-color 0.2s;
}

.scroll-to-bottom:hover {
  background: #0056b3;
}

.load-more, .loading-more {
  text-align: center;
  padding: 10px;
  margin: 10px 0;
}

.load-more {
  background: #e9ecef;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.load-more:hover {
  background: #dee2e6;
}

.loading-more {
  color: #6c757d;
  font-style: italic;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .message-list-container {
    padding: 15px;
  }

  .message-content {
    max-width: 85%;
  }

  .text-message {
    padding: 8px 12px;
    font-size: 14px;
  }
}
</style>