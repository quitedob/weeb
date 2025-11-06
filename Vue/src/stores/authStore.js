// File path: /Vue/src/stores/authStore.js
import { defineStore } from 'pinia';
import api from '@/api';

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
    async login(credentials) {
      try {
        const response = await api.auth.login(credentials);
        if (response.code === 0 && response.data) {
          const { token, user, expiresIn } = response.data;
          
          this.accessToken = token;
          this.refreshToken = token; // 使用同一个token作为refreshToken
          
          // 计算token过期时间（当前时间 + expiresIn秒）
          if (expiresIn) {
            this.tokenExpiry = (Date.now() + expiresIn * 1000).toString();
            localStorage.setItem('token_expiry', this.tokenExpiry);
          }
          
          localStorage.setItem('jwt_token', this.accessToken);
          localStorage.setItem('refresh_token', this.accessToken);
          
          // 直接使用登录返回的用户信息
          if (user) {
            this.currentUser = user;
            localStorage.setItem('currentUser', JSON.stringify(user));
          }

          return true;
        } else {
          throw new Error(response.message || '登录失败');
        }
      } catch (error) {
        this.logoutCleanup();
        throw error;
      }
    },
    async fetchUserInfo() {
      if (!this.accessToken) return null;
      
      // 防止重复请求：如果正在请求中，返回同一个Promise
      if (this._fetchingUserInfo) {
        return this._fetchingUserInfo;
      }
      
      try {
        this._fetchingUserInfo = api.auth.getUserInfo();
        const response = await this._fetchingUserInfo;
        
        if (response.code === 0 && response.data) { // 后端返回的是 ApiResponse 格式
          this.currentUser = response.data;
          localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
          return this.currentUser;
        }
        return null;
      } catch (error) {
        console.error('fetchUserInfo failed:', error);
        // 抛出错误，让调用者（如路由守卫）知道验证失败
        throw error;
      } finally {
        this._fetchingUserInfo = null;
      }
    },
    async refreshAccessToken() {
      if (this.isRefreshing) {
        return this.refreshPromise;
      }

      if (!this.accessToken) {
        throw new Error('No access token available');
      }

      this.isRefreshing = true;
      this.refreshPromise = (async () => {
        try {
          const response = await api.auth.refreshToken(this.accessToken);
          
          if (response.code === 0 && response.data) {
            const { token, expiresIn } = response.data;
            
            this.accessToken = token;
            this.refreshToken = token;
            
            if (expiresIn) {
              this.tokenExpiry = (Date.now() + expiresIn * 1000).toString();
              localStorage.setItem('token_expiry', this.tokenExpiry);
            }
            
            localStorage.setItem('jwt_token', this.accessToken);
            localStorage.setItem('refresh_token', this.accessToken);
            
            return token;
          } else {
            throw new Error(response.message || 'Token refresh failed');
          }
        } catch (error) {
          console.error('Token refresh failed:', error);
          this.logoutCleanup();
          throw error;
        } finally {
          this.isRefreshing = false;
          this.refreshPromise = null;
        }
      })();

      return this.refreshPromise;
    },

    logoutCleanup() {
      this.accessToken = null;
      this.refreshToken = null;
      this.tokenExpiry = null;
      this.currentUser = null;
      this.isRefreshing = false;
      this.refreshPromise = null;
      
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('token_expiry');
      localStorage.removeItem('currentUser');
      
      console.log('AuthStore: State and localStorage cleared for logout.');
    },
    async logout() {
      try {
        await api.auth.logout();
      } catch (error) {
        console.warn("API logout call failed:", error.message);
      } finally {
        this.logoutCleanup();
        
        // 清理其他Store
        const { useChatStore } = await import('./chatStore');
        const { useNotificationStore } = await import('./notificationStore');
        
        const chatStore = useChatStore();
        const notificationStore = useNotificationStore();
        
        chatStore.$reset();
        notificationStore.resetState();
      }
    },
    syncAuthStatus() {
      const token = localStorage.getItem('jwt_token');
      const refreshToken = localStorage.getItem('refresh_token');
      const tokenExpiry = localStorage.getItem('token_expiry');
      
      if (token) {
        this.accessToken = token;
        this.refreshToken = refreshToken;
        this.tokenExpiry = tokenExpiry;
        
        // 检查token是否过期
        if (this.isTokenExpired) {
          console.log('Token已过期，尝试刷新');
          if (this.refreshToken) {
            this.refreshAccessToken().catch(() => {
              this.logoutCleanup();
            });
          } else {
            this.logoutCleanup();
          }
        } else if (!this.currentUser) {
          this.fetchUserInfo().catch(() => {
            this.logoutCleanup();
          });
        }
      } else {
        this.logoutCleanup();
      }
    },
    
    startTokenRefreshTimer() {
      // 每分钟检查一次token是否需要刷新
      setInterval(() => {
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
        this.currentUser = userData;
        localStorage.setItem('currentUser', JSON.stringify(userData));
    },
    
    // 添加缺失的 setToken 方法
    setToken(token) {
        this.accessToken = token;
        localStorage.setItem('jwt_token', token);
    }
  },
});
