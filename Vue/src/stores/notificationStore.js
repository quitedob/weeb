import { defineStore } from 'pinia';
import notificationApi from '@/api/modules/notification';

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    unreadCount: 0,
    currentPage: 1,
    totalPages: 1,
    pageSize: 10,
    isLoading: false,
    lastFetchTime: null
  }),

  getters: {
    unreadNotifications: (state) => {
      return state.notifications.filter(notification => !notification.isRead);
    },
    
    readNotifications: (state) => {
      return state.notifications.filter(notification => notification.isRead);
    }
  },

  actions: {
    async fetchNotifications(page = 1, pageSize = 10) {
      this.isLoading = true;
      try {
        const response = await notificationApi.getNotifications(page, pageSize);

        if (response.code === 0 && response.data) {
          const data = response.data;

          if (page === 1) {
            // 第一页，替换所有通知
            this.notifications = data.notifications || [];
          } else {
            // 后续页，追加通知
            this.notifications.push(...(data.notifications || []));
          }

          this.currentPage = page;
          this.totalPages = Math.ceil((data.total || 0) / pageSize);
          this.pageSize = pageSize;
          this.lastFetchTime = new Date();
        }

        // 同时获取未读数量
        await this.fetchUnreadCount();

      } catch (error) {
        console.error('获取通知列表失败:', error);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    async fetchUnreadCount() {
      try {
        const response = await notificationApi.getUnreadCount();
        if (response.code === 0 && response.data) {
          this.unreadCount = response.data.unreadCount || 0;
        }
      } catch (error) {
        console.error('获取未读通知数量失败:', error);
      }
    },

    async markAsRead(notificationId) {
      try {
        const response = await notificationApi.markAsRead(notificationId);
        if (response.code === 0) {
          // 更新本地状态
          const notification = this.notifications.find(n => n.id === notificationId);
          if (notification && !notification.isRead) {
            notification.isRead = true;
            this.unreadCount = Math.max(0, this.unreadCount - 1);
          }
        }
        return response;
      } catch (error) {
        console.error('标记通知为已读失败:', error);
        throw error;
      }
    },

    async markAllAsRead() {
      try {
        const response = await notificationApi.markAllAsRead();
        if (response.code === 0) {
          // 更新本地状态
          this.notifications.forEach(notification => {
            notification.isRead = true;
          });
          this.unreadCount = 0;
        }
        return response;
      } catch (error) {
        console.error('标记所有通知为已读失败:', error);
        throw error;
      }
    },

    async deleteReadNotifications() {
      try {
        const response = await notificationApi.deleteReadNotifications();
        if (response.code === 0) {
          // 从本地状态中移除已读通知
          this.notifications = this.notifications.filter(notification => !notification.isRead);

          // 重新计算分页信息
          const remainingCount = this.notifications.length;
          this.totalPages = Math.ceil(remainingCount / this.pageSize);
          this.currentPage = Math.min(this.currentPage, this.totalPages || 1);
        }
        return response;
      } catch (error) {
        console.error('删除已读通知失败:', error);
        throw error;
      }
    },

    // 添加新通知到列表顶部（用于实时通知）
    addNotification(notification) {
      this.notifications.unshift(notification);
      if (!notification.isRead) {
        this.unreadCount++;
      }
    },

    // 清空所有通知
    clearNotifications() {
      this.notifications = [];
      this.unreadCount = 0;
      this.currentPage = 1;
      this.totalPages = 1;
    },

    // 重置状态
    resetState() {
      this.notifications = [];
      this.unreadCount = 0;
      this.currentPage = 1;
      this.totalPages = 1;
      this.pageSize = 10;
      this.isLoading = false;
      this.lastFetchTime = null;
    }
  }
});