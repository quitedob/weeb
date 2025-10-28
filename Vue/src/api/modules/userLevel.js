import axiosInstance from '../axiosInstance';

/**
 * 用户等级API模块
 * 处理用户等级相关的接口
 */

// 获取用户等级信息
export function getUserLevelInfo(userId) {
  const url = userId ? `/api/users/${userId}/level` : '/api/users/me/level';
  return axiosInstance.get(url);
}

// 获取等级历史记录
export function getLevelHistory(params = {}) {
  return axiosInstance.get('/api/users/me/level/history', { params });
}

// 获取升级进度
export function getUpgradeProgress() {
  return axiosInstance.get('/api/users/me/level/upgrade-progress');
}

// 获取等级配置
export function getLevelConfig() {
  return axiosInstance.get('/api/level/config');
}

// 获取所有等级列表
export function getAllLevels() {
  return axiosInstance.get('/api/level/list');
}

// 管理员：手动调整用户等级
export function adjustUserLevel(userId, levelData) {
  return axiosInstance.post(`/api/admin/users/${userId}/level`, levelData);
}

// 管理员：获取用户等级历史
export function getAdminLevelHistory(userId, params = {}) {
  return axiosInstance.get(`/api/admin/users/${userId}/level/history`, { params });
}

export default {
  getUserLevelInfo,
  getLevelHistory,
  getUpgradeProgress,
  getLevelConfig,
  getAllLevels,
  adjustUserLevel,
  getAdminLevelHistory
};
