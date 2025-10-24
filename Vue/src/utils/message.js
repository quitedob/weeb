import { createApp } from 'vue'
import AppleMessage from '@/components/common/AppleMessage.vue'

// 创建消息容器
let messageContainer = null
let messageApp = null
let messageId = 0

const createMessageContainer = () => {
  if (messageContainer) return messageContainer

  messageContainer = document.createElement('div')
  messageContainer.className = 'apple-message-container'
  messageContainer.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    z-index: 3000;
    pointer-events: none;
  `
  document.body.appendChild(messageContainer)

  messageApp = createApp({
    components: { AppleMessage }
  })
  messageApp.mount(messageContainer)

  return messageContainer
}

const createMessage = (options) => {
  const container = createMessageContainer()
  const id = ++messageId

  const message = {
    id,
    type: options.type || 'info',
    message: options.message || '',
    duration: options.duration || 3000,
    onClose: options.onClose,
    showClose: options.showClose !== false
  }

  // 创建消息元素
  const messageElement = document.createElement('div')
  messageElement.className = 'apple-message-wrapper'
  messageElement.innerHTML = `
    <div class="apple-message apple-message--${message.type}" data-message-id="${id}">
      <div class="apple-message__content">
        <div class="apple-message__icon">
          <i class="icon-${message.type}"></i>
        </div>
        <div class="apple-message__text">${message.message}</div>
        ${message.showClose ? '<button class="apple-message__closebtn"><i class="icon-close"></i></button>' : ''}
      </div>
    </div>
  `

  container.appendChild(messageElement)

  // 添加显示动画
  setTimeout(() => {
    messageElement.querySelector('.apple-message').classList.add('apple-message--show')
  }, 10)

  // 关闭消息
  const closeMessage = () => {
    const messageEl = messageElement.querySelector('.apple-message')
    messageEl.classList.add('apple-message--hide')

    setTimeout(() => {
      if (messageElement.parentNode) {
        messageElement.parentNode.removeChild(messageElement)
      }
      if (message.onClose) {
        message.onClose()
      }
    }, 300)
  }

  // 绑定关闭事件
  if (message.showClose) {
    const closeBtn = messageElement.querySelector('.apple-message__closebtn')
    if (closeBtn) {
      closeBtn.addEventListener('click', closeMessage)
    }
  }

  // 自动关闭
  if (message.duration > 0) {
    setTimeout(closeMessage, message.duration)
  }

  return {
    close: closeMessage,
    id
  }
}

// 消息方法
const showMessage = {
  success: (message, options = {}) => {
    return createMessage({ type: 'success', message, ...options })
  },
  warning: (message, options = {}) => {
    return createMessage({ type: 'warning', message, ...options })
  },
  info: (message, options = {}) => {
    return createMessage({ type: 'info', message, ...options })
  },
  error: (message, options = {}) => {
    return createMessage({ type: 'error', message, duration: 0, ...options }) // 错误消息默认不自动关闭
  }
}

// 确认对话框
const showConfirm = {
  warning: async (message, title = '确认') => {
    return new Promise((resolve) => {
      const confirmed = window.confirm(`${title}\n\n${message}`)
      resolve(confirmed)
    })
  },

  prompt: async (message, title = '输入', options = {}) => {
    return new Promise((resolve) => {
      const input = window.prompt(`${title}\n\n${message}`, options.defaultValue || '')

      if (input === null) {
        resolve(null)
        return
      }

      // 验证输入
      if (options.required && !input.trim()) {
        showMessage.error('此字段为必填项')
        resolve(null)
        return
      }

      if (options.minLength && input.length < options.minLength) {
        showMessage.error(`输入长度不能少于${options.minLength}个字符`)
        resolve(null)
        return
      }

      if (options.maxLength && input.length > options.maxLength) {
        showMessage.error(`输入长度不能超过${options.maxLength}个字符`)
        resolve(null)
        return
      }

      resolve(input.trim())
    })
  }
}

export { showMessage, showConfirm }
export default { showMessage, showConfirm }