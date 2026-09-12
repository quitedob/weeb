// File path: /Vue/src/api/axiosInstance.js
// Refactored to use proper ES Module imports and fix Pinia store access timing
// Response interceptor simplified to handle only standardized ApiResponse<T> format

import axios from 'axios';
import appleMessage from '@/utils/appleMessage';
// 不再需要从这里导入 router
import { useAuthStore } from '@/stores/authStore';
import { log } from '@/utils/logger';
import { resolveAvatarUrls } from '@/utils/assetUrl';
import { captureSession, isCurrentSession, isCurrentCredential, waitForCredentialRenewal } from '@/utils/session';
import { apiBaseUrl } from '@/utils/serviceUrls';

const staleRequest = config => new axios.CanceledError('Request belongs to an inactive session', config);
// A renewal request already owns the lock; waiting on itself would deadlock.
const isRenewalRequest = config => config?.url?.split('?')[0].endsWith('/api/auth/refresh');

// 区分开发环境和生产环境的 baseURL
// 1. 开发环境 (import.meta.env.DEV) 时，使用相对路径 '/'
//    这样所有请求（如 /api/auth/login）都会发往 http://localhost:5173
//    然后被 vite.config.js 中的 proxy 拦截并转发到 http://localhost:8080，完美避开CORS。
// 2. 生产环境 (import.meta.env.PROD) 时，才使用 .env 文件中配置的 VITE_API_BASE_URL。
const API_BASE_URL = apiBaseUrl();

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
      if (config.authSession && !isCurrentSession(config.authSession)) throw staleRequest(config);
      const previousToken = config.headers?.Authorization?.replace(/^Bearer /, '');
      if (!config.authSession && previousToken && previousToken !== token) throw staleRequest(config);
      config.authSession = { ...captureSession(), token: token ?? null };
    } catch (error) {
      if (axios.isCancel(error)) throw error;
      // 如果 Pinia 还未初始化或出现其他错误，回退到 localStorage
      log.warn('Pinia store not available, falling back to localStorage:', error);
      token = localStorage.getItem('jwt_token');
    }

    // 如果获取到 token，添加到请求头
    if (token) {
      config.headers = config.headers || {};
      config.headers['Authorization'] = `Bearer ${token}`;
    } else if (config.authSession) {
      delete config.headers.Authorization;
    }

    return config;
  },
  (error) => {
    log.error('Request Error Interceptor:', error);
    throw error;
  },
  { synchronous: true }
);

/* ----- 响应拦截器 -----
   简化版本：只处理标准化的 ApiResponse<T> 格式
   1. 所有后端接口现在都返回统一的 ApiResponse 格式
   2. 处理 token 失效（code === -1 或 HTTP 401）时清理 localStorage & Pinia 并跳转到 /login
*/
instance.interceptors.response.use(
  async (response) => {
    const session = response.config?.authSession;
    if (session && !isCurrentSession(session)) throw staleRequest(response.config);
    if (response.status === 204) {
      return { code: 0, message: '', data: null };
    }
    const res = response.data;
    
    // 检查是否为标准 ApiResponse 格式
    if (typeof res?.code !== 'number' || !('message' in res)) {
      log.warn('Unexpected response format:', res);
      return res; // 返回原始数据，让调用方处理
    }

    // 处理业务错误（code !== 0）
    if (res.code !== 0) {
      if (res.code === 1002 && !isRenewalRequest(response.config)) await waitForCredentialRenewal(session);
      if (res.code === 1002 && session && !isCurrentCredential(session)) throw staleRequest(response.config);
      appleMessage.error(res.message || '请求失败', 5000);

      // ApiResponse.ErrorCode.UNAUTHORIZED = 1002; system errors retain the session.
      if (res.code === 1002 && useAuthStore().accessToken) {
        // **核心修改点**：只清理状态，不跳转页面
        useAuthStore().logoutCleanup();
      }
      
      return Promise.reject(new Error(res.message || '请求失败'));
    }

    // 成功响应，返回 ApiResponse 对象（包含 data 字段）
    return { ...res, data: resolveAvatarUrls(res.data) };
  },
  async (error) => {
    if (axios.isCancel(error)) return Promise.reject(error);
    const session = error.config?.authSession;
    if (error.response?.status === 401 && !isRenewalRequest(error.config)) await waitForCredentialRenewal(session);
    if (session && (!isCurrentSession(session) || (error.response?.status === 401 && !isCurrentCredential(session)))) {
      return Promise.reject(staleRequest(error.config));
    }
    log.error('Response Error Interceptor:', { status: error.response?.status, message: error.message });
    let message = error.message;
    let shouldRetry = false;

    if (error.response) {
      const status = error.response.status;
      if (status === 401 && useAuthStore().accessToken) useAuthStore().logoutCleanup();
      const messages = {
        401: '认证失败，请重新登录', 403: '禁止访问', 404: '请求资源未找到',
        408: '请求超时', 429: '请求过于频繁，请稍后再试',
        500: '服务器内部错误', 502: '服务器暂时不可用，请稍后再试',
        503: '服务器暂时不可用，请稍后再试', 504: '服务器暂时不可用，请稍后再试'
      };
      message = error.response.data?.message || messages[status] || `网络或请求错误 ${status}`;
      shouldRetry = [408, 500, 502, 503, 504].includes(status);
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
    const safeToRetry = ['get', 'head', 'options'].includes(config?.method?.toLowerCase());
    if (shouldRetry && safeToRetry && config && (config.retry ?? instance.defaults.retry) > 0) {
      config.retry = config.retry ?? instance.defaults.retry;
      config.retryCount = config.retryCount || 0;

      if (config.retryCount < config.retry) {
        config.retryCount++;

        // 创建新的Promise来重试请求
        const retryDelay = config.retryDelay ?? instance.defaults.retryDelay;

        return new Promise(resolve => {
          setTimeout(() => {
            log.info(`Retrying request (${config.retryCount}/${config.retry}): ${config.url}`);
            resolve(instance(config));
          }, retryDelay);
        });
      }
    }

    appleMessage.error(message, 5000);
    error.message = message;

    return Promise.reject(error);
  }
);

// 默认导出 axios 实例
export default instance;
