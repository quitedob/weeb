import { defineStore } from 'pinia';
import notificationApi from '@/api/modules/notification';

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [], // 通知列表
    unreadCount: 0, // 未读通知数量
    isLoading: false, // 加载状态
    currentPage: 1, // 当前页码
    totalPages: 1, // 总页数
  }),

  getters: {
    // 获取未读通知
    unreadNotifications: (state) => {
      return state.notifications.filter(notification => !notification.isRead);
    },
    
    // 获取已读通知
    readNotifications: (state) => {
      return state.notifications.filter(notification => notification.isRead);
    }
  },

  actions: {
    /**
     * 从API获取通知列表
     * @param {number} page - 页码
     */
    async fetchNotifications(page = 1) {
      this.isLoading = true;
      try {
        const response = await notificationApi.getNotifications(page);
        if (response.data && response.data.code === 0) {
          const data = response.data.data;
          if (page === 1) {
            // 第一页，替换整个列表
            this.notifications = data.notifications || [];
          } else {
            // 其他页，追加到列表
            this.notifications = [...this.notifications, ...(data.notifications || [])];
          }
          this.unreadCount = data.unreadCount || 0;
          this.currentPage = page;
          this.totalPages = data.totalPages || 1;
        }
      } catch (error) {
        console.error('获取通知列表失败:', error);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    /**
     * 从API获取未读数量
     */
    async fetchUnreadCount() {
      try {
        const response = await notificationApi.getUnreadCount();
        if (response.data && response.data.code === 0) {
          this.unreadCount = response.data.data.unreadCount || 0;
        }
      } catch (error) {
        console.error('获取未读通知数量失败:', error);
        throw error;
      }
    },

    /**
     * 标记所有通知为已读
     */
    async markAllAsRead() {
      try {
        const response = await notificationApi.markAllAsRead();
        if (response.data && response.data.code === 0) {
          // 更新本地状态
          this.notifications.forEach(notification => {
            notification.isRead = true;
          });
          this.unreadCount = 0;
        }
      } catch (error) {
        console.error('标记所有通知为已读失败:', error);
        throw error;
      }
    },

    /**
     * 标记指定通知为已读
     * @param {number} notificationId - 通知ID
     */
    async markAsRead(notificationId) {
      try {
        const response = await notificationApi.markAsRead(notificationId);
        if (response.data && response.data.code === 0) {
          // 更新本地状态
          const notification = this.notifications.find(n => n.id === notificationId);
          if (notification) {
            notification.isRead = true;
            this.unreadCount = Math.max(0, this.unreadCount - 1);
          }
        }
      } catch (error) {
        console.error('标记通知为已读失败:', error);
        throw error;
      }
    },

    /**
     * 删除已读通知
     */
    async deleteReadNotifications() {
      try {
        const response = await notificationApi.deleteReadNotifications();
        if (response.data && response.data.code === 0) {
          // 从本地状态中移除已读通知
          this.notifications = this.notifications.filter(notification => !notification.isRead);
        }
      } catch (error) {
        console.error('删除已读通知失败:', error);
        throw error;
      }
    },

    /**
     * 由WebSocket监听器调用，实时添加新通知
     * @param {Object} newNotification - 新通知对象
     */
    addNotification(newNotification) {
      // 在列表顶部添加新通知
      this.notifications.unshift(newNotification);
      // 增加未读数量
      if (!newNotification.isRead) {
        this.unreadCount++;
      }
    },

    /**
     * 重置store状态
     */
    reset() {
      this.notifications = [];
      this.unreadCount = 0;
      this.isLoading = false;
      this.currentPage = 1;
      this.totalPages = 1;
    }
  }
}); 