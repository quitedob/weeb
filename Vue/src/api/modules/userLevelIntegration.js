// File path: /Vue/src/api/modules/userLevelIntegration.js

import axiosInstance from '../axiosInstance';

/**
 * 用户等级集成API模块
 * 与后端UserLevelIntegrationController接口对齐
 */

// ================== 等级变更处理 ==================

/**
 * 处理用户等级变更（自动记录历史并同步角色）
 * @param {number} userId - 用户ID
 * @param {number} newLevel - 新等级
 * @param {string} changeReason - 变更原因
 * @param {number} changeType - 变更类型（1:系统自动, 2:管理员操作, 3:用户行为触发）
 * @param {number} operatorId - 操作者ID（可选）
 */
export const changeUserLevel = (userId, newLevel, changeReason, changeType, operatorId = null) => {
  const payload = {
    userId,
    newLevel,
    changeReason,
    changeType,
    operatorId
  };
  return axiosInstance.post('/api/user-level-integration/change-level', payload);
};

// ================== 等级查询 ==================

/**
 * 获取用户的完整等级信息
 * @param {number} userId - 用户ID
 */
export const getUserLevelInfo = (userId) => {
  return axiosInstance.get(`/api/user-level-integration/user/${userId}/level-info`);
};

// ================== 等级同步 ==================

/**
 * 根据用户等级同步角色权限
 * @param {number} userId - 用户ID
 * @param {boolean} forceSync - 是否强制同步，默认false
 */
export const syncUserRoles = (userId, forceSync = false) => {
  return axiosInstance.post('/api/user-level-integration/sync-roles', {
    userId,
    forceSync
  });
};

// ================== 等级推荐 ==================

/**
 * 为用户推荐下一个等级
 * @param {number} userId - 用户ID
 */
export const recommendNextLevel = (userId) => {
  return axiosInstance.get(`/api/user-level-integration/recommend-next-level/${userId}`);
};

export default {
  changeUserLevel,
  getUserLevelInfo,
  syncUserRoles,
  recommendNextLevel
};
