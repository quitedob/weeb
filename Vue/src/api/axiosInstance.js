// File path: /Vue/src/api/axiosInstance.js
// Refactored to use proper ES Module imports and fix Pinia store access timing
// Response interceptor simplified to handle only standardized ApiResponse<T> format

import axios from 'axios';
import { ElMessage } from 'element-plus';
// 不再需要从这里导入 router
import { useAuthStore } from '@/stores/authStore';

// 使用环境变量配置 baseURL（Vite 环境）
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const instance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000, // Increased timeout for better user experience
  retry: 2, // Number of retries for failed requests
  retryDelay: 1000, // Delay between retries in milliseconds
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
        // **核心修改点**：只清理状态，不跳转页面
        useAuthStore().logout();
      }
      
      return Promise.reject(new Error(res.message || '请求失败'));
    }

    // 成功响应，返回 ApiResponse 对象（包含 data 字段）
    return res;
  },
  async (error) => {
    console.error('Response Error Interceptor:', error);
    let message = error.message;
    let shouldRetry = false;

    if (error.response) {
      // HTTP 状态码处理
      const status = error.response.status;
      if (error.response.data && error.response.data.message) {
        message = error.response.data.message;
      } else {
        switch (status) {
          case 401:
            message = '认证失败，请重新登录';
            useAuthStore().logout();
            break;
          case 403:
            message = '禁止访问';
            if (!error.config.url?.includes('/logout')) {
              useAuthStore().logout();
            }
            break;
          case 404:
            message = '请求资源未找到';
            break;
          case 408:
            message = '请求超时';
            shouldRetry = true;
            break;
          case 429:
            message = '请求过于频繁，请稍后再试';
            break;
          case 500:
            message = '服务器内部错误';
            shouldRetry = true;
            break;
          case 502:
          case 503:
          case 504:
            message = '服务器暂时不可用，请稍后再试';
            shouldRetry = true;
            break;
          default:
            message = `网络或请求错误 ${status}`;
        }
      }
    } else if (error.request) {
      // 网络错误
      if (error.code === 'ECONNABORTED' && error.message.includes('timeout')) {
        message = '请求超时，请检查网络连接';
        shouldRetry = true;
      } else {
        message = '网络连接失败，请检查网络设置';
        shouldRetry = true;
      }
    }

    // 检查是否应该重试
    const config = error.config;
    if (shouldRetry && config && (!config.retry || config.retry > 0)) {
      config.retry = config.retry || instance.defaults.retry;
      config.retryCount = config.retryCount || 0;

      if (config.retryCount < config.retry) {
        config.retryCount++;

        // 创建新的Promise来重试请求
        const retryDelay = config.retryDelay || instance.defaults.retryDelay;

        return new Promise(resolve => {
          setTimeout(() => {
            console.log(`Retrying request (${config.retryCount}/${config.retry}): ${config.url}`);
            resolve(instance(config));
          }, retryDelay);
        });
      }
    }

    // 显示错误消息（对于非重试或重试失败的情况）
    if (!shouldRetry || (config && config.retryCount >= config.retry)) {
      ElMessage({
        message,
        type: 'error',
        duration: 5000,
        showClose: true
      });
    }

    return Promise.reject(error);
  }
);

// 默认导出 axios 实例
export default instance;
