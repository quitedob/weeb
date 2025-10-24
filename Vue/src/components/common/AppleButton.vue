<template>
  <button
    :class="buttonClass"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="loading-spinner"></span>
    <span v-if="$slots.icon" class="button-icon">
      <slot name="icon"></slot>
    </span>
    <span v-if="$slots.default" class="button-text">
      <slot></slot>
    </span>
  </button>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'AppleButton',
  props: {
    type: {
      type: String,
      default: 'primary',
      validator: (value) => ['primary', 'secondary', 'success', 'warning', 'danger', 'ghost', 'link'].includes(value)
    },
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large'].includes(value)
    },
    disabled: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    block: {
      type: Boolean,
      default: false
    },
    rounded: {
      type: Boolean,
      default: false
    }
  },
  emits: ['click'],
  setup(props, { emit }) {
    const buttonClass = computed(() => {
      const classes = [
        'apple-button',
        `apple-button--${props.type}`,
        `apple-button--${props.size}`
      ]

      if (props.disabled) {
        classes.push('apple-button--disabled')
      }

      if (props.loading) {
        classes.push('apple-button--loading')
      }

      if (props.block) {
        classes.push('apple-button--block')
      }

      if (props.rounded) {
        classes.push('apple-button--rounded')
      }

      return classes.join(' ')
    })

    const handleClick = (event) => {
      if (!props.disabled && !props.loading) {
        emit('click', event)
      }
    }

    return {
      buttonClass,
      handleClick
    }
  }
}
</script>

<style scoped>
.apple-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
  border-radius: 8px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
  outline: none;
  position: relative;
  overflow: hidden;
}

.apple-button:focus-visible {
  outline: 2px solid var(--apple-blue);
  outline-offset: 2px;
}

/* 按钮类型 */
.apple-button--primary {
  background: var(--apple-blue);
  color: white;
}

.apple-button--primary:hover:not(.apple-button--disabled) {
  background: var(--apple-blue-dark);
}

.apple-button--secondary {
  background: var(--apple-bg-secondary);
  color: var(--apple-text-primary);
}

.apple-button--secondary:hover:not(.apple-button--disabled) {
  background: var(--apple-bg-tertiary);
}

.apple-button--success {
  background: var(--apple-green);
  color: white;
}

.apple-button--success:hover:not(.apple-button--disabled) {
  background: #28a745;
}

.apple-button--warning {
  background: var(--apple-orange);
  color: white;
}

.apple-button--warning:hover:not(.apple-button--disabled) {
  background: #e0a800;
}

.apple-button--danger {
  background: var(--apple-red);
  color: white;
}

.apple-button--danger:hover:not(.apple-button--disabled) {
  background: #dc3545;
}

.apple-button--ghost {
  background: transparent;
  color: var(--apple-blue);
  border: 1px solid var(--apple-bg-tertiary);
}

.apple-button--ghost:hover:not(.apple-button--disabled) {
  background: var(--apple-bg-secondary);
}

.apple-button--link {
  background: transparent;
  color: var(--apple-blue);
  border: none;
  text-decoration: underline;
}

.apple-button--link:hover:not(.apple-button--disabled) {
  color: var(--apple-blue-dark);
}

/* 按钮尺寸 */
.apple-button--small {
  padding: 6px 12px;
  font-size: 12px;
  min-height: 28px;
}

.apple-button--medium {
  padding: 8px 16px;
  font-size: 14px;
  min-height: 34px;
}

.apple-button--large {
  padding: 12px 24px;
  font-size: 16px;
  min-height: 44px;
}

/* 按钮状态 */
.apple-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.apple-button--loading {
  cursor: wait;
}

/* 块级按钮 */
.apple-button--block {
  width: 100%;
}

/* 圆角按钮 */
.apple-button--rounded {
  border-radius: 20px;
}

/* 图标和文字 */
.button-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.button-text {
  white-space: nowrap;
}

/* 加载动画 */
.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-button--small {
    padding: 8px 12px;
    font-size: 13px;
    min-height: 32px;
  }

  .apple-button--medium {
    padding: 10px 16px;
    font-size: 15px;
    min-height: 38px;
  }

  .apple-button--large {
    padding: 14px 20px;
    font-size: 16px;
    min-height: 48px;
  }
}
</style>