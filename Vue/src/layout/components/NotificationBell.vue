<template>
  <div class="notification-bell">
    <!-- 通知铃铛按钮 -->
    <button class="bell-button" @click="togglePopover" ref="bellButton">
      <div class="bell-icon">🔔</div>
      <!-- 未读通知徽章 -->
      <div v-if="unreadCount > 0" class="notification-badge apple-badge">
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </div>
    </button>

    <!-- 通知弹窗 -->
    <div v-if="showPopover" class="notification-popover apple-card">
      <div class="popover-header">
        <h3 class="popover-title">通知</h3>
        <button class="close-button" @click="closePopover">✕</button>
      </div>

      <div class="notification-list">
        <div v-if="loading" class="loading-container">
          <div class="apple-loading"></div>
          <span class="loading-text">加载中...</span>
        </div>

        <div v-else-if="notifications.length === 0" class="empty-state">
          <div class="empty-icon">📭</div>
          <p class="empty-text">暂无通知</p>
        </div>

        <div v-else class="notifications">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            class="notification-item"
            :class="{ unread: !notification.isRead }"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-icon">
              {{ getNotificationIcon(notification.type) }}
            </div>
            <div class="notification-content">
              <div class="notification-text">{{ notification.content }}</div>
              <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
            </div>
            <div v-if="!notification.isRead" class="unread-dot"></div>
          </div>
        </div>
      </div>

      <div class="popover-footer">
        <button class="apple-button apple-button-secondary footer-button" @click="markAllAsRead">
          全部标记为已读
        </button>
        <router-link to="/notifications" class="view-all-link">
          查看全部
        </router-link>
      </div>
    </div>

    <!-- 点击外部关闭弹窗 -->
    <div v-if="showPopover" class="popover-overlay" @click="closePopover"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notificationStore'
import { ElMessage } from 'element-plus'

const router = useRouter()
const notificationStore = useNotificationStore()

// 响应式数据
const showPopover = ref(false)
const loading = ref(false)
const bellButton = ref(null)

// 计算属性
const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)

// 切换弹窗显示
const togglePopover = () => {
  showPopover.value = !showPopover.value
  if (showPopover.value) {
    loadNotifications()
  }
}

// 关闭弹窗
const closePopover = () => {
  showPopover.value = false
}

// 加载通知
const loadNotifications = async () => {
  if (notifications.value.length === 0) {
    loading.value = true
    try {
      await notificationStore.fetchNotifications()
    } catch (error) {
      console.error('加载通知失败:', error)
      ElMessage.error('加载通知失败')
    } finally {
      loading.value = false
    }
  }
}

// 处理通知点击
const handleNotificationClick = async (notification) => {
  if (!notification.isRead) {
    try {
      await notificationStore.markAsRead(notification.id)
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
  
  // 根据通知类型跳转到相应页面
  switch (notification.type) {
    case 'ARTICLE_LIKE':
      router.push(`/article/read/${notification.entityId}`)
      break
    case 'NEW_FOLLOWER':
      router.push(`/user/${notification.actorId}`)
      break
    case 'COMMENT_MENTION':
      router.push(`/article/read/${notification.entityId}`)
      break
    default:
      router.push('/notifications')
  }
  
  closePopover()
}

// 全部标记为已读
const markAllAsRead = async () => {
  try {
    await notificationStore.markAllAsRead()
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    console.error('标记全部已读失败:', error)
    ElMessage.error('操作失败')
  }
}

// 获取通知图标
const getNotificationIcon = (type) => {
  const icons = {
    'ARTICLE_LIKE': '👍',
    'NEW_FOLLOWER': '👤',
    'COMMENT_MENTION': '💬',
    'SYSTEM': '🔔',
    'default': '📢'
  }
  return icons[type] || icons.default
}

// 格式化时间
const formatTime = (timestamp) => {
  const now = new Date()
  const time = new Date(timestamp)
  const diff = now - time
  
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return time.toLocaleDateString()
}

// 点击外部关闭
const handleClickOutside = (event) => {
  if (bellButton.value && !bellButton.value.contains(event.target)) {
    closePopover()
  }
}

// 生命周期
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.notification-bell {
  position: relative;
}

.bell-button {
  position: relative;
  background: none;
  border: none;
  padding: var(--apple-spacing-sm);
  border-radius: var(--apple-radius-medium);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bell-button:hover {
  background-color: var(--apple-bg-secondary);
}

.bell-icon {
  font-size: var(--apple-font-lg);
  color: var(--apple-text-secondary);
}

.notification-badge {
  position: absolute;
  top: 0;
  right: 0;
  transform: translate(50%, -50%);
  min-width: 18px;
  height: 18px;
  font-size: var(--apple-font-xs);
  font-weight: 600;
}

.notification-popover {
  position: absolute;
  top: 100%;
  right: 0;
  width: 360px;
  max-height: 480px;
  margin-top: var(--apple-spacing-sm);
  z-index: var(--z-popover);
  box-shadow: var(--apple-shadow-heavy);
  border: 1px solid var(--apple-border-secondary);
}

.popover-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--apple-spacing-md) var(--apple-spacing-lg);
  border-bottom: 1px solid var(--apple-border-secondary);
}

.popover-title {
  margin: 0;
  font-size: var(--apple-font-lg);
  font-weight: 600;
  color: var(--apple-text-primary);
}

.close-button {
  background: none;
  border: none;
  font-size: var(--apple-font-md);
  color: var(--apple-text-tertiary);
  cursor: pointer;
  padding: var(--apple-spacing-xs);
  border-radius: var(--apple-radius-small);
  transition: all 0.2s ease;
}

.close-button:hover {
  background-color: var(--apple-bg-secondary);
  color: var(--apple-text-secondary);
}

.notification-list {
  max-height: 320px;
  overflow-y: auto;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--apple-spacing-xl);
  gap: var(--apple-spacing-md);
}

.loading-text {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-tertiary);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--apple-spacing-xl);
  gap: var(--apple-spacing-md);
}

.empty-icon {
  font-size: 48px;
  color: var(--apple-text-quaternary);
}

.empty-text {
  font-size: var(--apple-font-md);
  color: var(--apple-text-tertiary);
  margin: 0;
}

.notifications {
  padding: var(--apple-spacing-sm);
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: var(--apple-spacing-md);
  border-radius: var(--apple-radius-medium);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  gap: var(--apple-spacing-sm);
}

.notification-item:hover {
  background-color: var(--apple-bg-secondary);
}

.notification-item.unread {
  background-color: rgba(0, 122, 255, 0.05);
}

.notification-item.unread:hover {
  background-color: rgba(0, 122, 255, 0.1);
}

.notification-icon {
  font-size: var(--apple-font-lg);
  margin-top: 2px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-primary);
  line-height: 1.4;
  margin-bottom: var(--apple-spacing-xs);
  word-break: break-word;
}

.notification-time {
  font-size: var(--apple-font-xs);
  color: var(--apple-text-tertiary);
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--apple-blue);
  flex-shrink: 0;
  margin-top: 4px;
}

.popover-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--apple-spacing-md) var(--apple-spacing-lg);
  border-top: 1px solid var(--apple-border-secondary);
}

.footer-button {
  height: 32px;
  font-size: var(--apple-font-sm);
}

.view-all-link {
  font-size: var(--apple-font-sm);
  color: var(--apple-blue);
  text-decoration: none;
  font-weight: 500;
}

.view-all-link:hover {
  text-decoration: underline;
}

.popover-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: calc(var(--z-popover) - 1);
  background-color: transparent;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .notification-popover {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 90vw;
    max-width: 360px;
    max-height: 80vh;
  }
  
  .popover-overlay {
    background-color: rgba(0, 0, 0, 0.5);
  }
}

/* 主题适配 */
.notification-popover {
  background-color: var(--apple-bg-primary);
  border-color: var(--apple-border-secondary);
}

.popover-header {
  border-bottom-color: var(--apple-border-secondary);
}

.popover-footer {
  border-top-color: var(--apple-border-secondary);
}

.bell-button:hover {
  background-color: var(--apple-bg-tertiary);
}

.bell-icon {
  color: var(--apple-text-secondary);
}

.popover-title {
  color: var(--apple-text-primary);
}

.close-button {
  color: var(--apple-text-tertiary);
}

.close-button:hover {
  background-color: var(--apple-bg-tertiary);
  color: var(--apple-text-secondary);
}

.notification-item:hover {
  background-color: var(--apple-bg-tertiary);
}

.notification-text {
  color: var(--apple-text-primary);
}

.notification-time {
  color: var(--apple-text-tertiary);
}

.loading-text,
.empty-text {
  color: var(--apple-text-tertiary);
}
</style> 