<template>
  <div class="custom-status">
    <!-- 当前状态显示 -->
    <div class="status-display" @click="showStatusSelector = true">
      <div class="status-icon">
        <el-icon v-if="currentStatus.icon" :class="currentStatus.icon" />
        <span v-else>{{ currentStatus.emoji || '😊' }}</span>
      </div>
      <div class="status-text">
        <div class="status-message">{{ currentStatus.message || '设置状态' }}</div>
        <div class="status-time">{{ formatTime(currentStatus.setTime) }}</div>
      </div>
      <div class="status-arrow">
        <el-icon><ArrowDown /></el-icon>
      </div>
    </div>

    <!-- 状态选择器弹窗 -->
    <div v-if="showStatusSelector" class="status-selector-modal">
      <div class="status-selector">
        <!-- 预设状态 -->
        <div class="status-section">
          <div class="section-title">快速状态</div>
          <div class="preset-statuses">
            <button
              v-for="status in presetStatuses"
              :key="status.id"
              class="status-option"
              :class="{ active: currentStatus.id === status.id }"
              @click="selectStatus(status)"
            >
              <div class="status-icon">
                <el-icon v-if="status.icon" :class="status.icon" />
                <span v-else>{{ status.emoji }}</span>
              </div>
              <div class="status-text">
                <div class="status-message">{{ status.message }}</div>
              </div>
            </button>
          </div>
        </div>

        <!-- 自定义状态 -->
        <div class="status-section">
          <div class="section-title">自定义状态</div>
          <div class="custom-status-input">
            <div class="emoji-selector">
              <button
                v-for="emoji in commonEmojis"
                :key="emoji"
                class="emoji-btn"
                @click="selectedEmoji = emoji"
              >
                {{ emoji }}
              </button>
            </div>
            <el-input
              v-model="customMessage"
              placeholder="输入自定义状态..."
              maxlength="50"
              show-word-limit
            />
            <div class="custom-actions">
              <el-button size="small" @click="clearCustomStatus">清除</el-button>
              <el-button type="primary" size="small" @click="setCustomStatus">设置</el-button>
            </div>
          </div>
        </div>

        <!-- 状态历史 -->
        <div class="status-section">
          <div class="section-title">最近状态</div>
          <div class="status-history">
            <button
              v-for="status in recentStatuses"
              :key="status.id"
              class="status-option small"
              @click="selectStatus(status)"
            >
              <div class="status-icon">
                <el-icon v-if="status.icon" :class="status.icon" />
                <span v-else>{{ status.emoji }}</span>
              </div>
              <div class="status-text">
                <div class="status-message">{{ status.message }}</div>
                <div class="status-time">{{ formatTime(status.setTime) }}</div>
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 清空状态按钮 -->
    <div v-if="currentStatus.id" class="status-actions">
      <el-button size="small" text type="danger" @click="clearStatus">
        清空状态
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'

const props = defineProps({
  userId: {
    type: [String, Number],
    required: true
  },
  currentStatus: {
    type: Object,
    default: () => ({
      id: '',
      message: '',
      emoji: '',
      icon: '',
      setTime: null
    })
  }
})

const emit = defineEmits(['status-change'])

const showStatusSelector = ref(false)
const selectedEmoji = ref('😊')
const customMessage = ref('')

// 预设状态选项
const presetStatuses = ref([
  { id: 'online', message: '在线', emoji: '🟢', icon: '' },
  { id: 'busy', message: '忙碌', emoji: '🔴', icon: '' },
  { id: 'away', message: '离开', emoji: '🟡', icon: '' },
  { id: 'meeting', message: '会议中', emoji: '📅', icon: '' },
  { id: 'lunch', message: '吃午饭', emoji: '🍽️', icon: '' },
  { id: 'commute', message: '通勤中', emoji: '🚗', icon: '' },
  { id: 'vacation', message: '度假中', emoji: '🏖️', icon: '' },
  { id: 'sick', message: '生病中', emoji: '🤒', icon: '' }
])

// 常用表情
const commonEmojis = ref([
  '😊', '😂', '😍', '🤔', '😎', '😢', '😡', '👍', '👎', '❤️', '🔥', '💯'
])

// 最近状态（模拟数据）
const recentStatuses = ref([
  { id: 'custom-1', message: '工作很忙', emoji: '💼', setTime: new Date(Date.now() - 3600000) },
  { id: 'custom-2', message: '学习新知识', emoji: '📚', setTime: new Date(Date.now() - 86400000) }
])

// 点击外部关闭弹窗
const handleClickOutside = (event) => {
  const selector = document.querySelector('.status-selector-modal')
  if (selector && !selector.contains(event.target)) {
    showStatusSelector.value = false
  }
}

// 选择状态
const selectStatus = (status) => {
  emit('status-change', {
    id: status.id,
    message: status.message,
    emoji: status.emoji,
    icon: status.icon,
    setTime: new Date()
  })
  showStatusSelector.value = false
}

// 设置自定义状态
const setCustomStatus = () => {
  if (!customMessage.value.trim()) {
    ElMessage.warning('请输入状态消息')
    return
  }

  emit('status-change', {
    id: `custom-${Date.now()}`,
    message: customMessage.value.trim(),
    emoji: selectedEmoji.value,
    icon: '',
    setTime: new Date()
  })

  showStatusSelector.value = false
  customMessage.value = ''
}

// 清空状态
const clearStatus = () => {
  emit('status-change', {
    id: '',
    message: '',
    emoji: '',
    icon: '',
    setTime: null
  })
}

// 清空自定义状态输入
const clearCustomStatus = () => {
  customMessage.value = ''
  selectedEmoji.value = '😊'
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) { // 1分钟内
    return '刚刚'
  } else if (diff < 3600000) { // 1小时内
    return Math.floor(diff / 60000) + '分钟前'
  } else if (diff < 86400000) { // 24小时内
    return Math.floor(diff / 3600000) + '小时前'
  } else {
    return date.toLocaleDateString()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

// 组件销毁时清理事件监听
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.custom-status {
  position: relative;
}

.status-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.status-display:hover {
  background: #e9ecef;
  border-color: #409eff;
}

.status-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.status-text {
  flex: 1;
  min-width: 0;
}

.status-message {
  font-size: 14px;
  color: #2c3e50;
  line-height: 1.2;
}

.status-time {
  font-size: 11px;
  color: #6c757d;
  line-height: 1;
}

.status-arrow {
  color: #6c757d;
  transition: transform 0.2s ease;
}

.status-display:hover .status-arrow {
  transform: rotate(180deg);
}

/* 状态选择器弹窗 */
.status-selector-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.status-selector {
  background: white;
  border-radius: 8px;
  padding: 20px;
  max-width: 400px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.status-section {
  margin-bottom: 24px;
}

.status-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 12px;
  padding-bottom: 4px;
  border-bottom: 1px solid #e9ecef;
}

.preset-statuses {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.status-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: none;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  width: 100%;
}

.status-option:hover {
  background: #f8f9fa;
  border-color: #409eff;
}

.status-option.active {
  background: #e3f2fd;
  border-color: #409eff;
}

.status-option.small {
  padding: 6px 8px;
}

.emoji-selector {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.emoji-btn {
  background: none;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  padding: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s ease;
}

.emoji-btn:hover {
  background: #f0f0f0;
  border-color: #409eff;
}

.custom-status-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.custom-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.status-history {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-actions {
  margin-top: 8px;
  text-align: right;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .status-display {
    padding: 6px 10px;
  }

  .status-selector {
    margin: 20px;
    max-width: calc(100vw - 40px);
  }

  .preset-statuses {
    grid-template-columns: 1fr;
  }
}
</style>
