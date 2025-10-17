// File path: /Vue/src/api/modules/auth.js
import axiosInstance from '../axiosInstance';

export default {
  // 用户登录 - 使用LoginVo格式
  login(data) {
    // 确保数据格式符合LoginVo
    const loginVo = {
      username: data.username,
      password: data.password,
      rememberMe: data.rememberMe || false
    };
    return axiosInstance.post('/api/login', loginVo);
  },

  // 用户注册
  register(data) {
    return axiosInstance.post('/api/register', data);
  },

  // 用户登出
  logout() {
    return axiosInstance.post('/api/logout');
  },

  // 获取用户信息 - 修复路径匹配
  getUserInfo() {
    return axiosInstance.get('/api/user/info');
  },

  // 更新用户信息 - 使用UpdateUserVo格式
  updateUserInfo(data) {
    // 确保数据格式符合UpdateUserVo
    const updateVo = {
      username: data.username,
      userEmail: data.userEmail,
      phoneNumber: data.phoneNumber,
      sex: data.sex,
      nickname: data.nickname,
      bio: data.bio,
      avatar: data.avatar
    };
    return axiosInstance.put('/api/user/info', updateVo);
  },

  // 获取验证码
  getCaptcha() {
    return axiosInstance.get('/api/captcha');
  },

  // 忘记密码
  forgotPassword(data) {
    return axiosInstance.post('/api/forget', data);
  },

  // 重置密码
  resetPassword(data) {
    return axiosInstance.post('/api/reset', data);
  },

  // 发送密码重置链接
  sendPasswordResetLink(email) {
    return axiosInstance.post('/api/forgot-password', { email });
  },

  // 验证重置令牌
  validateResetToken(token) {
    return axiosInstance.get('/api/validate-reset-token', {
      params: { token }
    });
  },

  // 执行密码重置（前端页面调用的接口）
  executePasswordReset(data) {
    return axiosInstance.post('/api/password/execute-reset', data);
  },

  // 重置密码（原有的接口）
  resetPasswordOld(data) {
    return axiosInstance.post('/api/reset-password', data);
  }
};
