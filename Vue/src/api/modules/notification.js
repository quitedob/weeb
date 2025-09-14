// 通知相关的API模块
import { instance as axiosInstance } from '../axiosInstance';

const notificationApi = {
  /**
   * 获取用户通知列表
   * @param {number} page - 页码
   * @param {number} size - 每页大小
   * @returns {Promise} API响应
   */
  getNotifications(page = 1, size = 10) {
    return axiosInstance.get('/api/notifications', {
      params: { page, size }
    });
  },

  /**
   * 获取未读通知数量
   * @returns {Promise} API响应
   */
  getUnreadCount() {
    return axiosInstance.get('/api/notifications/unread-count');
  },

  /**
   * 标记所有通知为已读
   * @returns {Promise} API响应
   */
  markAllAsRead() {
    return axiosInstance.post('/api/notifications/read-all');
  },

  /**
   * 标记指定通知为已读
   * @param {number} notificationId - 通知ID
   * @returns {Promise} API响应
   */
  markAsRead(notificationId) {
    return axiosInstance.post(`/api/notifications/${notificationId}/read`);
  },

  /**
   * 删除已读通知
   * @returns {Promise} API响应
   */
  deleteReadNotifications() {
    return axiosInstance.delete('/api/notifications/read');
  },

  /**
   * 创建测试通知（仅用于开发测试）
   * @returns {Promise} API响应
   */
  createTestNotification() {
    return axiosInstance.post('/api/notifications/test');
  }
};

export default notificationApi; 