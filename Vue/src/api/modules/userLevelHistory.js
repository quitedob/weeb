// File path: /Vue/src/api/modules/userLevelHistory.js

import axiosInstance from '../axiosInstance';

/**
 * 用户等级历史API模块
 * 与后端UserLevelHistoryController接口对齐
 */

// ================== 基础查询 ==================

/**
 * 获取指定ID的等级变更记录详情
 * @param {number} id - 记录ID
 */
export const getLevelHistoryById = (id) => {
  return axiosInstance.get(`/api/user-level-history/${id}`);
};

/**
 * 获取用户的等级变更历史
 * @param {number} userId - 用户ID
 * @param {object} params - 分页参数 { pageNum, pageSize }
 */
export const getUserLevelHistory = (userId, params = {}) => {
  return axiosInstance.get(`/api/user-level-history/user/${userId}`, { params });
};

// ================== 高级查询 ==================

/**
 * 高级查询等级变更历史（支持多条件筛选）
 * @param {object} queryVo - 查询条件
 */
export const queryLevelHistory = (queryVo) => {
  return axiosInstance.post('/api/user-level-history/query', queryVo);
};

// ================== 快捷查询 ==================

/**
 * 获取用户最近的等级变更记录
 * @param {number} userId - 用户ID
 * @param {number} limit - 返回数量限制，默认5
 */
export const getRecentLevelHistory = (userId, limit = 5) => {
  return axiosInstance.get(`/api/user-level-history/user/${userId}/recent`, {
    params: { limit }
  });
};

/**
 * 获取用户当前等级信息
 * @param {number} userId - 用户ID
 */
export const getCurrentLevel = (userId) => {
  return axiosInstance.get(`/api/user-level-history/user/${userId}/current-level`);
};

// ================== 统计信息 ==================

/**
 * 获取用户的等级变更统计
 * @param {number} userId - 用户ID
 */
export const getUserLevelStats = (userId) => {
  return axiosInstance.get(`/api/user-level-history/user/${userId}/stats`);
};

/**
 * 获取等级提升统计
 * @param {number} days - 统计天数，默认30
 */
export const getLevelUpStats = (days = 30) => {
  return axiosInstance.get('/api/user-level-history/level-up', { params: { days } });
};

/**
 * 获取等级降低统计
 * @param {number} days - 统计天数，默认30
 */
export const getLevelDownStats = (days = 30) => {
  return axiosInstance.get('/api/user-level-history/level-down', { params: { days } });
};

/**
 * 获取用户的等级变更记录数量
 * @param {number} userId - 用户ID
 */
export const getUserLevelHistoryCount = (userId) => {
  return axiosInstance.get(`/api/user-level-history/user/${userId}/count`);
};

export default {
  getLevelHistoryById,
  getUserLevelHistory,
  queryLevelHistory,
  getRecentLevelHistory,
  getCurrentLevel,
  getUserLevelStats,
  getLevelUpStats,
  getLevelDownStats,
  getUserLevelHistoryCount
};
