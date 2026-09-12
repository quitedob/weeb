// File path: /Vue/src/stores/authStore.js
import { defineStore } from 'pinia';
import api from '@/api';
import { normalizeTokenExpiry, tokenExpiresAt, withRenewalLock } from '@/utils/session';

export const useAuthStore = defineStore('auth', {
  persist: {
    key: 'auth-store',
    paths: ['accessToken', 'refreshToken', 'currentUser', 'tokenExpiry'],
    storage: localStorage,
  },
  state: () => ({
    accessToken: localStorage.getItem('jwt_token') || null,
    refreshToken: localStorage.getItem('refresh_token') || null,
    tokenExpiry: localStorage.getItem('token_expiry') || null,
    currentUser: JSON.parse(localStorage.getItem('currentUser')) || null,
    isRefreshing: false,
    refreshPromise: null,
    sessionEpoch: 0,
    _fetchingUserInfo: null,
    refreshTimer: null,
    preferences: null,
    preferencesRevision: 0,
    preferenceRequests: { load: 0, privacy: 0, notifications: 0 },
  }),
  getters: {
    isLoggedIn: (state) => !!state.accessToken,
    isAuthenticated: (state) => !!state.accessToken,
    user: (state) => state.currentUser,
    isTokenExpired: (state) => {
      if (!state.tokenExpiry) return true;
      return Date.now() >= parseInt(state.tokenExpiry);
    },
    needsRefresh: (state) => {
      if (!state.tokenExpiry) return false;
      const expiryTime = parseInt(state.tokenExpiry);
      const now = Date.now();
      const fiveMinutes = 5 * 60 * 1000;
      return (expiryTime - now) < fiveMinutes && (expiryTime - now) > 0;
    },
  },
  actions: {
    async loadPreferences() {
      const token = this.accessToken;
      if (!token) return null;
      const revision = this.preferencesRevision;
      const request = ++this.preferenceRequests.load;
      const isCurrent = () => this.accessToken === token && this.preferencesRevision === revision
        && this.preferenceRequests.load === request;
      try {
        const response = await api.user.getSettings();
        if (!isCurrent()) return null;
        if (response.code !== 0 || !response.data) throw new Error(response.message || '无法读取设置');
        this.preferences = response.data;
        return this.preferences;
      } catch (error) {
        if (!isCurrent()) return null;
        throw error;
      }
    },
    savePrivacyPreferences(privacy) {
      return this.savePreferencesSection('privacy', privacy);
    },
    saveNotificationPreferences(notifications) {
      return this.savePreferencesSection('notifications', notifications);
    },
    async savePreferencesSection(section, settings) {
      const token = this.accessToken;
      if (!token) throw new Error('请先登录');
      const save = section === 'privacy' ? api.user.savePrivacySettings
        : section === 'notifications' ? api.user.saveNotificationSettings : null;
      if (!save) throw new Error('未知设置类型');
      const request = ++this.preferenceRequests[section];
      ++this.preferencesRevision; // Reads started before this save must not restore older settings.
      const isCurrent = () => this.accessToken === token && this.preferenceRequests[section] === request;
      try {
        const response = await save({ ...settings });
        if (!isCurrent()) return null;
        if (response.code !== 0 || !response.data) throw new Error(response.message || '保存失败');
        ++this.preferencesRevision; // Also invalidate reads that started while the save was pending.
        this.preferences = { ...this.preferences, [section]: response.data };
        return response.data;
      } catch (error) {
        if (!isCurrent()) return null;
        throw error;
      }
    },
    applyToken(data) {
      this.tokenExpiry = normalizeTokenExpiry(data);
      this.refreshToken = data.token;
      localStorage.setItem('jwt_token', data.token);
      localStorage.setItem('refresh_token', data.token);
      if (this.tokenExpiry) localStorage.setItem('token_expiry', this.tokenExpiry);
      else localStorage.removeItem('token_expiry');
      this.accessToken = data.token;
    },
    async login(credentials) {
      this.logoutCleanup();
      const epoch = this.sessionEpoch;
      try {
        const response = await api.auth.login(credentials);
        if (epoch !== this.sessionEpoch) return false;
        if (response.code !== 0 || !response.data?.token) throw new Error(response.message || 'Login failed');
        this.setCurrentUser(response.data.user);
        this.applyToken(response.data);
        return true;
      } catch (error) {
        if (epoch !== this.sessionEpoch) return false;
        throw error;
      }
    },
    async fetchUserInfo() {
      if (!this.accessToken) return null;
      if (this._fetchingUserInfo) return this._fetchingUserInfo;
      const epoch = this.sessionEpoch;
      const request = (async () => {
        try {
          const response = await api.auth.getUserInfo();
          if (epoch !== this.sessionEpoch) return null;
          if (response.code === 0 && response.data) {
            this.setCurrentUser(response.data);
            return this.currentUser;
          }
          return null;
        } catch (error) {
          if (epoch !== this.sessionEpoch) return null;
          throw error;
        } finally {
          if (this._fetchingUserInfo === request) this._fetchingUserInfo = null;
        }
      })();
      this._fetchingUserInfo = request;
      return request;
    },
    async refreshAccessToken() {
      if (this.isRefreshing) return this.refreshPromise;
      if (!this.accessToken) throw new Error('No access token available');
      const epoch = this.sessionEpoch;
      const token = this.accessToken;
      const isCurrent = () => epoch === this.sessionEpoch && token === this.accessToken;
      this.isRefreshing = true;
      const request = (async () => {
        try {
          return await withRenewalLock(async () => {
            if (!isCurrent()) return null;
            // The previous lock holder writes the replacement before releasing it.
            // A queued tab adopts that credential without consuming it a second time.
            if (localStorage.getItem('jwt_token') !== token) {
              this.syncAuthStatus();
              return this.accessToken;
            }
            const response = await api.auth.refreshToken(token);
            if (!isCurrent()) return null;
            if (response.code !== 0 || !response.data?.token) throw new Error(response.message || 'Token refresh failed');
            this.applyToken(response.data);
            return response.data.token;
          });
        } catch (error) {
          if (!isCurrent()) return null;
          // The HTTP interceptor clears only a current, invalid credential.
          // Temporary service failures leave a still-valid token available for retry.
          throw error;
        } finally {
          if (this.refreshPromise === request) {
            this.isRefreshing = false;
            this.refreshPromise = null;
          }
        }
      })();
      this.refreshPromise = request;
      return request;
    },

    logoutCleanup(clearStorage = true) {
      ++this.sessionEpoch;
      this._fetchingUserInfo = null;
      ++this.preferencesRevision;
      for (const section of Object.keys(this.preferenceRequests)) ++this.preferenceRequests[section];
      this.accessToken = null;
      this.refreshToken = null;
      this.tokenExpiry = null;
      this.currentUser = null;
      this.preferences = null;
      this.isRefreshing = false;
      this.refreshPromise = null;
      
      if (clearStorage) {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('token_expiry');
        localStorage.removeItem('currentUser');
      }
      
      console.log('AuthStore: State and localStorage cleared for logout.');
    },
    async logout() {
      const epoch = this.sessionEpoch;
      try {
        await api.auth.logout();
      } catch (error) {
        if (epoch === this.sessionEpoch) console.warn('API logout call failed:', error.message);
      } finally {
        if (epoch !== this.sessionEpoch) return;
        this.logoutCleanup();
        const clearedEpoch = this.sessionEpoch;
        const [{ useChatStore }, { useNotificationStore }] = await Promise.all([
          import('./chatStore'), import('./notificationStore')
        ]);
        if (clearedEpoch !== this.sessionEpoch || this.accessToken) return;
        const chatStore = useChatStore();
        chatStore.disconnectWebSocket();
        chatStore.$reset();
        useNotificationStore().resetState();
      }
    },
    syncAuthStatus() {
      const token = localStorage.getItem('jwt_token');
      const storedExpiry = localStorage.getItem('token_expiry');
      let storedUser = null;
      try { storedUser = JSON.parse(localStorage.getItem('currentUser')); } catch { /* Ignore invalid cache. */ }
      if (!token) {
        this.logoutCleanup();
        return;
      }
      if (token !== this.accessToken) {
        // Cross-tab account changes must pass through logout so shared stores reset.
        // Do not echo a logout into shared storage while adopting another tab's session.
        this.logoutCleanup(false);
        this.currentUser = storedUser?.user || storedUser;
      } else if (this.currentUser?.user) {
        this.currentUser = this.currentUser.user;
      }
      const expiresAt = tokenExpiresAt(token) ?? Number(storedExpiry);
      this.tokenExpiry = normalizeTokenExpiry({ token, expiresAt });
      this.refreshToken = token;
      this.accessToken = token;
      if (this.isTokenExpired) {
        // The backend renews only a still-valid Bearer token.
        this.logoutCleanup();
      } else if (!this.currentUser) {
        const epoch = this.sessionEpoch;
        this.fetchUserInfo().catch(() => {});
      }
    },

    startTokenRefreshTimer() {
      // 每分钟检查一次token是否需要刷新
      if (this.refreshTimer) return;
      this.refreshTimer = setInterval(() => {
        if (this.needsRefresh && !this.isRefreshing) {
          console.log('Token即将过期，自动刷新');
          this.refreshAccessToken().catch(error => {
            console.error('自动刷新token失败:', error);
          });
        }
      }, 60000); // 60秒
    },
    // Utility to update current user info if changed elsewhere (e.g. settings page)
    setCurrentUser(userData) {
        this.currentUser = userData?.user || userData || null;
        if (this.currentUser) localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
        else localStorage.removeItem('currentUser');
    },
    
    // 添加缺失的 setToken 方法
    setToken(token) {
        if (token !== this.accessToken) this.logoutCleanup();
        if (token) this.applyToken({ token });
    }
  },
});
