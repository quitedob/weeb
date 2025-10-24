<template>
  <div class="apple-message" :class="messageClasses" :style="messageStyle">
    <div class="apple-message__content">
      <div class="apple-message__icon">
        <i :class="iconClass"></i>
      </div>
      <div class="apple-message__text">{{ message }}</div>
      <button
        v-if="showClose"
        class="apple-message__closebtn"
        @click="close"
      >
        <i class="icon-close"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'info',
    validator: (value) => ['success', 'warning', 'error', 'info'].includes(value)
  },
  message: {
    type: String,
    required: true
  },
  duration: {
    type: Number,
    default: 3000
  },
  showClose: {
    type: Boolean,
    default: true
  },
  onClose: {
    type: Function,
    default: () => {}
  }
})

const emit = defineEmits(['close'])

// 计算属性
const messageClasses = computed(() => [
  `apple-message--${props.type}`,
  {
    'apple-message--closable': props.showClose
  }
])

const messageStyle = computed(() => ({}))

const iconClass = computed(() => {
  const iconMap = {
    success: 'icon-check-circle',
    warning: 'icon-warning',
    error: 'icon-close-circle',
    info: 'icon-info-circle'
  }
  return iconMap[props.type] || 'icon-info-circle'
})

// 方法
const close = () => {
  emit('close')
  if (props.onClose) {
    props.onClose()
  }
}

let timer = null

// 生命周期
onMounted(() => {
  if (props.duration > 0) {
    timer = setTimeout(() => {
      close()
    }, props.duration)
  }
})

onUnmounted(() => {
  if (timer) {
    clearTimeout(timer)
  }
})
</script>

<style scoped>
.apple-message {
  position: relative;
  min-width: 320px;
  max-width: 500px;
  padding: var(--apple-spacing-md) var(--apple-spacing-lg);
  margin-bottom: var(--apple-spacing-sm);
  border-radius: var(--apple-border-radius-lg);
  background-color: var(--apple-bg-elevated);
  border: 1px solid var(--apple-border-secondary);
  box-shadow: var(--apple-shadow-large);
  backdrop-filter: blur(20px);
  transform: translateX(100%);
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: all;
}

.apple-message--show {
  transform: translateX(0);
  opacity: 1;
}

.apple-message--hide {
  transform: translateX(100%);
  opacity: 0;
}

.apple-message__content {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
}

.apple-message__icon {
  flex-shrink: 0;
  font-size: 18px;
}

.apple-message__text {
  flex: 1;
  font-size: var(--apple-font-sm);
  line-height: 1.5;
  color: var(--apple-text-primary);
  word-break: break-word;
}

.apple-message__closebtn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  background: none;
  color: var(--apple-text-tertiary);
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 12px;
}

.apple-message__closebtn:hover {
  background-color: var(--apple-bg-hover);
  color: var(--apple-text-secondary);
}

/* 类型样式 */
.apple-message--success {
  border-color: rgba(52, 199, 89, 0.2);
  background-color: rgba(52, 199, 89, 0.05);
}

.apple-message--success .apple-message__icon {
  color: var(--apple-green);
}

.apple-message--warning {
  border-color: rgba(255, 149, 0, 0.2);
  background-color: rgba(255, 149, 0, 0.05);
}

.apple-message--warning .apple-message__icon {
  color: var(--apple-orange);
}

.apple-message--error {
  border-color: rgba(255, 59, 48, 0.2);
  background-color: rgba(255, 59, 48, 0.05);
}

.apple-message--error .apple-message__icon {
  color: var(--apple-red);
}

.apple-message--info {
  border-color: rgba(0, 122, 255, 0.2);
  background-color: rgba(0, 122, 255, 0.05);
}

.apple-message--info .apple-message__icon {
  color: var(--apple-blue);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-message {
    min-width: 280px;
    max-width: calc(100vw - 40px);
    margin: 0 20px var(--apple-spacing-sm) 20px;
  }

  .apple-message__content {
    gap: var(--apple-spacing-xs);
  }

  .apple-message__icon {
    font-size: 16px;
  }

  .apple-message__text {
    font-size: var(--apple-font-xs);
  }
}

/* 暗色模式支持 */
@media (prefers-color-scheme: dark) {
  .apple-message {
    background-color: var(--apple-bg-elevated);
    border-color: var(--apple-border-secondary);
  }

  .apple-message__text {
    color: var(--apple-text-primary);
  }

  .apple-message__closebtn {
    color: var(--apple-text-tertiary);
  }

  .apple-message__closebtn:hover {
    background-color: var(--apple-bg-hover);
    color: var(--apple-text-secondary);
  }
}

/* 动画性能优化 */
.apple-message {
  will-change: transform, opacity;
}

/* 消息容器样式 */
:global(.apple-message-container) {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 3000;
  pointer-events: none;
  max-width: 100%;
}

/* 消息堆叠效果 */
:global(.apple-message-wrapper) {
  position: relative;
  margin-bottom: var(--apple-spacing-sm);
}

:global(.apple-message-wrapper:last-child) {
  margin-bottom: 0;
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .apple-message {
    border-width: 2px;
  }

  .apple-message__icon {
    filter: contrast(1.2);
  }
}

/* 减少动画模式支持 */
@media (prefers-reduced-motion: reduce) {
  .apple-message {
    transition: none;
  }

  .apple-message__closebtn {
    transition: none;
  }
}
</style>