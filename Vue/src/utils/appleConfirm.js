import { createApp, h } from 'vue'
import AppleModal from '@/components/common/AppleModal.vue'

const confirm = (title, message, options = {}) => new Promise(resolve => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  let settled = false
  const finish = value => {
    if (settled) return
    settled = true
    app.unmount()
    container.remove()
    resolve(value)
  }
  const app = createApp({ render: () => h(AppleModal, {
    modelValue: true, title: String(title || '确认'), showFooter: true,
    confirmText: options.confirmText || '确认', cancelText: options.cancelText || '取消',
    maskClosable: false, zIndex: 10000,
    onConfirm: () => finish(true), onClose: () => finish(false)
  }, { default: () => String(message ?? '') }) })
  app.mount(container)
})

export default { confirm }
