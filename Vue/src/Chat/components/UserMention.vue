<template>
  <span class="user-mention" @click="handleMentionClick" :class="{ 'mentioned': isCurrentUser }">
    <span class="mention-symbol">@</span>
    <span class="mention-username">{{ username }}</span>
    <span v-if="showTooltip" class="mention-tooltip">
      <div class="tooltip-content">
        <div class="tooltip-avatar">
          <img :src="userAvatar" :alt="username" />
        </div>
        <div class="tooltip-info">
          <div class="tooltip-name">{{ displayName }}</div>
          <div class="tooltip-role">{{ userRole }}</div>
        </div>
      </div>
    </span>
  </span>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  username: {
    type: String,
    required: true
  },
  userId: {
    type: [String, Number],
    required: true
  },
  displayName: {
    type: String,
    default: ''
  },
  userAvatar: {
    type: String,
    default: ''
  },
  userRole: {
    type: String,
    default: ''
  },
  isCurrentUser: {
    type: Boolean,
    default: false
  },
  showTooltip: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['mention-click'])

const tooltipVisible = ref(false)

// 计算显示名称
const displayName = computed(() => {
  return props.displayName || props.username
})

// 处理提及点击
const handleMentionClick = () => {
  emit('mention-click', {
    username: props.username,
    userId: props.userId,
    displayName: displayName.value
  })
}

// 鼠标悬停显示提示
const handleMouseEnter = () => {
  if (props.showTooltip) {
    tooltipVisible.value = true
  }
}

// 鼠标离开隐藏提示
const handleMouseLeave = () => {
  tooltipVisible.value = false
}

onMounted(() => {
  // 可以在这里添加键盘快捷键支持等
})

onUnmounted(() => {
  // 清理工作
})
</script>

<style scoped>
.user-mention {
  display: inline-flex;
  align-items: center;
  background: #e3f2fd;
  color: #1976d2;
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.user-mention:hover {
  background: #bbdefb;
  transform: scale(1.05);
}

.user-mention.mentioned {
  background: #fff3e0;
  color: #f57c00;
}

.mention-symbol {
  font-weight: bold;
  margin-right: 2px;
}

.mention-username {
  font-weight: 500;
}

.mention-tooltip {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  white-space: nowrap;
  z-index: 1000;
  margin-bottom: 5px;
  opacity: 0;
  visibility: hidden;
  transition: all 0.2s ease;
}

.user-mention:hover .mention-tooltip {
  opacity: 1;
  visibility: visible;
}

.tooltip-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tooltip-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.tooltip-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.tooltip-info {
  display: flex;
  flex-direction: column;
}

.tooltip-name {
  font-weight: 500;
  line-height: 1.2;
}

.tooltip-role {
  font-size: 11px;
  opacity: 0.8;
  line-height: 1.2;
}

/* 箭头 */
.mention-tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: rgba(0, 0, 0, 0.8);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-mention {
    padding: 1px 4px;
    font-size: 13px;
  }

  .mention-tooltip {
    font-size: 11px;
    padding: 6px 8px;
  }

  .tooltip-avatar {
    width: 20px;
    height: 20px;
  }
}
</style>
