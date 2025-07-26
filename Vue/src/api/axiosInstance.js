// File path: /Vue/src/api/axiosInstance.js
import axios from 'axios';
import { ElMessage } from 'element-plus'; // Assuming Element Plus is installed and set up
import router from '../router'; // Import router for navigation on auth errors
import { useAuthStore } from '@/stores/authStore'; // Import authStore

export const instance = axios.create({
    baseURL: 'http://localhost:8080', // 后端服务器地址
    timeout: 10000,  // Request timeout
});

// Request interceptor
instance.interceptors.request.use(
    config => {
        // Using authStore as per user's example:
        // This assumes authStore is already initialized when API calls are made.
        // If authStore itself makes API calls upon initialization, ensure no circular dependency.
        try {
            const authStore = useAuthStore();
            const token = authStore.accessToken;
            if (token) {
                config.headers['Authorization'] = `Bearer ${token}`;
            }
        } catch (e) {
            // This can happen if Pinia is not yet initialized, or if the store is accessed
            // too early in the app lifecycle (e.g. if a module using this instance is imported
            // at the very top level of main.js before Pinia is setup).
            // Fallback to localStorage or handle error.
            console.warn('Could not get token from useAuthStore in axiosInstance request interceptor:', e);
            const token = localStorage.getItem('jwt_token'); // Fallback example
             if (token) {
                config.headers['Authorization'] = `Bearer ${token}`;
            }
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
        // 后端返回的数据结构
        if (res.success === false) {
            ElMessage({
                message: res.message || '请求失败',
                type: 'error',
                duration: 5 * 1000
            });

            // 处理认证错误
            if (error.response && (error.response.status === 401 || error.response.status === 403)) {
                try {
                    const authStore = useAuthStore();
                    authStore.logout(); // 清除前端状态
                } catch (e) {
                    console.warn('useAuthStore not available in response interceptor for logout:', e);
                    localStorage.removeItem('jwt_token'); // 手动清理
                }
                router.push('/login'); // 重定向到登录页
            }
            return Promise.reject(new Error(res.message || '请求失败'));
        }
        return res; // 返回后端响应数据
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
                        const authStore = useAuthStore();
                        authStore.logout();
                    } catch (e) {
                        console.warn('useAuthStore not available in 401 error interceptor:', e);
                        localStorage.removeItem('jwt_token'); // Manual cleanup
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
