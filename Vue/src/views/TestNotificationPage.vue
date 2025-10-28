<template>
  <div class="test-notification-page">
    <div class="page-header">
      <h1>通知测试页面</h1>
      <p>用于测试系统通知功能</p>
    </div>

    <div class="test-section">
      <h2>发送测试通知</h2>
      <div class="test-controls">
        <button @click="sendTestNotification('info')" class="btn btn-info">
          发送信息通知
        </button>
        <button @click="sendTestNotification('success')" class="btn btn-success">
          发送成功通知
        </button>
        <button @click="sendTestNotification('warning')" class="btn btn-warning">
          发送警告通知
        </button>
        <button @click="sendTestNotification('error')" class="btn btn-error">
          发送错误通知
        </button>
      </div>
    </div>

    <div class="test-section">
      <h2>最近的通知</h2>
      <div v-if="notifications.length === 0" class="empty-state">
        暂无通知
      </div>
      <div v-else class="notification-list">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          :class="['notification-item', `notification-${notification.type}`]"
        >
          <div class="notification-content">
            <strong>{{ notification.title }}</strong>
            <p>{{ notification.message }}</p>
            <small>{{ formatTime(notification.time) }}</small>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

const notifications = ref([]);

const sendTestNotification = (type) => {
  const titles = {
    info: '信息通知',
    success: '成功通知',
    warning: '警告通知',
    error: '错误通知'
  };

  const messages = {
    info: '这是一条测试信息通知',
    success: '操作成功完成！',
    warning: '请注意这个警告信息',
    error: '发生了一个错误'
  };

  const notification = {
    id: Date.now(),
    type,
    title: titles[type],
    message: messages[type],
    time: new Date()
  };

  notifications.value.unshift(notification);

  ElMessage({
    type,
    message: `${titles[type]}: ${messages[type]}`
  });
};

const formatTime = (time) => {
  return new Date(time).toLocaleString('zh-CN');
};
</script>

<style scoped>
.test-notification-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e0e0e0;
}

.page-header h1 {
  margin: 0 0 10px 0;
  color: #333;
}

.page-header p {
  margin: 0;
  color: #666;
}

.test-section {
  margin-bottom: 30px;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.test-section h2 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 18px;
}

.test-controls {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: white;
  transition: opacity 0.2s;
}

.btn:hover {
  opacity: 0.8;
}

.btn-info {
  background-color: #409eff;
}

.btn-success {
  background-color: #67c23a;
}

.btn-warning {
  background-color: #e6a23c;
}

.btn-error {
  background-color: #f56c6c;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-item {
  padding: 15px;
  border-radius: 4px;
  border-left: 4px solid;
  background: white;
}

.notification-info {
  border-left-color: #409eff;
}

.notification-success {
  border-left-color: #67c23a;
}

.notification-warning {
  border-left-color: #e6a23c;
}

.notification-error {
  border-left-color: #f56c6c;
}

.notification-content strong {
  display: block;
  margin-bottom: 5px;
  color: #333;
}

.notification-content p {
  margin: 0 0 5px 0;
  color: #666;
}

.notification-content small {
  color: #999;
  font-size: 12px;
}
</style>
