// File path: /Vue/src/api/axiosInstance.js
import axios from 'axios';
import { ElMessage } from 'element-plus'; // Assuming Element Plus is installed and set up
import router from '../router'; // Import router for navigation on auth errors
import { useAuthStore } from '@/stores/authStore'; // Import authStore

// 使用环境变量配置baseURL，支持多环境部署
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const instance = axios.create({
    baseURL: API_BASE_URL, // 从环境变量获取后端服务器地址
    timeout: 10000,  // Request timeout
});

// Request interceptor - 修复token获取逻辑，优先使用localStorage，延迟获取Pinia store
instance.interceptors.request.use(
    config => {
        // 优先从localStorage获取token，确保在Pinia未初始化时也能正常工作
        let token = localStorage.getItem('jwt_token');

        // 如果localStorage中没有token，尝试从Pinia store获取（但不强制要求）
        if (!token) {
            try {
                // 在函数内部导入store，避免在模块顶部导入导致的初始化问题
                const { useAuthStore } = require('@/stores/authStore');
                const authStore = useAuthStore();
                token = authStore.accessToken;
            } catch (e) {
                // Pinia未初始化或store不可用，继续使用null token
                console.warn('Pinia store not available for token retrieval:', e);
            }
        }

        // 设置Authorization头
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }

        return config;
    },
    error => {
        console.error('Request Error Interceptor:', error);
        return Promise.reject(error);
    }
);

// Response interceptor
instance.interceptors.response.use(
    response => {
        const res = response.data;
        // 兼容两种响应结构：ApiResponse 与 ResultUtil
        const isApiResponse = (typeof res.code === 'number' && 'message' in res);
        const isResultUtil = (typeof res.code === 'number' && ('msg' in res || 'data' in res) && !('message' in res));

        if (isApiResponse) {
            if (res.code !== 0) {
                ElMessage({ message: res.message || '请求失败', type: 'error', duration: 5000 });
                return Promise.reject(new Error(res.message || '请求失败'));
            }
            return res; // ApiResponse 直接返回
        }

        if (res.success === false || (isResultUtil && res.code !== 0)) {
            ElMessage({
                message: res.message || res.msg || '请求失败',
                type: 'error',
                duration: 5 * 1000
            });

            // 处理认证错误
            if (res.code === -1) { // token失效
                try {
                    // 在函数内部导入store，避免初始化问题
                    const { useAuthStore } = require('@/stores/authStore');
                    const authStore = useAuthStore();
                    authStore.logout(); // 清除前端状态
                } catch (e) {
                    console.warn('useAuthStore not available in response interceptor for logout:', e);
                    // 手动清理localStorage
                    localStorage.removeItem('jwt_token');
                    localStorage.removeItem('currentUser');
                }
                router.push('/login');
            }
            return Promise.reject(new Error(res.message || res.msg || '请求失败'));
        }
        return res;
    },
    error => {
        console.error('Response Error Interceptor:', error);
        let message = error.message;
        if (error.response && error.response.data && error.response.data.message) {
            message = error.response.data.message;
        } else if (error.response && error.response.status) {
             switch (error.response.status) {
                case 401:
                    message = '未授权，请重新登录'; // Unauthorized, please log in again
                    try {
                        // 在函数内部导入store，避免初始化问题
                        const { useAuthStore } = require('@/stores/authStore');
                        const authStore = useAuthStore();
                        authStore.logout();
                    } catch (e) {
                        console.warn('useAuthStore not available in 401 error interceptor:', e);
                        // 手动清理localStorage
                        localStorage.removeItem('jwt_token');
                        localStorage.removeItem('currentUser');
                    }
                    router.push('/login');
                    break;
                case 403:
                    message = '禁止访问'; // Forbidden
                    break;
                case 404:
                    message = '请求资源未找到'; // Resource not found
                    break;
                case 500:
                    message = '服务器内部错误'; // Internal server error
                    break;
                default:
                    message = `网络或请求错误 ${error.response.status}`; // Connection error
            }
        } else if (error.request) {
            message = '请求已发出，但无响应'; // Request made but no response
        }

        ElMessage({
            message: message,
            type: 'error',
            duration: 5 * 1000
        });
        return Promise.reject(error);
    }
);
