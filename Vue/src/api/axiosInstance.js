// File path: /Vue/src/api/axiosInstance.js
// Refactored to use proper ES Module imports and fix Pinia store access timing
// Response interceptor simplified to handle only standardized ApiResponse<T> format

import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '../router';
import { useAuthStore } from '@/stores/authStore';

// 使用环境变量配置 baseURL（Vite 环境）
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const instance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

/* ----- 请求拦截器 -----
   逻辑：
   1. 在请求时调用 useAuthStore() 来获取当前的认证状态
   2. 如果 Pinia 还未初始化，回退到 localStorage
   3. 添加适当的错误处理以防止拦截器失败
*/
instance.interceptors.request.use(
  (config) => {
    let token = null;

    try {
      // 尝试从 Pinia store 获取 token（在请求时调用，确保 Pinia 已初始化）
      const authStore = useAuthStore();
      token = authStore.accessToken;
    } catch (error) {
      // 如果 Pinia 还未初始化或出现其他错误，回退到 localStorage
      console.warn('Pinia store not available, falling back to localStorage:', error);
      token = localStorage.getItem('jwt_token');
    }

    // 如果获取到 token，添加到请求头
    if (token) {
      config.headers = config.headers || {};
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    console.error('Request Error Interceptor:', error);
    return Promise.reject(error);
  }
);

/* ----- 响应拦截器 -----
   简化版本：只处理标准化的 ApiResponse<T> 格式
   1. 所有后端接口现在都返回统一的 ApiResponse 格式
   2. 处理 token 失效（code === -1 或 HTTP 401）时清理 localStorage & Pinia 并跳转到 /login
*/
instance.interceptors.response.use(
  (response) => {
    const res = response.data;
    
    // 检查是否为标准 ApiResponse 格式
    if (typeof res?.code !== 'number' || !('message' in res)) {
      console.warn('Unexpected response format:', res);
      return res; // 返回原始数据，让调用方处理
    }

    // 处理业务错误（code !== 0）
    if (res.code !== 0) {
      ElMessage({ 
        message: res.message || '请求失败', 
        type: 'error', 
        duration: 5000 
      });

      // 处理认证失败（系统错误 code === -1 或未授权 code === 1002）
      if (res.code === -1 || res.code === 1002) {
        try {
          const authStore = useAuthStore();
          if (authStore.logout) {
            authStore.logout();
          }
        } catch (e) {
          console.warn('useAuthStore not available in response interceptor for logout:', e);
          // 回退到手动清理 localStorage
          localStorage.removeItem('jwt_token');
          localStorage.removeItem('currentUser');
        }
        router.push('/login');
      }
      
      return Promise.reject(new Error(res.message || '请求失败'));
    }

    // 成功响应，返回 ApiResponse 对象（包含 data 字段）
    return res;
  },
  async (error) => {
    console.error('Response Error Interceptor:', error);
    let message = error.message;

    if (error.response) {
      // HTTP 状态码处理
      const status = error.response.status;
      if (error.response.data && error.response.data.message) {
        message = error.response.data.message;
      } else {
        switch (status) {
          case 401:
            message = '未授权，请重新登录';
            // 尝试清理 store/localStorage
            try {
              const authStore = useAuthStore();
              if (authStore.logout) {
                authStore.logout();
              }
            } catch (e) {
              console.warn('useAuthStore not available for 401 cleanup:', e);
              // 回退到手动清理 localStorage
              localStorage.removeItem('jwt_token');
              localStorage.removeItem('currentUser');
            }
            router.push('/login');
            break;
          case 403:
            message = '禁止访问';
            break;
          case 404:
            message = '请求资源未找到';
            break;
          case 500:
            message = '服务器内部错误';
            break;
          default:
            message = `网络或请求错误 ${status}`;
        }
      }
    } else if (error.request) {
      message = '请求已发出，但无响应';
    }

    ElMessage({ message, type: 'error', duration: 5000 });
    return Promise.reject(error);
  }
);
