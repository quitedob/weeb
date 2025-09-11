// File path: /Vue/src/api/modules/auth.js
import { instance } from '../axiosInstance';

export default {
  // 用户登录
  login(data) {
    return instance.post('/login', data);
  },
  
  // 用户注册
  register(data) {
    return instance.post('/register', data);
  },
  
  // 用户登出
  logout() {
    return instance.post('/logout');
  },
  
  // 获取用户信息 - 修复路径匹配
  getUserInfo() {
    return instance.get('/api/user/info');
  },

  // 更新用户信息
  updateUserInfo(data) {
    return instance.put('/api/user/info', data);
  },
  
  // 获取验证码
  getCaptcha() {
    return instance.get('/captcha');
  },
  
  // 忘记密码
  forgotPassword(data) {
    return instance.post('/forget', data);
  },
  
  // 重置密码
  resetPassword(data) {
    return instance.post('/reset', data);
  }
};
