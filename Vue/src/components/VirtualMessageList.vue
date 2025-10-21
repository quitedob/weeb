<template>
  <div class="virtual-message-container" ref="containerRef">
    <div
      ref="scrollElementRef"
      class="virtual-message-scroll"
      :style="{ height: `${totalSize}px`, position: 'relative' }"
      @scroll="handleScroll"
    >
      <div
        :style="{
          height: `${virtualizer.getTotalSize()}px`,
          width: '100%',
          position: 'relative',
        }"
      >
        <div
          v-for="virtualRow in virtualizer.getVirtualItems()"
          :key="virtualRow.index"
          :style="{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: `${virtualRow.size}px`,
            transform: `translateY(${virtualRow.start}px)`,
          }"
          class="virtual-message-item"
        >
          <slot
            :message="msgRecord[virtualRow.index]"
            :index="virtualRow.index"
            :virtualRow="virtualRow"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'

const props = defineProps({
  msgRecord: {
    type: Array,
    required: true
  },
  height: {
    type: Number,
    default: 500
  },
  itemHeight: {
    type: Number,
    default: 80
  }
})

const containerRef = ref(null)
const scrollElementRef = ref(null)

// 计算消息实际高度（可以根据消息内容动态计算）
const getItemHeight = (index) => {
  const message = props.msgRecord[index]
  if (!message) return props.itemHeight

  // 简单的高度估算逻辑，可以根据实际消息内容进行优化
  let estimatedHeight = props.itemHeight

  // 如果消息包含长文本，增加高度
  if (message.content && message.content.length > 100) {
    estimatedHeight += Math.floor(message.content.length / 50) * 20
  }

  // 如果是图片消息，增加高度
  if (message.contentType === 'image') {
    estimatedHeight += 100
  }

  // 如果是文件消息，增加高度
  if (message.contentType === 'file') {
    estimatedHeight += 40
  }

  return estimatedHeight
}

// 创建虚拟化器
const virtualizer = useVirtualizer({
  count: computed(() => props.msgRecord.length),
  getScrollElement: () => scrollElementRef.value,
  estimateSize: getItemHeight,
  overscan: 5, // 预渲染额外的项目数
})

// 处理滚动事件
const handleScroll = (event) => {
  virtualizer.scrollToOffset(event.target.scrollTop)
}

// 滚动到底部的方法
const scrollToBottom = () => {
  if (virtualizer.value && props.msgRecord.length > 0) {
    const lastItemIndex = props.msgRecord.length - 1
    const lastItem = virtualizer.value.getVirtualItems().find(item => item.index === lastItemIndex)
    if (lastItem) {
      scrollElementRef.value?.scrollTo({
        top: lastItem.start,
        behavior: 'smooth'
      })
    }
  }
}

// 滚动到指定消息
const scrollToMessage = (messageIndex) => {
  if (virtualizer.value && messageIndex >= 0 && messageIndex < props.msgRecord.length) {
    const virtualItem = virtualizer.value.getVirtualItems().find(item => item.index === messageIndex)
    if (virtualItem) {
      scrollElementRef.value?.scrollTo({
        top: virtualItem.start,
        behavior: 'smooth'
      })
    }
  }
}

// 监听消息变化，自动滚动到底部
watch(() => props.msgRecord.length, (newLength, oldLength) => {
  if (newLength > oldLength) {
    // 延迟滚动以确保DOM已更新
    setTimeout(() => {
      scrollToBottom()
    }, 100)
  }
})

// 暴露方法给父组件
defineExpose({
  scrollToBottom,
  scrollToMessage
})

onMounted(() => {
  // 初始滚动到底部
  if (props.msgRecord.length > 0) {
    setTimeout(() => {
      scrollToBottom()
    }, 100)
  }
})
</script>

<style scoped>
.virtual-message-container {
  height: 100%;
  overflow: hidden;
  position: relative;
}

.virtual-message-scroll {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: #888 #f1f1f1;
}

.virtual-message-scroll::-webkit-scrollbar {
  width: 6px;
}

.virtual-message-scroll::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.virtual-message-scroll::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

.virtual-message-scroll::-webkit-scrollbar-thumb:hover {
  background: #555;
}

.virtual-message-item {
  padding: 4px 8px;
  box-sizing: border-box;
}

/* 消息项样式保持与原始聊天页面一致 */
:deep(.msg-item) {
  display: flex;
  margin-bottom: 12px;
  align-items: flex-end;
}

:deep(.sent-message) {
  background-color: #007bff;
  color: white;
}

:deep(.received-message) {
  background-color: #f1f1f1;
  color: #333;
}
</style>