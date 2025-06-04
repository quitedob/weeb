// 功能说明: 统一管理项目的所有API请求
import axios from 'axios';

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 后端接口的基础路径, vite.config.js中已配置代理
  timeout: 15000, // 请求超时时间
});

// 请求拦截器: 为每个请求附带JWT
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    console.error('Request Error:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器: 可用于统一处理错误
service.interceptors.response.use(
  (response) => {
    // 如果后端返回的 code 不是 200, 则认为是错误
    if (response.data.code && response.data.code !== 200) {
      console.error('API Error:', response.data.message);
      // 在这里可以添加全局错误提示, e.g., using a library like Element Plus
    }
    return response.data; // 直接返回后端响应的 data 部分
  },
  (error) => {
    console.error('Network/Response Error:', error);
    return Promise.reject(error);
  }
);

// 导出所有API函数
export default {
  // 认证模块
  login: (data) => service.post('/login', data),
  register: (data) => service.post('/register', data),
  getCaptcha: () => service.get('/captcha'),

  // 用户模块
  getUserInfo: () => service.get('/user/info'),
  updateUserInfo: (data) => service.post('/user/update', data),

  // 群组模块
  createGroup: (data) => service.post('/group/create'), // Path should be /api/group/create based on GroupController
  getMyGroups: () => service.get('/group/my-list'),   // Path should be /api/group/my-list
  joinGroup: (data) => service.post('/group/join'),   // Path should be /api/group/join
  quitGroup: (data) => service.post('/group/quit'),   // Path should be /api/group/quit
  searchGroups: (keyword) => service.get(`/search/group?keyword=${keyword}`), // Path for search/group needs to be checked against SearchController
};
