// File path: /Vue/src/stores/authStore.js
import { defineStore } from 'pinia';
import api from '@/api';
import router from '@/router'; // For router.push if needed, though usually handled by interceptor/component

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('jwt_token') || null, // Consistent with user's interceptor example
    currentUser: JSON.parse(localStorage.getItem('currentUser')) || null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.accessToken,
    user: (state) => state.currentUser, // Alias for currentUser for convenience
  },
  actions: {
    async login(credentials) {
      try {
        const response = await api.auth.login(credentials);
        // Assuming backend login response structure is { code: 200, data: { token: '...', user: {...} } }
        // Or just { code: 200, data: { token: '...' } } and then fetchUserInfo
        if (response.code === 200 && response.data && response.data.token) {
          this.accessToken = response.data.token;
          localStorage.setItem('jwt_token', this.accessToken);

          // If login response includes user data, set it directly
          if (response.data.user) {
            this.currentUser = response.data.user;
            localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
          } else {
            // Otherwise, fetch user info separately
            await this.fetchUserInfo();
          }
          return true; // Indicate login success to component
        } else {
          throw new Error(response.message || 'Login failed');
        }
      } catch (error) {
        this.logoutCleanup(); // Centralize cleanup
        throw error; // Re-throw for component to handle UI
      }
    },
    async fetchUserInfo() {
      if (!this.accessToken) return null;
      try {
        const response = await api.user.getUserInfo();
        if (response.code === 200 && response.data) {
          this.currentUser = response.data;
          localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
          return this.currentUser;
        }
        // If fetch fails, interceptor should ideally catch 401 and trigger logout.
        // No need to call this.logout() here as interceptor in axiosInstance.js handles it.
        return null;
      } catch (error) {
        console.error('fetchUserInfo failed:', error);
        // If a 401 error occurs, the response interceptor in api/axiosInstance.js should call logout.
        return null;
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
    // Action to be called by components or other parts of the app to initiate logout.
    async logout() {
      try {
        await api.auth.logout(); // Call backend logout
      } catch (error) {
        console.error("API logout call failed:", error);
        // Still proceed with frontend cleanup even if backend call fails
      }
      this.logoutCleanup();
      // Navigation is typically handled by the component initiating logout or by global navigation guards
      // based on auth state, rather than directly in the store's logout action after backend call.
      // The interceptor in axiosInstance.js handles redirection for 401s.
      // For manual logout, the component calling this action should handle redirection.
      // router.push('/login').catch(err => console.error('Router push to login failed:', err));
    },
    // Called by app initialization (e.g., in App.vue or main.js) to sync store with localStorage
    syncAuthStatus() {
      const token = localStorage.getItem('jwt_token');
      if (token) {
        this.accessToken = token;
        if (!this.currentUser) { // If user data isn't in store, try to fetch it
          this.fetchUserInfo().catch(() => {
            // If fetchUserInfo fails (e.g. token invalid), interceptor in axiosInstance.js
            // should catch the 401 and trigger logout (which includes cleanup).
          });
        }
      } else {
        // Ensure state is clean if no token is found.
        // This might be redundant if components guard routes properly,
        // but good for consistency.
        this.logoutCleanup();
      }
    },
    // Utility to update current user info if changed elsewhere (e.g. settings page)
    setCurrentUser(userData) {
        this.currentUser = userData;
        localStorage.setItem('currentUser', JSON.stringify(userData));
    }
  },
});
