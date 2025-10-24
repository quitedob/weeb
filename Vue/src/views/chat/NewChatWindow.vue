<template>
  <div class="chat-window">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="chat-info">
        <h2 class="chat-title">{{ chatTitle }}</h2>
        <div class="chat-status">
          <span v-if="isOnline" class="status-online">在线</span>
          <span v-else class="status-offline">离线</span>
        </div>
      </div>
      <div class="chat-actions">
        <button class="action-button" @click="toggleChatSettings" title="设置">
          ⚙️
        </button>
        <button class="action-button" @click="closeChat" title="关闭">
          ❌
        </button>
      </div>
    </div>

    <!-- 消息列表区域 -->
    <div class="chat-messages">
      <MessageList
        :messages="messages"
        :current-user-id="currentUserId"
        :is-group-chat="isGroupChat"
        :loading-more="loadingMore"
        :has-more-messages="hasMoreMessages"
        @load-more="loadMoreMessages"
      />
    </div>

    <!-- 消息输入区域 -->
    <div class="chat-input">
      <MessageInput
        :recipient-id="recipientId"
        :recipient-type="recipientType"
        @message-sent="handleMessageSent"
      />
    </div>

    <!-- 表情选择器 -->
    <EmojiSelector
      :visible="showEmojiSelector"
      :position="emojiPosition"
      @select="insertEmoji"
      @close="hideEmojiSelector"
    />

    <!-- 聊天设置弹窗 -->
    <div v-if="showSettings" class="settings-overlay" @click="hideSettings">
      <div class="settings-modal" @click.stop>
        <div class="settings-header">
          <h3>聊天设置</h3>
          <button class="close-button" @click="hideSettings">×</button>
        </div>
        <div class="settings-content">
          <div class="setting-item">
            <label>
              <input
                v-model="settings.enableNotifications"
                type="checkbox"
              />
              启用消息通知
            </label>
          </div>
          <div class="setting-item">
            <label>
              <input
                v-model="settings.enableSound"
                type="checkbox"
              />
              启用消息提示音
            </label>
          </div>
          <div class="setting-item">
            <label>
              <input
                v-model="settings.showTimestamp"
                type="checkbox"
              />
              显示时间戳
            </label>
          </div>
          <div class="setting-item">
            <label>字体大小</label>
            <select v-model="settings.fontSize">
              <option value="small">小</option>
              <option value="medium">中</option>
              <option value="large">大</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MessageList from '@/components/message/MessageList.vue'
import MessageInput from '@/components/message/MessageInput.vue'
import EmojiSelector from '@/components/message/EmojiSelector.vue'
import { useAuthStore } from '@/stores/authStore'
import { sendMessageApi, getChatHistoryApi } from '@/api/modules/message'

export default {
  name: 'NewChatWindow',
  components: {
    MessageList,
    MessageInput,
    EmojiSelector
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const authStore = useAuthStore()

    // 响应式数据
    const messages = ref([])
    const loadingMore = ref(false)
    const hasMoreMessages = ref(true)
    const showEmojiSelector = ref(false)
    const showSettings = ref(false)
    const emojiPosition = ref({ top: '0px', left: '0px' })
    const currentPage = ref(1)
    const pageSize = 50

    // 聊天设置
    const settings = ref({
      enableNotifications: true,
      enableSound: true,
      showTimestamp: true,
      fontSize: 'medium'
    })

    // 计算属性
    const recipientId = computed(() => route.params.id)
    const recipientType = computed(() => route.query.type || 'user')
    const currentUserId = computed(() => authStore.user?.id)
    const isGroupChat = computed(() => recipientType.value === 'group')
    const isOnline = ref(true) // 这里可以从API获取在线状态

    const chatTitle = computed(() => {
      // 这里可以根据recipientId和type获取聊天对象信息
      return route.query.name || (isGroupChat.value ? '群聊' : '私聊')
    })

    // 加载聊天记录
    const loadChatHistory = async (reset = false) => {
      try {
        if (reset) {
          currentPage.value = 1
          messages.value = []
          hasMoreMessages.value = true
        }

        if (!hasMoreMessages.value) return

        loadingMore.value = true

        const response = await getChatHistoryApi({
          recipientId: recipientId.value,
          recipientType: recipientType.value,
          page: currentPage.value,
          pageSize: pageSize
        })

        if (response.success) {
          const newMessages = response.data.list || []

          if (reset) {
            messages.value = newMessages.reverse() // 倒序显示
          } else {
            // 加载更多时，将新消息添加到开头
            messages.value = [...newMessages.reverse(), ...messages.value]
          }

          // 检查是否还有更多消息
          hasMoreMessages.value = newMessages.length === pageSize
          currentPage.value++
        }
      } catch (error) {
        console.error('加载聊天记录失败:', error)
      } finally {
        loadingMore.value = false
      }
    }

    // 发送消息
    const handleMessageSent = async (messageData) => {
      try {
        const response = await sendMessageApi(messageData)

        if (response.success) {
          // 将发送的消息添加到消息列表
          const newMessage = {
            id: Date.now(), // 临时ID
            senderId: currentUserId.value,
            senderName: authStore.user?.username || '我',
            content: messageData.content,
            type: messageData.type,
            timestamp: new Date().toISOString(),
            status: 'sending'
          }

          messages.value.push(newMessage)

          // 更新消息状态
          if (response.data) {
            newMessage.id = response.data.id
            newMessage.status = 'sent'
          }
        } else {
          throw new Error(response.message || '发送失败')
        }
      } catch (error) {
        console.error('发送消息失败:', error)
        // 可以显示错误提示
      }
    }

    // 加载更多消息
    const loadMoreMessages = () => {
      loadChatHistory(false)
    }

    // 插入表情
    const insertEmoji = (emoji) => {
      // 这里可以通过事件总线或者其他方式通知MessageInput组件插入表情
      showEmojiSelector.value = false
    }

    // 显示/隐藏表情选择器
    const showEmojiSelectorAt = (position) => {
      emojiPosition.value = position
      showEmojiSelector.value = true
    }

    const hideEmojiSelector = () => {
      showEmojiSelector.value = false
    }

    // 显示/隐藏设置
    const toggleChatSettings = () => {
      showSettings.value = !showSettings.value
    }

    const hideSettings = () => {
      showSettings.value = false
    }

    // 关闭聊天
    const closeChat = () => {
      router.push('/chat')
    }

    // 监听设置变化，保存到本地存储
    watch(settings, (newSettings) => {
      localStorage.setItem(`chat-settings-${recipientId.value}`, JSON.stringify(newSettings))
    }, { deep: true })

    // 监听路由参数变化
    watch([recipientId, recipientType], () => {
      if (recipientId.value) {
        loadChatHistory(true)
        // 加载保存的设置
        const savedSettings = localStorage.getItem(`chat-settings-${recipientId.value}`)
        if (savedSettings) {
          try {
            settings.value = JSON.parse(savedSettings)
          } catch (error) {
            console.error('加载设置失败:', error)
          }
        }
      }
    }, { immediate: true })

    // 组件挂载时加载聊天记录
    onMounted(() => {
      if (recipientId.value) {
        loadChatHistory(true)
      }
    })

    return {
      messages,
      loadingMore,
      hasMoreMessages,
      showEmojiSelector,
      showSettings,
      emojiPosition,
      settings,
      recipientId,
      recipientType,
      currentUserId,
      isGroupChat,
      isOnline,
      chatTitle,
      handleMessageSent,
      loadMoreMessages,
      insertEmoji,
      showEmojiSelectorAt,
      hideEmojiSelector,
      toggleChatSettings,
      hideSettings,
      closeChat
    }
  }
}
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f8f9fa;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: white;
  border-bottom: 1px solid #e9ecef;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.chat-info {
  flex: 1;
}

.chat-title {
  margin: 0 0 5px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.chat-status {
  font-size: 12px;
}

.status-online {
  color: #28a745;
}

.status-offline {
  color: #6c757d;
}

.chat-actions {
  display: flex;
  gap: 10px;
}

.action-button {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.action-button:hover {
  background: #f0f0f0;
}

.chat-messages {
  flex: 1;
  overflow: hidden;
}

.chat-input {
  border-top: 1px solid #e9ecef;
}

/* 设置弹窗样式 */
.settings-overlay {
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

.settings-modal {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
}

.settings-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.settings-content {
  padding: 20px;
}

.setting-item {
  margin-bottom: 15px;
}

.setting-item label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
}

.setting-item input[type="checkbox"] {
  width: 18px;
  height: 18px;
}

.setting-item select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  background: white;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-header {
    padding: 10px 15px;
  }

  .chat-title {
    font-size: 16px;
  }

  .settings-modal {
    width: 95%;
    margin: 20px;
  }
}
</style>