import axiosInstance from '../axiosInstance';

/**
 * 用户API模块
 * 与后端UserController接口对齐
 */

// 获取当前用户信息
export function getCurrentUser() {
  return axiosInstance.get('/api/users/me');
}

// 获取当前用户信息（兼容接口）
export function getCurrentUserProfile() {
  return axiosInstance.get('/api/users/me/profile');
}

// 获取当前用户基本信息
export function getCurrentUserInfo() {
  return axiosInstance.get('/api/users/me/info');
}

// 更新当前用户信息
export function updateCurrentUser(userData) {
  return axiosInstance.put('/api/users/me', userData);
}

// 获取用户列表
export function getUsers(params = {}) {
  return axiosInstance.get('/api/users', { params });
}

// 搜索用户
export function searchUsers(params = {}) {
  return axiosInstance.get('/api/users/search', { params });
}

// 根据用户ID获取用户信息
export function getUserById(userId) {
  return axiosInstance.get(`/api/users/${userId}`);
}

// 获取当前用户的群组列表
export function getMyGroups() {
  return axiosInstance.get('/api/users/me/groups');
}

// 关注用户
export function followUser(userId) {
  return axiosInstance.post(`/api/users/${userId}/follow`);
}

// 取消关注用户
export function unfollowUser(userId) {
  return axiosInstance.delete(`/api/users/${userId}/follow`);
}

// 获取用户关注列表
export function getUserFollowing(userId, params = {}) {
  return axiosInstance.get(`/api/users/${userId}/following`, { params });
}

// 获取用户粉丝列表
export function getUserFollowers(userId, params = {}) {
  return axiosInstance.get(`/api/users/${userId}/followers`, { params });
}

// 检查关注状态
export function checkFollowStatus(userId) {
  return axiosInstance.get(`/api/users/${userId}/follow/status`);
}

// 获取关注统计
export function getFollowStats() {
  return axiosInstance.get('/api/user-follows/count/me');
}

// 获取指定用户的关注统计
export function getUserFollowStats(userId) {
  return axiosInstance.get(`/api/user-follows/count/${userId}`);
}

// 获取用户统计信息
export function getUserStats(userId) {
  return axiosInstance.get(`/api/users/${userId}/stats`);
}

// 获取用户活动记录
export function getUserActivities(userId) {
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

// 用户操作（管理员功能）
export function banUser(userId) {
  return axiosInstance.post(`/api/users/${userId}/ban`);
}

export function unbanUser(userId) {
  return axiosInstance.post(`/api/users/${userId}/unban`);
}

export function resetUserPassword(userId) {
  return axiosInstance.post(`/api/users/${userId}/reset-password`);
}

export default {
  getCurrentUser,
  getCurrentUserProfile,
  getCurrentUserInfo,
  updateCurrentUser,
  getUsers,
  searchUsers,
  getUserById,
  getMyGroups,
  followUser,
  unfollowUser,
  getUserFollowing,
  getUserFollowers,
  checkFollowStatus,
  getFollowStats,
  getUserFollowStats,
  getUserStats,
  getUserActivities,
  updateProfile,
  uploadAvatar,
  banUser,
  unbanUser,
  resetUserPassword
};
