import { defineStore } from 'pinia';
import { watch } from 'vue';
import notificationApi from '@/api/modules/notification';
import { captureSession, isCurrentSession } from '@/utils/session';

export const useNotificationStore = defineStore('notification', {
  persist: {
    key: 'notification-store',
    paths: ['unreadCount', 'lastFetchTime'],
    storage: sessionStorage,
  },
  
  state: () => ({
    notifications: [],
    unreadCount: 0,
    currentPage: 1,
    totalPages: 1,
    pageSize: 10,
    isLoading: false,
    lastFetchTime: null,
    autoRefreshInterval: null,
    firstPage: 1,
    totalCount: 0,
    listGeneration: 0,
    unreadGeneration: 0,
    unreadRequest: null,
    realtimeIds: [],
    realtimeRevision: 0,
    realtimeConnected: false,
    autoRefreshEnabled: false,
    refreshFailures: 0,
    maxNotifications: 100, // 最多缓存100条通知
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
    async fetchNotifications(page = 1, pageSize = this.pageSize, direction = page === 1 ? 'replace' : 'older', reconcileEvents = true) {
      const session = captureSession();
      const initialLiveIds = new Set(this.realtimeIds);
      const initialRevision = this.realtimeRevision;
      const generation = ++this.listGeneration;
      const current = () => isCurrentSession(session) && generation === this.listGeneration;
      this.isLoading = true;
      try {
        const response = await notificationApi.getNotifications(page, pageSize);
        if (!current()) return null;
        if (response.code !== 0 || !response.data) throw new Error('Invalid notification response');
        const data = response.data;
        // Merge arrivals received after this request began; accepting the completed page
        // avoids postponing history indefinitely during an active notification stream.
        const existing = new Map(this.notifications.map(row => [String(row.id), row]));
        const rows = (data.notifications || []).map(row => existing.get(String(row.id))?.isRead
          ? { ...row, isRead: true } : { ...row });
        const rowIds = new Set(rows.map(row => String(row.id)));
        const arrived = this.notifications.filter(row => this.realtimeIds.includes(String(row.id))
          && !initialLiveIds.has(String(row.id)) && !rowIds.has(String(row.id)));
        if (reconcileEvents && direction === 'replace' && page === 1 && this.firstPage > 1 && initialRevision !== this.realtimeRevision) {
          // Older windows deliberately do not retain new payloads. One fresh read recovers
          // events that arrived while explicitly switching back to the head of the list.
          return this.fetchNotifications(page, pageSize, direction, false);
        }
        this.pageSize = data.pageSize ?? pageSize;
        this.totalCount = (data.totalCount ?? data.total ?? 0) + (direction === 'replace' && page === 1 ? arrived.length : 0);
        this.totalPages = Math.max(data.totalPages ?? 0, Math.ceil(this.totalCount / this.pageSize));
        const returnedPage = data.currentPage ?? page;
        if (returnedPage > Math.max(1, this.totalPages)) return this.fetchNotifications(Math.max(1, this.totalPages), pageSize, 'replace');
        const pagesInWindow = Math.max(1, Math.floor(this.maxNotifications / this.pageSize));
        if (direction === 'replace') {
          this.notifications = page === 1 ? [...arrived, ...rows] : rows;
          this.firstPage = returnedPage;
          this.currentPage = returnedPage + Math.max(0, Math.floor(Math.min(this.notifications.length, this.maxNotifications) / this.pageSize) - 1);
        } else if (direction === 'newer') {
          this.notifications = this.uniqueNotifications([...rows, ...this.notifications]);
          this.firstPage = returnedPage;
          this.currentPage = Math.min(this.currentPage, returnedPage + pagesInWindow - 1);
        } else {
          this.notifications = this.uniqueNotifications([...this.notifications, ...rows]);
          this.currentPage = returnedPage;
          this.firstPage = Math.max(this.firstPage, returnedPage - pagesInWindow + 1);
        }
        this.pruneNotifications(direction);
        this.lastFetchTime = new Date();
        await this.fetchUnreadCount();
      } catch (error) {
        if (!current()) return null;
        throw error;
      } finally { if (current()) this.isLoading = false; }
    },

    uniqueNotifications(rows) {
      const seen = new Set();
      return rows.filter(row => { const key = String(row.id); if (seen.has(key)) return false; seen.add(key); return true; });
    },

    loadNewerNotifications() {
      if (this.firstPage <= 1) return;
      return this.fetchNotifications(this.firstPage - 1, this.pageSize, 'newer');
    },

    loadLatestNotifications() { return this.fetchNotifications(1, this.pageSize, 'replace'); },

    async fetchUnreadCount() {
      if (this.unreadRequest) return this.unreadRequest;
      const session = captureSession();
      const generation = this.unreadGeneration;
      const request = (async () => {
        try {
          const response = await notificationApi.getUnreadCount();
          if (!isCurrentSession(session) || generation !== this.unreadGeneration) return null;
          if (response.code !== 0 || !response.data) return false;
          this.unreadCount = response.data.unreadCount || 0;
          return true;
        } catch (error) {
          if (!isCurrentSession(session) || generation !== this.unreadGeneration) return null;
          return false;
        }
      })();
      this.unreadRequest = request;
      try { return await request; }
      finally {
        if (isCurrentSession(session) && this.unreadRequest === request) {
          this.unreadRequest = null;
          if (generation !== this.unreadGeneration) this.requestReconciliation();
        }
      }
    },

    invalidateNotificationReads() {
      ++this.listGeneration;
      ++this.unreadGeneration;
      this.isLoading = false;
    },

    async markAsRead(notificationId) {
      const session = captureSession();
      this.invalidateNotificationReads();
      const response = await notificationApi.markAsRead(notificationId).catch(error => {
        if (isCurrentSession(session)) throw error;
      });
      if (!isCurrentSession(session)) return null;
      if (response?.code === 0) {
        this.invalidateNotificationReads();
        const notification = this.notifications.find(item => String(item.id) === String(notificationId));
        const wasUnread = notification && !notification.isRead;
        if (notification) notification.isRead = true;
        if (wasUnread) this.unreadCount = Math.max(0, this.unreadCount - 1);
        ++this.unreadGeneration;
        this.requestReconciliation();
      }
      return response;
    },

    async markAllAsRead() {
      const session = captureSession();
      const ids = new Set(this.notifications.map(item => String(item.id)));
      const previousUnread = this.unreadCount;
      this.invalidateNotificationReads();
      const response = await notificationApi.markAllAsRead().catch(error => { if (isCurrentSession(session)) throw error; });
      if (!isCurrentSession(session)) return null;
      if (response?.code === 0) {
        this.invalidateNotificationReads();
        this.notifications.forEach(item => { if (ids.has(String(item.id))) item.isRead = true; });
        this.unreadCount = Math.max(0, this.unreadCount - previousUnread);
        ++this.unreadGeneration;
        this.requestReconciliation();
      }
      return response;
    },

    async deleteReadNotifications() {
      const session = captureSession();
      this.invalidateNotificationReads();
      const response = await notificationApi.deleteReadNotifications().catch(error => { if (isCurrentSession(session)) throw error; });
      if (!isCurrentSession(session)) return null;
      if (response?.code === 0) {
        this.invalidateNotificationReads();
        this.notifications = this.notifications.filter(item => !item.isRead);
        await this.fetchNotifications(1, this.pageSize);
      }
      return response;
    },

    addNotification(notification) {
      ++this.realtimeRevision;
      const key = String(notification.id);
      const existing = this.notifications.find(item => String(item.id) === key);
      const duplicate = !!existing || this.realtimeIds.includes(key);
      if (existing) {
        if (!existing.isRead && notification.isRead) this.unreadCount = Math.max(0, this.unreadCount - 1);
        Object.assign(existing, notification, { isRead: existing.isRead || notification.isRead });
      }
      else if (this.firstPage === 1) this.notifications.unshift(notification);
      if (!duplicate) {
        this.realtimeIds.push(key);
        this.realtimeIds = this.realtimeIds.slice(-this.maxNotifications);
        if (!notification.isRead) ++this.unreadCount;
        ++this.totalCount;
        this.totalPages = Math.ceil(this.totalCount / this.pageSize);
      }
      ++this.unreadGeneration;
      this.pruneNotifications('newer');
      this.requestReconciliation();
    },

    clearNotifications() {
      this.invalidateNotificationReads();
      this.notifications = [];
      this.unreadCount = 0;
      this.currentPage = this.firstPage = 1;
      this.totalPages = 1;
      this.totalCount = 0;
      this.realtimeIds = [];
      this.realtimeRevision = 0;
    },

    resetState() {
      this.stopAutoRefresh();
      this.clearNotifications();
      this.pageSize = 10;
      this.lastFetchTime = null;
      this.unreadRequest = null;
      this.realtimeConnected = false;
      this.refreshFailures = 0;
    },

    canReconcile() {
      return this.autoRefreshEnabled && !document.hidden && navigator.onLine !== false;
    },

    setRealtimeConnected(connected) {
      if (this.realtimeConnected === connected) return;
      this.realtimeConnected = connected;
      this.refreshFailures = 0;
      this.requestReconciliation(0);
    },

    refreshVisibility() {
      if (this.autoRefreshInterval) clearTimeout(this.autoRefreshInterval);
      this.autoRefreshInterval = null;
      if (this.canReconcile()) this.requestReconciliation(0);
    },

    startAutoRefresh() {
      this.stopAutoRefresh();
      this.autoRefreshEnabled = true;
      this.requestReconciliation(0);
    },

    stopAutoRefresh() {
      this.autoRefreshEnabled = false;
      if (this.autoRefreshInterval) clearTimeout(this.autoRefreshInterval);
      this.autoRefreshInterval = null;
    },

    requestReconciliation(delay = 250) {
      if (!this.canReconcile()) return;
      if (this.autoRefreshInterval) clearTimeout(this.autoRefreshInterval);
      const session = captureSession();
      this.autoRefreshInterval = setTimeout(async () => {
        this.autoRefreshInterval = null;
        if (!isCurrentSession(session) || !this.canReconcile()) return;
        const success = await this.fetchUnreadCount();
        if (!isCurrentSession(session) || !this.canReconcile()) return;
        this.refreshFailures = success === false ? this.refreshFailures + 1 : 0;
        // Realtime bursts can schedule an earlier reconciliation while this read is pending.
        if (this.autoRefreshInterval) return;
        const interval = this.realtimeConnected ? 300000 : Math.min(30000 * 2 ** this.refreshFailures, 300000);
        this.requestReconciliation(interval);
      }, delay);
    },

    pruneNotifications(direction = 'newer') {
      if (this.notifications.length <= this.maxNotifications) return;
      this.notifications = direction === 'older' ? this.notifications.slice(-this.maxNotifications)
        : this.notifications.slice(0, this.maxNotifications);
    },

    setupWatchers() {
      // 监听未读数量变化
      watch(
        () => this.unreadCount,
        (newCount, oldCount) => {
          if (newCount > oldCount) {
            console.log(`新增 ${newCount - oldCount} 条未读通知`);
            
            // 可以触发浏览器通知
            if ('Notification' in window && Notification.permission === 'granted') {
              new Notification('新通知', {
                body: `您有 ${newCount} 条未读通知`,
                icon: '/favicon.ico'
              });
            }
          }
        }
      );

      // 监听通知列表变化，自动清理
      watch(
        () => this.notifications.length,
        () => {
          this.pruneNotifications();
        }
      );
    }
  }
});
