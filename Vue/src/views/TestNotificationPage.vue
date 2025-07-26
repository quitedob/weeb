<template>
  <div class="test-notification-page">
    <el-card>
      <template #header>
        <h2>通知系统测试页面</h2>
      </template>
      
      <div class="test-controls">
        <el-button type="primary" @click="createTestNotification">
          创建测试通知
        </el-button>
        <el-button type="success" @click="fetchNotifications">
          获取通知列表
        </el-button>
        <el-button type="warning" @click="markAllAsRead">
          全部标记为已读
        </el-button>
        <el-button type="danger" @click="deleteReadNotifications">
          删除已读通知
        </el-button>
      </div>

      <div class="notification-info">
        <h3>通知信息</h3>
        <p>未读通知数量: {{ unreadCount }}</p>
        <p>总通知数量: {{ totalNotifications }}</p>
      </div>

      <div class="notification-list" v-if="notifications.length > 0">
        <h3>通知列表</h3>
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: !notification.isRead }"
        >
          <div class="notification-content">
            <div class="notification-header">
              <span class="notification-type">{{ notification.type }}</span>
              <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
            </div>
            <div class="notification-details">
              <p>ID: {{ notification.id }}</p>
              <p>接收者: {{ notification.recipientId }}</p>
              <p>触发者: {{ notification.actorId }}</p>
              <p>实体类型: {{ notification.entityType }}</p>
              <p>实体ID: {{ notification.entityId }}</p>
              <p>已读状态: {{ notification.isRead ? '已读' : '未读' }}</p>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="no-notifications">
        <el-empty description="暂无通知" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useNotificationStore } from '@/stores/notificationStore';
import { ElMessage } from 'element-plus';
import notificationApi from '@/api/modules/notification';

const notificationStore = useNotificationStore();

// 响应式数据
const notifications = ref([]);
const unreadCount = ref(0);
const totalNotifications = ref(0);

// 方法
const createTestNotification = async () => {
  try {
    const response = await notificationApi.createTestNotification();
    if (response.data && response.data.code === 0) {
      ElMessage.success('测试通知创建成功');
      await fetchNotifications();
    } else {
      ElMessage.error('创建测试通知失败');
    }
  } catch (error) {
    console.error('创建测试通知失败:', error);
    ElMessage.error('创建测试通知失败');
  }
};

const fetchNotifications = async () => {
  try {
    const response = await notificationApi.getNotifications(1, 50);
    if (response.data && response.data.code === 0) {
      const data = response.data.data;
      notifications.value = data.notifications || [];
      unreadCount.value = data.unreadCount || 0;
      totalNotifications.value = notifications.value.length;
    }
  } catch (error) {
    console.error('获取通知列表失败:', error);
    ElMessage.error('获取通知列表失败');
  }
};

const markAllAsRead = async () => {
  try {
    const response = await notificationApi.markAllAsRead();
    if (response.data && response.data.code === 0) {
      ElMessage.success('已标记所有通知为已读');
      await fetchNotifications();
    } else {
      ElMessage.error('标记已读失败');
    }
  } catch (error) {
    console.error('标记已读失败:', error);
    ElMessage.error('标记已读失败');
  }
};

const deleteReadNotifications = async () => {
  try {
    const response = await notificationApi.deleteReadNotifications();
    if (response.data && response.data.code === 0) {
      ElMessage.success('已删除已读通知');
      await fetchNotifications();
    } else {
      ElMessage.error('删除已读通知失败');
    }
  } catch (error) {
    console.error('删除已读通知失败:', error);
    ElMessage.error('删除已读通知失败');
  }
};

const formatTime = (timeString) => {
  if (!timeString) return '';
  const time = new Date(timeString);
  return time.toLocaleString();
};

// 生命周期
onMounted(async () => {
  await fetchNotifications();
});
</script>

<style scoped>
.test-notification-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.test-controls {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.notification-info {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.notification-info h3 {
  margin: 0 0 12px 0;
  color: #303133;
}

.notification-info p {
  margin: 4px 0;
  color: #606266;
}

.notification-list {
  margin-top: 20px;
}

.notification-list h3 {
  margin: 0 0 16px 0;
  color: #303133;
}

.notification-item {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  background-color: #fff;
}

.notification-item.unread {
  background-color: #f0f9ff;
  border-color: #409eff;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.notification-type {
  font-weight: 600;
  color: #409eff;
  background-color: #ecf5ff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-details p {
  margin: 4px 0;
  font-size: 14px;
  color: #606266;
}

.no-notifications {
  text-align: center;
  padding: 40px 20px;
}

@media (max-width: 768px) {
  .test-controls {
    flex-direction: column;
  }
  
  .test-controls .el-button {
    width: 100%;
  }
}
</style> 