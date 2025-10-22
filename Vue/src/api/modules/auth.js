// File path: /Vue/src/api/modules/auth.js
import axiosInstance from '../axiosInstance';

export default {
  // 用户登录
  login(data) {
    const loginVo = {
      username: data.username,
      password: data.password,
      rememberMe: data.rememberMe || false
    };
    return axiosInstance.post('/api/auth/login', loginVo);
  },

  // 用户注册
  register(data) {
    return axiosInstance.post('/api/auth/register', data);
  },

  // 用户登出
  logout() {
    return axiosInstance.post('/api/auth/logout');
  },

  // 获取当前用户信息
  getUserInfo() {
    return axiosInstance.get('/api/user/info');
  },

  // 更新当前用户信息
  updateUserInfo(data) {
    return axiosInstance.put('/api/user/info', data);
  },

  // 获取验证码 (mock)
  getCaptcha() {
    return axiosInstance.get('/api/auth/captcha');
  },

  // 忘记密码 (发送重置请求)
  forgotPassword(email) {
    return axiosInstance.post('/api/auth/forgot-password', { email });
  },

  // 重置密码 (使用令牌完成重置)
  resetPassword(data) {
    // data expected to contain: token, newPassword
    return axiosInstance.post('/api/auth/reset-password', data);
  }
};
