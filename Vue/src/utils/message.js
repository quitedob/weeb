import appleMessage from './appleMessage'
import appleConfirm from './appleConfirm'

const showMessage = Object.fromEntries(['success', 'warning', 'info', 'error'].map(type => [type, (message, options = {}) => appleMessage[type](message, options.duration)]))

// 确认对话框
const showConfirm = {
  warning: async (message, title = '确认') => {
    return appleConfirm.confirm(title, message)
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