// File path: /Vue/src/stores/authStore.js
import { defineStore } from 'pinia';
import api from '@/api';

export const useAuthStore = defineStore('auth', {
  persist: {
    key: 'auth-store',
    paths: ['accessToken', 'currentUser'],
    storage: localStorage,
  },
  state: () => ({
    accessToken: localStorage.getItem('jwt_token') || null, // Consistent with user's interceptor example
    currentUser: JSON.parse(localStorage.getItem('currentUser')) || null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.accessToken,
    isAuthenticated: (state) => !!state.accessToken, // Alias for consistency
    user: (state) => state.currentUser, // Alias for currentUser for convenience
  },
  actions: {
    async login(credentials) {
      try {
        const response = await api.auth.login(credentials);
        // 后端统一ApiResponse格式: { code: 0, data: { token: '...' } }
        if (response.code === 0 && response.data && response.data.token) {
          this.accessToken = response.data.token;
          localStorage.setItem('jwt_token', this.accessToken);

          // 获取用户信息
          await this.fetchUserInfo();
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
      try {
        const response = await api.user.getUserInfo(); // 注意: 你的api/index.js中user模块才有getUserInfo
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
      }
    },
    // Centralized method to clear local state and storage, called internally or by interceptor.
    logoutCleanup() {
      this.accessToken = null;
      this.currentUser = null;
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('currentUser');
      console.log('AuthStore: State and localStorage cleared for logout.');
    },
    // **核心修改点**：这是供外部调用的登出动作
    async logout() {
      // 1. 尽力通知后端，但不等待结果或处理它的失败
      api.auth.logout().catch(error => {
        // 后端登出失败是预料之中的（因为token已失效），静默处理即可
        console.warn("API logout call failed (this is expected if token was invalid):", error.message);
      });

      // 2. 无论后端调用是否成功，立即无条件清理前端状态
      this.logoutCleanup();
    },
    syncAuthStatus() {
      const token = localStorage.getItem('jwt_token');
      if (token) {
        this.accessToken = token;
        if (!this.currentUser) {
          this.fetchUserInfo().catch(() => {
            // 如果token无效，fetchUserInfo会失败，axios拦截器或路由守卫会处理登出
          });
        }
      } else {
        this.logoutCleanup();
      }
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
