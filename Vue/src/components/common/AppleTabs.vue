<template>
  <div class="apple-tabs" :class="tabsClass">
    <!-- Tab导航 -->
    <div class="apple-tabs-nav" :class="navClass">
      <div class="apple-tabs-nav-wrap">
        <div class="apple-tabs-nav-list" ref="navListRef">
          <div
            v-for="(tab, index) in tabs"
            :key="tab.name || index"
            :class="['apple-tabs-item', getTabClass(tab)]"
            @click="handleTabClick(tab, index)"
          >
            <span class="apple-tabs-item-text">{{ tab.label || tab.name }}</span>
            <span
              v-if="tab.closable && !tab.disabled"
              class="apple-tabs-item-close"
              @click.stop="handleTabClose(tab, index)"
            >
              ×
            </span>
          </div>
        </div>

        <!-- 活动指示器 -->
        <div
          class="apple-tabs-indicator"
          :style="indicatorStyle"
        ></div>
      </div>

      <!-- 额外操作按钮 -->
      <div v-if="$slots.extra" class="apple-tabs-extra">
        <slot name="extra"></slot>
      </div>
    </div>

    <!-- Tab内容 -->
    <div class="apple-tabs-content">
      <slot></slot>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch, nextTick, onMounted, provide } from 'vue'

export default {
  name: 'AppleTabs',
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    },
    type: {
      type: String,
      default: 'line',
      validator: (value) => ['line', 'card', 'border-card'].includes(value)
    },
    tabPosition: {
      type: String,
      default: 'top',
      validator: (value) => ['top', 'bottom', 'left', 'right'].includes(value)
    },
    stretch: {
      type: Boolean,
      default: false
    },
    beforeLeave: {
      type: Function,
      default: null
    },
    editable: {
      type: Boolean,
      default: false
    },
    addable: {
      type: Boolean,
      default: false
    },
    closable: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'tab-click', 'tab-change', 'tab-remove', 'tab-add', 'edit'],
  setup(props, { emit, slots }) {
    const activeName = ref(props.modelValue)
    const navListRef = ref(null)
    const indicatorStyle = ref({})
    const tabs = ref([])

    // 收集所有的tab pane
    const collectTabs = () => {
      const tabPanes = []
      if (slots.default) {
        const defaultSlot = slots.default()
        defaultSlot.forEach(vnode => {
          if (vnode.type && vnode.type.name === 'AppleTabPane') {
            tabPanes.push(vnode.props || {})
          }
        })
      }
      tabs.value = tabPanes
    }

    const tabsClass = computed(() => {
      const classes = []

      if (props.type) {
        classes.push(`apple-tabs--${props.type}`)
      }

      if (props.tabPosition) {
        classes.push(`apple-tabs--${props.tabPosition}`)
      }

      if (props.stretch) {
        classes.push('apple-tabs--stretch')
      }

      return classes
    })

    const navClass = computed(() => {
      const classes = []

      if (props.tabPosition === 'left' || props.tabPosition === 'right') {
        classes.push('apple-tabs-nav--vertical')
      }

      return classes
    })

    const getTabClass = (tab) => {
      const classes = []

      if ((tab.name || tab.label) === activeName.value) {
        classes.push('apple-tabs-item--active')
      }

      if (tab.disabled) {
        classes.push('apple-tabs-item--disabled')
      }

      if (tab.closable || props.closable) {
        classes.push('apple-tabs-item--closable')
      }

      return classes
    }

    const handleTabClick = async (tab, index) => {
      if (tab.disabled) {
        return
      }

      const tabName = tab.name || tab.label || index

      // 如果有beforeLeave钩子，先执行
      if (props.beforeLeave) {
        try {
          const result = await props.beforeLeave(activeName.value, tabName)
          if (result === false) {
            return
          }
        } catch (error) {
          console.error('beforeLeave hook error:', error)
          return
        }
      }

      activeName.value = tabName
      emit('update:modelValue', tabName)
      emit('tab-click', tab)
      emit('tab-change', tabName)

      updateIndicator()
    }

    const handleTabClose = (tab, index) => {
      if (tab.disabled) {
        return
      }

      emit('tab-remove', tab)

      // 如果关闭的是当前激活的tab，需要切换到其他tab
      if ((tab.name || tab.label) === activeName.value) {
        const remainingTabs = tabs.value.filter((t, i) => i !== index)
        if (remainingTabs.length > 0) {
          const newActiveIndex = Math.min(index, remainingTabs.length - 1)
          const newActiveTab = remainingTabs[newActiveIndex]
          handleTabClick(newActiveTab, newActiveIndex)
        }
      }
    }

    const updateIndicator = async () => {
      await nextTick()

      if (!navListRef.value) return

      const activeItem = navListRef.value.querySelector('.apple-tabs-item--active')
      if (!activeItem) return

      const navList = navListRef.value
      const navLeft = navList.offsetLeft
      const itemLeft = activeItem.offsetLeft
      const itemWidth = activeItem.offsetWidth

      indicatorStyle.value = {
        left: `${itemLeft - navLeft}px`,
        width: `${itemWidth}px`
      }
    }

    // 监听modelValue变化
    watch(() => props.modelValue, (newValue) => {
      activeName.value = newValue
      updateIndicator()
    })

    // 监听activeName变化
    watch(activeName, updateIndicator)

    onMounted(() => {
      collectTabs()
      updateIndicator()
    })

    // 提供tabs context给子组件
    provide('tabsContext', {
      activeName
    })

    return {
      activeName,
      navListRef,
      indicatorStyle,
      tabs,
      tabsClass,
      navClass,
      getTabClass,
      handleTabClick,
      handleTabClose,
      updateIndicator
    }
  }
}
</script>

<style scoped>
.apple-tabs {
  width: 100%;
}

/* 导航栏 */
.apple-tabs-nav {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.apple-tabs-nav-wrap {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.apple-tabs-nav-list {
  display: flex;
  transition: transform 0.3s ease;
}

.apple-tabs-item {
  position: relative;
  padding: 12px 20px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
  border-radius: 8px;
  margin-right: 4px;
  background: var(--apple-bg-primary);
  border: 1px solid var(--apple-bg-quaternary);
  color: var(--apple-text-secondary);
  font-size: 14px;
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.apple-tabs-item:hover {
  background: var(--apple-bg-tertiary);
  color: var(--apple-text-primary);
}

.apple-tabs-item--active {
  background: var(--apple-blue);
  color: white;
  border-color: var(--apple-blue);
}

.apple-tabs-item--disabled {
  cursor: not-allowed;
  opacity: 0.5;
  color: var(--apple-text-tertiary);
}

.apple-tabs-item--closable {
  padding-right: 12px;
}

.apple-tabs-item-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.apple-tabs-item-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  font-size: 12px;
  line-height: 1;
  transition: all 0.2s ease;
  opacity: 0.7;
}

.apple-tabs-item-close:hover {
  background: rgba(255, 255, 255, 0.2);
  opacity: 1;
}

.apple-tabs-item--active .apple-tabs-item-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 活动指示器 */
.apple-tabs-indicator {
  position: absolute;
  bottom: 0;
  height: 2px;
  background: var(--apple-blue);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 1px;
}

/* 额外操作区域 */
.apple-tabs-extra {
  margin-left: 16px;
  display: flex;
  align-items: center;
}

/* 内容区域 */
.apple-tabs-content {
  position: relative;
  overflow: hidden;
}

/* 不同类型的样式 */
.apple-tabs--card .apple-tabs-item {
  border-radius: 8px 8px 0 0;
  border-bottom: none;
  margin-right: 2px;
}

.apple-tabs--card .apple-tabs-item--active {
  background: var(--apple-bg-primary);
  color: var(--apple-text-primary);
  border-bottom: 1px solid var(--apple-bg-primary);
  position: relative;
  z-index: 1;
}

.apple-tabs--card .apple-tabs-indicator {
  display: none;
}

.apple-tabs--border-card {
  border: 1px solid var(--apple-bg-quaternary);
  border-radius: 8px;
}

.apple-tabs--border-card .apple-tabs-nav {
  background: var(--apple-bg-secondary);
  border-bottom: 1px solid var(--apple-bg-quaternary);
  padding: 0 16px;
  margin-bottom: 0;
}

.apple-tabs--border-card .apple-tabs-item {
  border: none;
  border-bottom: 2px solid transparent;
  margin-right: 16px;
  background: transparent;
}

.apple-tabs--border-card .apple-tabs-item:hover {
  background: var(--apple-bg-tertiary);
}

.apple-tabs--border-card .apple-tabs-item--active {
  background: transparent;
  color: var(--apple-blue);
  border-bottom-color: var(--apple-blue);
}

.apple-tabs--border-card .apple-tabs-indicator {
  display: none;
}

/* 垂直导航 */
.apple-tabs-nav--vertical {
  flex-direction: column;
  margin-bottom: 0;
  margin-right: 16px;
}

.apple-tabs-nav--vertical .apple-tabs-nav-list {
  flex-direction: column;
}

.apple-tabs-nav--vertical .apple-tabs-item {
  margin-right: 0;
  margin-bottom: 4px;
}

.apple-tabs-nav--vertical .apple-tabs-indicator {
  top: 0;
  left: 0;
  right: auto;
  bottom: auto;
  width: 2px;
  height: auto;
}

.apple-tabs--left {
  display: flex;
  flex-direction: row;
}

.apple-tabs--right {
  display: flex;
  flex-direction: row-reverse;
}

/* 拉伸模式 */
.apple-tabs--stretch .apple-tabs-item {
  flex: 1;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-tabs-item {
    padding: 10px 16px;
    font-size: 13px;
  }

  .apple-tabs-nav {
    margin-bottom: 12px;
  }
}

/* 动画 */
@keyframes tabSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.apple-tabs-content > :deep(*) {
  animation: tabSlideIn 0.3s ease;
}
</style>