// File path: /Vue/src/api/modules/rolePermission.js

import axiosInstance from '../axiosInstance';

/**
 * 角色权限管理API模块
 * 与后端RolePermissionController接口对齐
 */

// ================== 自动分配角色 ==================

/**
 * 为用户自动分配角色（基于用户等级）
 * @param {number} userId - 用户ID
 */
export const autoAssignRole = (userId) => {
  return axiosInstance.post(`/api/role-permissions/auto-assign/${userId}`);
};

/**
 * 批量自动分配角色
 * @param {Array<number>} userIds - 用户ID列表
 */
export const batchAutoAssignRoles = (userIds) => {
  return axiosInstance.post('/api/role-permissions/batch-auto-assign', { userIds });
};

// ================== 角色等级关联 ==================

/**
 * 获取指定等级对应的角色列表
 * @param {number} userLevel - 用户等级
 */
export const getRolesForLevel = (userLevel) => {
  return axiosInstance.get(`/api/role-permissions/roles-for-level/${userLevel}`);
};

// ================== 用户角色验证 ==================

/**
 * 检查用户是否有指定角色
 * @param {number} userId - 用户ID
 * @param {string} roleName - 角色名称
 */
export const hasRole = (userId, roleName) => {
  return axiosInstance.get(`/api/role-permissions/user/${userId}/has-role/${roleName}`);
};

// ================== 角色分配管理 ==================

/**
 * 为用户分配角色
 * @param {number} userId - 用户ID
 * @param {number} roleId - 角色ID
 */
export const assignRole = (userId, roleId) => {
  return axiosInstance.post('/api/role-permissions/assign', { userId, roleId });
};

/**
 * 从用户移除角色
 * @param {number} userId - 用户ID
 * @param {number} roleId - 角色ID
 */
export const removeRole = (userId, roleId) => {
  return axiosInstance.post('/api/role-permissions/remove', { userId, roleId });
};

// ================== 用户角色查询 ==================

/**
 * 获取用户的所有角色
 * @param {number} userId - 用户ID
 */
export const getUserRoles = (userId) => {
  return axiosInstance.get(`/api/role-permissions/user/${userId}/roles`);
};

// ================== 角色权限查询 ==================

/**
 * 获取角色的所有权限
 * @param {number} roleId - 角色ID
 */
export const getRolePermissions = (roleId) => {
  return axiosInstance.get(`/api/role-permissions/role/${roleId}/permissions`);
};

// ================== 用户权限查询 ==================

/**
 * 获取用户的所有权限
 * @param {number} userId - 用户ID
 */
export const getUserAllPermissions = (userId) => {
  return axiosInstance.get(`/api/role-permissions/user/${userId}/all-permissions`);
};

/**
 * 检查用户是否有指定权限
 * @param {number} userId - 用户ID
 * @param {string} permission - 权限标识
 */
export const hasPermission = (userId, permission) => {
  return axiosInstance.get(`/api/role-permissions/user/${userId}/has-permission/${permission}`);
};

// ================== 权限更新 ==================

/**
 * 更新角色的权限列表
 * @param {number} roleId - 角色ID
 * @param {Array<number>} permissionIds - 权限ID列表
 */
export const updateRolePermissions = (roleId, permissionIds) => {
  return axiosInstance.post('/api/role-permissions/update-role-permissions', {
    roleId,
    permissionIds
  });
};

// ================== 统计信息 ==================

/**
 * 获取角色权限统计信息
 */
export const getStatistics = () => {
  return axiosInstance.get('/api/role-permissions/statistics');
};

// ================== 角色验证 ==================

/**
 * 验证角色配置是否有效
 * @param {number} roleId - 角色ID
 */
export const validateRole = (roleId) => {
  return axiosInstance.get(`/api/role-permissions/validate/${roleId}`);
};

// ================== 等级同步 ==================

/**
 * 当用户等级变更时同步角色
 * @param {number} userId - 用户ID
 * @param {number} oldLevel - 原等级
 * @param {number} newLevel - 新等级
 */
export const syncOnLevelChange = (userId, oldLevel, newLevel) => {
  return axiosInstance.post('/api/role-permissions/sync-on-level-change', {
    userId,
    oldLevel,
    newLevel
  });
};

// ================== 推荐角色 ==================

/**
 * 获取等级推荐的角色
 * @param {number} userLevel - 用户等级
 */
export const getRecommendedRoles = (userLevel) => {
  return axiosInstance.get(`/api/role-permissions/recommendations/${userLevel}`);
};

// ================== 模板应用 ==================

/**
 * 应用角色权限模板
 * @param {number} templateId - 模板ID
 * @param {number} roleId - 目标角色ID
 */
export const applyTemplate = (templateId, roleId) => {
  return axiosInstance.post('/api/role-permissions/apply-template', {
    templateId,
    roleId
  });
};

export default {
  autoAssignRole,
  batchAutoAssignRoles,
  getRolesForLevel,
  hasRole,
  assignRole,
  removeRole,
  getUserRoles,
  getRolePermissions,
  getUserAllPermissions,
  hasPermission,
  updateRolePermissions,
  getStatistics,
  validateRole,
  syncOnLevelChange,
  getRecommendedRoles,
  applyTemplate
};
