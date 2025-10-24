<template>
  <div :class="cardClass" :style="cardStyle">
    <div v-if="$slots.header || title" class="apple-card-header">
      <slot name="header">
        <h3 v-if="title" class="apple-card-title">{{ title }}</h3>
        <div v-if="subtitle" class="apple-card-subtitle">{{ subtitle }}</div>
      </slot>
    </div>

    <div class="apple-card-body">
      <slot></slot>
    </div>

    <div v-if="$slots.footer" class="apple-card-footer">
      <slot name="footer"></slot>
    </div>

    <div v-if="loading" class="apple-card-loading">
      <div class="loading-spinner"></div>
      <div class="loading-text">{{ loadingText }}</div>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'AppleCard',
  props: {
    title: {
      type: String,
      default: ''
    },
    subtitle: {
      type: String,
      default: ''
    },
    shadow: {
      type: Boolean,
      default: true
    },
    bordered: {
      type: Boolean,
      default: true
    },
    hoverable: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    loadingText: {
      type: String,
      default: '加载中...'
    },
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large'].includes(value)
    },
    bodyStyle: {
      type: [Object, String],
      default: null
    },
    headStyle: {
      type: [Object, String],
      default: null
    },
    footerStyle: {
      type: [Object, String],
      default: null
    }
  },
  setup(props) {
    const cardClass = computed(() => {
      const classes = ['apple-card']

      if (props.shadow) {
        classes.push('apple-card--shadow')
      }

      if (props.bordered) {
        classes.push('apple-card--bordered')
      }

      if (props.hoverable) {
        classes.push('apple-card--hoverable')
      }

      if (props.loading) {
        classes.push('apple-card--loading')
      }

      if (props.size) {
        classes.push(`apple-card--${props.size}`)
      }

      return classes.join(' ')
    })

    const cardStyle = computed(() => {
      const style = {}

      if (props.bodyStyle) {
        style['--apple-card-body-padding'] = '16px'
      }

      return style
    })

    return {
      cardClass,
      cardStyle
    }
  }
}
</script>

<style scoped>
.apple-card {
  background: var(--apple-bg-primary);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
}

.apple-card--shadow {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.apple-card--bordered {
  border: 1px solid var(--apple-bg-quaternary);
}

.apple-card--hoverable:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.apple-card--loading {
  pointer-events: none;
  opacity: 0.8;
}

/* 卡片头部 */
.apple-card-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--apple-bg-quaternary);
  background: var(--apple-bg-secondary);
}

.apple-card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.apple-card-subtitle {
  margin-top: 4px;
  font-size: 14px;
  color: var(--apple-text-tertiary);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

/* 卡片内容 */
.apple-card-body {
  padding: 20px;
  color: var(--apple-text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

/* 卡片底部 */
.apple-card-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--apple-bg-quaternary);
  background: var(--apple-bg-secondary);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

/* 加载状态 */
.apple-card-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--apple-bg-quaternary);
  border-top: 2px solid var(--apple-blue);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

.loading-text {
  font-size: 14px;
  color: var(--apple-text-secondary);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 尺寸变化 */
.apple-card--small {
  border-radius: 8px;
}

.apple-card--small .apple-card-header {
  padding: 12px 16px;
}

.apple-card--small .apple-card-body {
  padding: 16px;
}

.apple-card--small .apple-card-footer {
  padding: 8px 16px;
}

.apple-card--small .apple-card-title {
  font-size: 14px;
}

.apple-card--small .apple-card-subtitle {
  font-size: 12px;
}

.apple-card--large {
  border-radius: 16px;
}

.apple-card--large .apple-card-header {
  padding: 20px 24px;
}

.apple-card--large .apple-card-body {
  padding: 24px;
}

.apple-card--large .apple-card-footer {
  padding: 16px 24px;
}

.apple-card--large .apple-card-title {
  font-size: 18px;
}

.apple-card--large .apple-card-subtitle {
  font-size: 15px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-card {
    border-radius: 8px;
  }

  .apple-card-header {
    padding: 12px 16px;
  }

  .apple-card-body {
    padding: 16px;
  }

  .apple-card-footer {
    padding: 8px 16px;
  }

  .apple-card-title {
    font-size: 15px;
  }

  .apple-card-subtitle {
    font-size: 13px;
  }
}

/* 无边框样式 */
.apple-card:not(.apple-card--bordered) {
  border: none;
}

/* 自定义样式覆盖 */
.apple-card-body:deep(*) {
  line-height: 1.5;
}

/* 内容样式 */
.apple-card-body :deep(p) {
  margin: 0 0 8px 0;
}

.apple-card-body :deep(p:last-child) {
  margin-bottom: 0;
}

.apple-card-body :deep(h1, h2, h3, h4, h5, h6) {
  margin: 0 0 12px 0;
  font-weight: 600;
}

.apple-card-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
}
</style>