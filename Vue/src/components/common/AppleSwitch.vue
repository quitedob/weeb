<template>
  <div class="apple-switch" :class="switchClass" @click="toggle">
    <div class="apple-switch-track">
      <div class="apple-switch-thumb"></div>
    </div>
    <span v-if="label || $slots.default" class="apple-switch-label">
      <slot>{{ label }}</slot>
    </span>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'AppleSwitch',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    label: {
      type: String,
      default: ''
    },
    disabled: {
      type: Boolean,
      default: false
    },
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large'].includes(value)
    },
    loading: {
      type: Boolean,
      default: false
    },
    activeColor: {
      type: String,
      default: ''
    },
    inactiveColor: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit }) {
    const switchClass = computed(() => {
      const classes = []

      if (props.modelValue) {
        classes.push('apple-switch--checked')
      }

      if (props.disabled) {
        classes.push('apple-switch--disabled')
      }

      if (props.loading) {
        classes.push('apple-switch--loading')
      }

      if (props.size) {
        classes.push(`apple-switch--${props.size}`)
      }

      return classes
    })

    const toggle = () => {
      if (props.disabled || props.loading) {
        return
      }

      const newValue = !props.modelValue
      emit('update:modelValue', newValue)
      emit('change', newValue)
    }

    return {
      switchClass,
      toggle
    }
  }
}
</script>

<style scoped>
.apple-switch {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}

.apple-switch:active {
  transform: scale(0.95);
}

.apple-switch--disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.apple-switch--disabled .apple-switch-track {
  cursor: not-allowed;
}

.apple-switch-track {
  position: relative;
  display: inline-block;
  background: var(--apple-bg-tertiary);
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid var(--apple-bg-quaternary);
}

.apple-switch-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  background: white;
  border-radius: 50%;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 尺寸变化 */
.apple-switch--small .apple-switch-track {
  width: 36px;
  height: 20px;
}

.apple-switch--small .apple-switch-thumb {
  width: 16px;
  height: 16px;
}

.apple-switch--medium .apple-switch-track {
  width: 44px;
  height: 24px;
}

.apple-switch--medium .apple-switch-thumb {
  width: 20px;
  height: 20px;
}

.apple-switch--large .apple-switch-track {
  width: 52px;
  height: 28px;
}

.apple-switch--large .apple-switch-thumb {
  width: 24px;
  height: 24px;
}

/* 选中状态 */
.apple-switch--checked .apple-switch-track {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.apple-switch--checked .apple-switch-thumb {
  transform: translateX(calc(var(--thumb-translate, 20px)));
}

.apple-switch--small.apple-switch--checked .apple-switch-thumb {
  --thumb-translate: 16px;
}

.apple-switch--medium.apple-switch--checked .apple-switch-thumb {
  --thumb-translate: 20px;
}

.apple-switch--large.apple-switch--checked .apple-switch-thumb {
  --thumb-translate: 24px;
}

/* 加载状态 */
.apple-switch--loading .apple-switch-thumb {
  background: transparent;
  box-shadow: none;
}

.apple-switch--loading .apple-switch-thumb::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  border: 2px solid white;
  border-top: 2px solid transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.apple-switch--small.apple-switch--loading .apple-switch-thumb::before {
  width: 10px;
  height: 10px;
  border-width: 1.5px;
}

.apple-switch--large.apple-switch--loading .apple-switch-thumb::before {
  width: 14px;
  height: 14px;
}

@keyframes spin {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  100% { transform: translate(-50%, -50%) rotate(360deg); }
}

/* 标签样式 */
.apple-switch-label {
  margin-left: 8px;
  color: var(--apple-text-primary);
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.apple-switch--disabled .apple-switch-label {
  color: var(--apple-text-tertiary);
}

/* 自定义颜色 */
.apple-switch--checked .apple-switch-track {
  background: v-bind(activeColor || 'var(--apple-blue)');
  border-color: v-bind(activeColor || 'var(--apple-blue)');
}

.apple-switch:not(.apple-switch--checked) .apple-switch-track {
  background: v-bind(inactiveColor || 'var(--apple-bg-tertiary)');
  border-color: v-bind(inactiveColor || 'var(--apple-bg-quaternary)');
}

/* 悬停效果 */
.apple-switch:hover .apple-switch-track {
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.1);
}

.apple-switch--checked:hover .apple-switch-track {
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.2);
}

.apple-switch--disabled:hover .apple-switch-track {
  box-shadow: none;
}

/* 焦点样式 */
.apple-switch:focus-within .apple-switch-track {
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.3);
  outline: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-switch-label {
    font-size: 13px;
  }
}
</style>