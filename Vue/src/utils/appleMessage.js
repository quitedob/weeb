import { createApp, h } from 'vue'
import AppleMessage from '@/components/common/AppleMessage.vue'

const show = (message, type = 'info', duration = 3000) => {
  const container = document.createElement('div')
  container.className = 'apple-toast-host'
  let stack = document.querySelector('.apple-toast-stack')
  if (!stack) {
    stack = document.createElement('div')
    stack.className = 'apple-toast-stack'
    stack.style.cssText = 'position:fixed;top:20px;right:20px;z-index:9999;pointer-events:none;'
    document.body.appendChild(stack)
  }
  stack.appendChild(container)
  let closed = false
  const close = () => {
    if (closed) return
    closed = true
    app.unmount()
    container.remove()
    if (!stack.children.length) stack.remove()
  }
  const app = createApp({ render: () => h(AppleMessage, {
    message: String(message ?? ''), type, duration, class: 'apple-message--show',
    role: type === 'error' ? 'alert' : 'status', onClose: close
  }) })
  app.mount(container)
  return { close }
}

export default {
  show,
  success: (message, duration) => show(message, 'success', duration),
  error: (message, duration) => show(message, 'error', duration),
  warning: (message, duration) => show(message, 'warning', duration),
  info: (message, duration) => show(message, 'info', duration)
}
