import axiosInstance from '../axiosInstance';

/**
 * 用户API模块
 * 与后端AuthController接口对齐
 */

// 获取用户列表 - 使用正确的API路径
export function getUserList(params = {}) {
  return axiosInstance.get('/api/user/list', { params });
}

// 获取用户Map
export function getUserMap() {
  return axiosInstance.get('/api/list/map');
}

// 获取在线用户列表
export function getOnlineUsers() {
  return axiosInstance.get('/api/online/web');
}

// 通过用户名查找用户
export function findByUsername(username) {
  return axiosInstance.get('/api/findByUsername', {
    params: { username }
  });
}

// 通过用户ID查找用户
export function findByUserID(userID) {
  return axiosInstance.get('/api/findByUserID', {
    params: { userID }
  });
}

// 更新用户信息
export function updateUser(updateData) {
  return axiosInstance.post('/api/update', updateData);
}

// 根据用户ID获取用户信息
export function getUserInfoById(userId) {
  return axiosInstance.get(`/api/users/${userId}`);
}

// 获取用户统计信息
export function getUserStats(userId) {
  return axiosInstance.get(`/api/users/${userId}/stats`);
}

// 获取用户最近活动
export function getRecentActivities(userId) {
  return axiosInstance.get(`/api/users/${userId}/activities`);
}

// 更新个人资料
export function updateProfile(profileData) {
  return axiosInstance.put('/api/users/profile', profileData);
}

// 上传用户头像
export function uploadAvatar(formData) {
  return axiosInstance.post('/api/users/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

export default {
  getUserList,
  getUserMap,
  getOnlineUsers,
  findByUsername,
  findByUserID,
  updateUser,
  getUserInfoById,
  getUserStats,
  getRecentActivities,
  updateProfile,
  uploadAvatar
};
