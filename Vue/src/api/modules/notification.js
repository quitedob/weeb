import { instance } from '../axiosInstance';

export default {
  // 获取通知列表
  getNotifications(page = 1, size = 10) {
    return instance.get('/api/notifications', {
      params: { page, size }
    });
  },

  // 获取未读通知数量
  getUnreadCount() {
    return instance.get('/api/notifications/unread-count');
  },

  // 标记指定通知为已读
  markAsRead(notificationId) {
    return instance.post(`/api/notifications/${notificationId}/read`);
  },

  // 标记所有通知为已读
  markAllAsRead() {
    return instance.post('/api/notifications/read-all');
  },

  // 删除所有已读通知
  deleteReadNotifications() {
    return instance.delete('/api/notifications/read');
  },

  // 创建测试通知（仅用于开发测试）
  createTestNotification() {
    return instance.post('/api/notifications/test');
  }
};