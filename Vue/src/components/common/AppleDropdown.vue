<template>
  <div class="apple-dropdown" ref="dropdownRef">
    <!-- 触发器 -->
    <div
      class="apple-dropdown-trigger"
      @click="handleTriggerClick"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
    >
      <slot name="trigger">
        <div class="default-trigger">
          <span>请选择</span>
          <span class="dropdown-arrow">▼</span>
        </div>
      </slot>
    </div>

    <!-- 下拉菜单 -->
    <Teleport to="body">
      <div
        v-if="visible"
        class="apple-dropdown-menu"
        :class="menuClass"
        :style="menuStyle"
        @click="handleMenuClick"
        @mouseenter="handleMenuMouseEnter"
        @mouseleave="handleMenuMouseLeave"
      >
        <div class="apple-dropdown-content">
          <slot></slot>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

export default {
  name: 'AppleDropdown',
  props: {
    trigger: {
      type: String,
      default: 'click',
      validator: (value) => ['click', 'hover', 'manual'].includes(value)
    },
    placement: {
      type: String,
      default: 'bottom-start',
      validator: (value) => [
        'top', 'top-start', 'top-end',
        'bottom', 'bottom-start', 'bottom-end',
        'left', 'left-start', 'left-end',
        'right', 'right-start', 'right-end'
      ].includes(value)
    },
    hideOnClick: {
      type: Boolean,
      default: true
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
    maxHeight: {
      type: [String, Number],
      default: '300px'
    },
    minWidth: {
      type: [String, Number],
      default: '120px'
    }
  },
  emits: ['visible-change', 'command'],
  setup(props, { emit }) {
    const dropdownRef = ref(null)
    const visible = ref(false)
    const triggerRef = ref(null)
    const menuRef = ref(null)

    let showTimer = null
    let hideTimer = null

    const menuClass = computed(() => {
      const classes = [`apple-dropdown-menu--${props.placement}`]

      if (props.size) {
        classes.push(`apple-dropdown-menu--${props.size}`)
      }

      return classes
    })

    const menuStyle = computed(() => {
      const style = {}

      if (props.maxHeight) {
        style.maxHeight = typeof props.maxHeight === 'number' ? `${props.maxHeight}px` : props.maxHeight
      }

      if (props.minWidth) {
        style.minWidth = typeof props.minWidth === 'number' ? `${props.minWidth}px` : props.minWidth
      }

      return style
    })

    const handleTriggerClick = () => {
      if (props.disabled) return
      if (props.trigger !== 'click') return

      visible.value ? hide() : show()
    }

    const handleMouseEnter = () => {
      if (props.disabled) return
      if (props.trigger !== 'hover') return

      clearTimeout(hideTimer)
      showTimer = setTimeout(() => {
        show()
      }, 100)
    }

    const handleMouseLeave = () => {
      if (props.trigger !== 'hover') return

      clearTimeout(showTimer)
      hideTimer = setTimeout(() => {
        hide()
      }, 100)
    }

    const handleMenuMouseEnter = () => {
      if (props.trigger !== 'hover') return
      clearTimeout(hideTimer)
    }

    const handleMenuMouseLeave = () => {
      if (props.trigger !== 'hover') return
      hideTimer = setTimeout(() => {
        hide()
      }, 100)
    }

    const handleMenuClick = (e) => {
      const target = e.target
      const item = target.closest('.apple-dropdown-item')

      if (item) {
        const command = item.getAttribute('data-command')
        const disabled = item.getAttribute('data-disabled') === 'true'

        if (!disabled) {
          emit('command', command)
          if (props.hideOnClick) {
            hide()
          }
        }
      }
    }

    const show = async () => {
      if (visible.value) return

      visible.value = true
      await nextTick()
      updatePosition()
      emit('visible-change', true)
    }

    const hide = () => {
      if (!visible.value) return

      visible.value = false
      emit('visible-change', false)
    }

    const updatePosition = () => {
      if (!dropdownRef.value || !menuRef.value) return

      const trigger = dropdownRef.value.querySelector('.apple-dropdown-trigger')
      const menu = menuRef.value

      if (!trigger) return

      const triggerRect = trigger.getBoundingClientRect()
      const menuRect = menu.getBoundingClientRect()

      let { left, top } = calculatePosition(triggerRect, menuRect)

      menu.style.left = `${left}px`
      menu.style.top = `${top}px`
    }

    const calculatePosition = (triggerRect, menuRect) => {
      const scrollX = window.pageXOffset
      const scrollY = window.pageYOffset
      const { innerWidth, innerHeight } = window

      let left = 0
      let top = 0

      // 根据placement计算位置
      switch (props.placement) {
        case 'top':
          left = triggerRect.left + (triggerRect.width - menuRect.width) / 2
          top = triggerRect.top - menuRect.height
          break
        case 'top-start':
          left = triggerRect.left
          top = triggerRect.top - menuRect.height
          break
        case 'top-end':
          left = triggerRect.right - menuRect.width
          top = triggerRect.top - menuRect.height
          break
        case 'bottom':
          left = triggerRect.left + (triggerRect.width - menuRect.width) / 2
          top = triggerRect.bottom
          break
        case 'bottom-start':
          left = triggerRect.left
          top = triggerRect.bottom
          break
        case 'bottom-end':
          left = triggerRect.right - menuRect.width
          top = triggerRect.bottom
          break
        case 'left':
          left = triggerRect.left - menuRect.width
          top = triggerRect.top + (triggerRect.height - menuRect.height) / 2
          break
        case 'left-start':
          left = triggerRect.left - menuRect.width
          top = triggerRect.top
          break
        case 'left-end':
          left = triggerRect.left - menuRect.width
          top = triggerRect.bottom - menuRect.height
          break
        case 'right':
          left = triggerRect.right
          top = triggerRect.top + (triggerRect.height - menuRect.height) / 2
          break
        case 'right-start':
          left = triggerRect.right
          top = triggerRect.top
          break
        case 'right-end':
          left = triggerRect.right
          top = triggerRect.bottom - menuRect.height
          break
      }

      // 确保菜单在视窗内
      if (left < scrollX) {
        left = scrollX
      }
      if (left + menuRect.width > scrollX + innerWidth) {
        left = scrollX + innerWidth - menuRect.width
      }
      if (top < scrollY) {
        top = scrollY
      }
      if (top + menuRect.height > scrollY + innerHeight) {
        top = scrollY + innerHeight - menuRect.height
      }

      return { left, top }
    }

    const handleClickOutside = (e) => {
      if (!dropdownRef.value || dropdownRef.value.contains(e.target)) {
        return
      }
      hide()
    }

    const handleEscape = (e) => {
      if (e.key === 'Escape' && visible.value) {
        hide()
      }
    }

    onMounted(() => {
      menuRef.value = document.querySelector('.apple-dropdown-menu')
      document.addEventListener('click', handleClickOutside)
      document.addEventListener('keydown', handleEscape)
      window.addEventListener('resize', updatePosition)
    })

    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
      window.removeEventListener('resize', updatePosition)
      clearTimeout(showTimer)
      clearTimeout(hideTimer)
    })

    return {
      dropdownRef,
      visible,
      menuClass,
      menuStyle,
      handleTriggerClick,
      handleMouseEnter,
      handleMouseLeave,
      handleMenuClick,
      handleMenuMouseEnter,
      handleMenuMouseLeave,
      show,
      hide
    }
  }
}
</script>

<style scoped>
.apple-dropdown {
  position: relative;
  display: inline-block;
}

.apple-dropdown-trigger {
  cursor: pointer;
  user-select: none;
}

.default-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--apple-bg-primary);
  border: 1px solid var(--apple-bg-quaternary);
  border-radius: 8px;
  color: var(--apple-text-primary);
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  transition: all 0.2s ease;
}

.default-trigger:hover {
  background: var(--apple-bg-tertiary);
  border-color: var(--apple-blue);
}

.dropdown-arrow {
  font-size: 12px;
  transition: transform 0.2s ease;
}

.apple-dropdown[visible] .dropdown-arrow {
  transform: rotate(180deg);
}

/* 下拉菜单 */
.apple-dropdown-menu {
  position: fixed;
  z-index: 2000;
  background: var(--apple-bg-primary);
  border: 1px solid var(--apple-bg-quaternary);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(20px);
  padding: 8px 0;
  margin: 4px;
  max-height: 300px;
  overflow-y: auto;
  animation: dropdownFadeIn 0.2s ease;
}

.apple-dropdown-content {
  max-height: inherit;
  overflow-y: auto;
}

/* 尺寸变化 */
.apple-dropdown-menu--small {
  padding: 4px 0;
}

.apple-dropdown-menu--large {
  padding: 12px 0;
}

/* 动画 */
@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 滚动条样式 */
.apple-dropdown-content::-webkit-scrollbar {
  width: 6px;
}

.apple-dropdown-content::-webkit-scrollbar-track {
  background: transparent;
}

.apple-dropdown-content::-webkit-scrollbar-thumb {
  background: var(--apple-bg-quaternary);
  border-radius: 3px;
}

.apple-dropdown-content::-webkit-scrollbar-thumb:hover {
  background: var(--apple-bg-tertiary);
}
</style>

<style>
/* 下拉项样式 (全局，因为slot内容在组件外部) */
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

.apple-dropdown-divider {
  height: 1px;
  background: var(--apple-bg-quaternary);
  margin: 4px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-dropdown-menu {
    max-width: 90vw;
  }

  .apple-dropdown-item {
    padding: 16px;
    font-size: 16px;
  }
}
</style>