<template>
  <div class="message-input-container">
    <!-- 表情选择器 -->
    <div class="emoji-button" @click="toggleEmojiSelector" title="选择表情">
      😊
    </div>

    <!-- 消息输入框 -->
    <div class="chat-msg-input">
      <input
        v-model="messageContent"
        type="text"
        placeholder="请输入消息"
        class="chat-text-input"
        @keydown.enter.exact="sendMessage"
        @keydown.enter.shift.exact="addNewLine"
        @input="handleTyping"
      />
    </div>

    <!-- 发送按钮 -->
    <button
      class="publish-button"
      :disabled="!messageContent.trim()"
      @click="sendMessage"
    >
      发送
    </button>

    <!-- 表情选择器弹窗 -->
    <div
      v-if="showEmojiSelector"
      class="emoji-popup"
      :style="emojiPosition"
    >
      <h4 class="emoji-title">选择表情</h4>
      <div class="emoji-search-container">
        <input
          v-model="emojiSearch"
          type="text"
          placeholder="搜索表情"
          class="emoji-search-input"
        />
      </div>
      <div class="emoji-grid">
        <div
          v-for="emoji in filteredEmojis"
          :key="emoji.name"
          :title="emoji.name"
          class="emoji-item"
          @click="insertEmoji(emoji.icon)"
        >
          {{ emoji.icon }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import emojis from '@constant/emoji/emoji.js'

export default {
  name: 'MessageInput',
  props: {
    recipientId: {
      type: [String, Number],
      required: true
    },
    recipientType: {
      type: String,
      required: true,
      validator: (value) => ['user', 'group'].includes(value)
    }
  },
  emits: ['message-sent'],
  setup(props, { emit }) {
    const messageContent = ref('')
    const showEmojiSelector = ref(false)
    const emojiSearch = ref('')
    const emojiPosition = ref({ top: '0px', left: '0px', width: '0px' })
    const inputRef = ref(null)

    // 计算属性：过滤后的表情列表
    const filteredEmojis = computed(() => {
      if (!emojiSearch.value) {
        return emojis.slice(0, 50) // 显示前50个表情
      }
      return emojis.filter(emoji =>
        emoji.name.toLowerCase().includes(emojiSearch.value.toLowerCase())
      )
    })

    // 发送消息
    const sendMessage = async () => {
      const content = messageContent.value.trim()
      if (!content) return

      try {
        // 构建消息对象
        const message = {
          recipientId: props.recipientId,
          recipientType: props.recipientType,
          content: content,
          type: 'text'
        }

        // 发射消息发送事件
        emit('message-sent', message)

        // 清空输入框
        messageContent.value = ''

        // 关闭表情选择器
        showEmojiSelector.value = false

      } catch (error) {
        console.error('发送消息失败:', error)
        // 可以添加错误提示
      }
    }

    // 换行处理
    const addNewLine = () => {
      messageContent.value += '\n'
    }

    // 处理输入事件（用于显示打字状态）
    const handleTyping = () => {
      // 可以在这里添加打字状态逻辑
    }

    // 切换表情选择器
    const toggleEmojiSelector = () => {
      showEmojiSelector.value = !showEmojiSelector.value
      if (showEmojiSelector.value) {
        // 计算表情选择器位置
        const inputRect = inputRef.value?.getBoundingClientRect()
        if (inputRect) {
          emojiPosition.value = {
            top: `${inputRect.top - 200}px`,
            left: `${inputRect.left}px`,
            width: `${inputRect.width}px`
          }
        }
      }
    }

    // 插入表情
    const insertEmoji = (emoji) => {
      messageContent.value += emoji
      showEmojiSelector.value = false
      inputRef.value?.focus()
    }

    // 点击外部关闭表情选择器
    const handleClickOutside = (event) => {
      if (!event.target.closest('.message-input-container') && !event.target.closest('.emoji-popup')) {
        showEmojiSelector.value = false
      }
    }

    onMounted(() => {
      document.addEventListener('click', handleClickOutside)
    })

    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside)
    })

    return {
      messageContent,
      showEmojiSelector,
      emojiSearch,
      emojiPosition,
      filteredEmojis,
      inputRef,
      sendMessage,
      addNewLine,
      handleTyping,
      toggleEmojiSelector,
      insertEmoji
    }
  }
}
</script>

<style scoped>
.message-input-container {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  border-top: 1px solid #e9ecef;
  gap: 10px;
}

.emoji-button {
  padding: 8px;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.2s;
  font-size: 18px;
}

.emoji-button:hover {
  background-color: #e9ecef;
}

.chat-msg-input {
  flex: 1;
  position: relative;
}

.chat-text-input {
  width: 100%;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
  resize: none;
  transition: border-color 0.2s;
}

.chat-text-input:focus {
  border-color: #007bff;
}

.publish-button {
  padding: 8px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.publish-button:hover:not(:disabled) {
  background: #0056b3;
}

.publish-button:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

/* 表情选择器样式 */
.emoji-popup {
  position: fixed;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  padding: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.emoji-title {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #333;
  text-align: center;
}

.emoji-search-container {
  margin-bottom: 10px;
}

.emoji-search-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
}

.emoji-item {
  padding: 5px;
  text-align: center;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
  font-size: 16px;
}

.emoji-item:hover {
  background-color: #f0f0f0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .message-input-container {
    padding: 10px;
    gap: 8px;
  }

  .emoji-grid {
    grid-template-columns: repeat(6, 1fr);
  }

  .emoji-popup {
    max-height: 250px;
  }
}
</style>