<template>
  <div
    :class="itemClass"
    :data-command="command"
    :data-disabled="disabled"
    @click="handleClick"
  >
    <div v-if="$slots.icon" class="apple-dropdown-item-icon">
      <slot name="icon"></slot>
    </div>

    <div class="apple-dropdown-item-content">
      <slot></slot>
    </div>

    <div v-if="$slots.suffix" class="apple-dropdown-item-suffix">
      <slot name="suffix"></slot>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'AppleDropdownItem',
  props: {
    command: {
      type: [String, Number, Object],
      default: null
    },
    disabled: {
      type: Boolean,
      default: false
    },
    divided: {
      type: Boolean,
      default: false
    },
    danger: {
      type: Boolean,
      default: false
    }
  },
  emits: ['click'],
  setup(props, { emit }) {
    const itemClass = computed(() => {
      const classes = ['apple-dropdown-item']

      if (props.disabled) {
        classes.push('apple-dropdown-item--disabled')
      }

      if (props.danger) {
        classes.push('apple-dropdown-item--danger')
      }

      return classes
    })

    const handleClick = (e) => {
      if (props.disabled) {
        e.preventDefault()
        e.stopPropagation()
        return
      }

      emit('click', props.command, e)
    }

    return {
      itemClass,
      handleClick
    }
  }
}
</script>

<style scoped>
.apple-dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: var(--apple-text-primary);
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.apple-dropdown-item:hover {
  background: var(--apple-bg-tertiary);
}

.apple-dropdown-item--disabled {
  color: var(--apple-text-tertiary);
  cursor: not-allowed;
}

.apple-dropdown-item--disabled:hover {
  background: transparent;
}

.apple-dropdown-item--danger {
  color: var(--apple-red);
}

.apple-dropdown-item--danger:hover {
  background: rgba(255, 59, 48, 0.1);
}

.apple-dropdown-item-icon {
  display: flex;
  align-items: center;
  font-size: 16px;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.apple-dropdown-item-content {
  flex: 1;
  display: flex;
  align-items: center;
}

.apple-dropdown-item-suffix {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: var(--apple-text-tertiary);
  margin-left: auto;
}
</style>

<style>
/* 分割线样式 (全局) */
.apple-dropdown-divider {
  height: 1px;
  background: var(--apple-bg-quaternary);
  margin: 4px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-dropdown-item {
    padding: 16px;
    font-size: 16px;
  }
}
</style>