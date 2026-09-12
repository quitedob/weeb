<template>
  <div class="notification-list-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <h2>通知中心</h2>
          <div class="header-actions">
            <el-button
              v-if="unreadCount > 0"
              type="primary"
              @click="markAllAsRead"
              :loading="isMarkingAllAsRead"
            >
              全部已读
            </el-button>
            <el-button
              type="danger"
              @click="deleteReadNotifications"
              :loading="isDeleting"
            >
              清空已读
            </el-button>
          </div>
        </div>
      </template>

      <!-- 通知列表 -->
      <div class="notification-container">
        <div v-if="notificationStore.hasNewer" class="load-more">
          <el-button :disabled="isLoading" @click="navigateWindow('newer')">较新通知</el-button>
          <el-button :disabled="isLoading" @click="navigateWindow('latest')">返回最新</el-button>
        </div>
        <div v-if="isLoading && notifications.length === 0" class="loading-state">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="notifications.length === 0" class="empty-state">
          <el-empty description="暂无通知" :image-size="120" />
        </div>

        <div v-else class="notification-list">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            class="notification-item"
            :class="{ unread: !notification.isRead }"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-avatar">
              <el-avatar :size="40" :src="getNotificationAvatar(notification)" />
            </div>
            <div class="notification-content">
              <div class="notification-header">
                <span class="notification-type">{{ getNotificationTypeText(notification.type) }}</span>
                <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
              </div>
              <div class="notification-text">
                {{ getNotificationText(notification) }}
              </div>
              <div class="notification-actions">
                <el-button
                  v-if="!notification.isRead"
                  type="text"
                  size="small"
                  @click.stop="markAsRead(notification.id)"
                >
                  标记已读
                </el-button>
              </div>
            </div>
            <div v-if="!notification.isRead" class="notification-dot"></div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMoreNotifications" class="load-more">
          <el-button
            v-if="!isLoading"
            type="text"
            @click="loadMoreNotifications"
          >
            加载更多
          </el-button>
          <el-button v-else type="text" disabled>
            加载中...
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useNotificationStore } from '@/stores/notificationStore';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getNotificationRoute, getNotificationTypeText, getNotificationText } from '@/utils/notificationPresentation';
import { captureSession, isCurrentSession } from '@/utils/session';

const router = useRouter();
const notificationStore = useNotificationStore();

// 响应式数据
const isMarkingAllAsRead = ref(false);
const isDeleting = ref(false);

// 计算属性
const notifications = computed(() => notificationStore.notifications);
const unreadCount = computed(() => notificationStore.unreadCount);
const isLoading = computed(() => notificationStore.isLoading);
const hasMoreNotifications = computed(() => {
  return notificationStore.hasMore;
});

// 方法
const markAllAsRead = async () => {
  const session = captureSession();
  try {
    isMarkingAllAsRead.value = true;
    await notificationStore.markAllAsRead();
    if (!isCurrentSession(session)) return;
    ElMessage.success('已标记所有通知为已读');
  } catch (error) {
    if (!isCurrentSession(session)) return;
    ElMessage.error('操作失败，请重试');
  } finally {
    if (isCurrentSession(session)) isMarkingAllAsRead.value = false;
  }
};

const deleteReadNotifications = async () => {
  const session = captureSession();
  try {
    await ElMessageBox.confirm(
      '确定要删除所有已读通知吗？此操作不可恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    if (!isCurrentSession(session)) return;
    isDeleting.value = true;
    await notificationStore.deleteReadNotifications();
    if (!isCurrentSession(session)) return;
    ElMessage.success('已删除所有已读通知');
  } catch (error) {
    if (!isCurrentSession(session)) return;
    if (error !== 'cancel') {
      ElMessage.error('删除失败，请重试');
    }
  } finally {
    if (isCurrentSession(session)) isDeleting.value = false;
  }
};

const markAsRead = async (notificationId) => {
  try {
    await notificationStore.markAsRead(notificationId);
  } catch (error) {
    ElMessage.error('标记已读失败，请重试');
  }
};

const handleNotificationClick = async (notification) => {
  const session = captureSession();
  // 如果通知未读，标记为已读
  if (!notification.isRead) {
    try {
      await notificationStore.markAsRead(notification.id);
    } catch (error) {
      console.error('标记通知为已读失败:', error);
    }
  }
  
  // 根据通知类型跳转到相应页面
  if (!isCurrentSession(session)) return;
  router.push(getNotificationRoute(notification));
};

const loadMoreNotifications = async () => {
  try {
    await notificationStore.fetchNotifications(notificationStore.currentPage + 1);
  } catch (error) {
    ElMessage.error('加载更多通知失败，请重试');
  }
};

const navigateWindow = async (direction) => {
  const session = captureSession();
  try {
    if (direction === 'latest') await notificationStore.loadLatestNotifications();
    else await notificationStore.loadNewerNotifications();
  } catch (error) {
    if (isCurrentSession(session)) ElMessage.error('加载通知失败，请重试');
  }
};

const getNotificationAvatar = (notification) => {
  // 这里可以根据通知类型返回不同的头像
  // 实际项目中可能需要从用户信息中获取头像
  return '';
};

const formatTime = (timeString) => {
  if (!timeString) return '';
  
  const now = new Date();
  const time = new Date(timeString);
  const diff = now - time;
  
  const minutes = Math.floor(diff / (1000 * 60));
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  
  return time.toLocaleDateString();
};

// 生命周期
onMounted(async () => {
  try {
    // 获取通知列表
    await notificationStore.fetchNotifications(1);
  } catch (error) {
    console.error('加载通知列表失败:', error);
    ElMessage.error('加载通知列表失败，请重试');
  }
});
</script>

<style scoped>
.notification-list-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.notification-container {
  min-height: 400px;
}

.loading-state {
  padding: 20px;
}

.empty-state {
  padding: 40px 20px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  border: 1px solid #ebeef5;
}

.notification-item:hover {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
}

.notification-item.unread {
  background-color: #f0f9ff;
  border-color: #409eff;
}

.notification-avatar {
  margin-right: 16px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-type {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  background-color: #ecf5ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  margin-bottom: 8px;
}

.notification-actions {
  display: flex;
  gap: 8px;
}

.notification-dot {
  width: 8px;
  height: 8px;
  background-color: #409eff;
  border-radius: 50%;
  position: absolute;
  top: 20px;
  right: 20px;
}

.load-more {
  text-align: center;
  padding: 20px 0;
}

@media (max-width: 768px) {
  .notification-list-page {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .notification-item {
    padding: 12px;
  }
  
  .notification-avatar {
    margin-right: 12px;
  }
}
</style>
