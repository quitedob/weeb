import axiosInstance from '../axiosInstance';

/**
 * 用户API模块
 * 与后端AuthController接口对齐
 */

// 获取用户列表
export function getUserList() {
  return axiosInstance.get('/api/list');
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

// 获取用户信息
export function getUserInfo() {
  return axiosInstance.get('/api/user/info');
}

// 更新用户信息（使用PUT）
export function updateUserInfo(data) {
  return axiosInstance.put('/api/user/info', data);
}

// 根据用户ID获取用户信息
export function getUserInfoById(userId) {
  return axiosInstance.get(`/api/user/${userId}`);
}

export default {
  getUserList,
  getUserMap,
  getOnlineUsers,
  findByUsername,
  findByUserID,
  updateUser,
  getUserInfo,
  updateUserInfo,
  getUserInfoById
};
