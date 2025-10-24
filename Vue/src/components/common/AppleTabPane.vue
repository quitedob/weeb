<template>
  <div
    v-show="isActive"
    class="apple-tab-pane"
    :class="paneClass"
  >
    <slot></slot>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'AppleTabPane',
  props: {
    label: {
      type: String,
      default: ''
    },
    name: {
      type: [String, Number],
      default: ''
    },
    disabled: {
      type: Boolean,
      default: false
    },
    closable: {
      type: Boolean,
      default: false
    },
    lazy: {
      type: Boolean,
      default: false
    }
  },
  setup(props) {
    // 简化实现：直接通过父组件传递的activeName来判断
    const isActive = computed(() => {
      // 这里先总是显示，稍后通过父组件来控制
      return true
    })

    const paneClass = computed(() => {
      const classes = []

      if (props.disabled) {
        classes.push('apple-tab-pane--disabled')
      }

      return classes
    })

    return {
      isActive,
      paneClass
    }
  }
}
</script>

<style scoped>
.apple-tab-pane {
  padding: 20px 0;
  color: var(--apple-text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  line-height: 1.6;
}

.apple-tab-pane--active {
  display: block;
}

.apple-tab-pane--disabled {
  pointer-events: none;
  opacity: 0.5;
}

/* 动画 */
.apple-tab-pane {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-tab-pane {
    padding: 16px 0;
  }
}
</style>