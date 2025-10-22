// File path: /Vue/src/api/modules/admin.js

import axiosInstance from '../axiosInstance';

/**
 * 管理员服务 API
 * 负责用户、角色、权限管理等操作
 */

// ================== 用户管理 ==================

/**
 * 获取用户列表（分页）
 * @param {object} params - 查询参数 { page, pageSize, keyword }
 */
export const getUsers = (params) => {
    return axiosInstance.get('/api/admin/users', { params });
};

/**
 * 封禁用户
 * @param {number} userId - 用户ID
 */
export const banUser = (userId) => {
    return axiosInstance.post(`/api/admin/users/${userId}/ban`);
};

/**
 * 解封用户
 * @param {number} userId - 用户ID
 */
export const unbanUser = (userId) => {
    return axiosInstance.post(`/api/admin/users/${userId}/unban`);
};

/**
 * 为用户分配角色
 * @param {number} userId - 用户ID
 * @param {number} roleId - 角色ID
 */
export const assignRoleToUser = (userId, roleId) => {
    return axiosInstance.post(`/api/admin/users/${userId}/roles/${roleId}`);
};

/**
 * 从用户移除角色
 * @param {number} userId - 用户ID
 * @param {number} roleId - 角色ID
 */
export const removeRoleFromUser = (userId, roleId) => {
    return axiosInstance.delete(`/api/admin/users/${userId}/roles/${roleId}`);
};


// ================== 角色管理 ==================

/**
 * 获取角色列表（分页）
 * @param {object} params - 查询参数 { page, pageSize, keyword }
 */
export const getRoles = (params) => {
    return axiosInstance.get('/api/admin/roles', { params });
};

/**
 * 创建角色
 * @param {object} roleData - 角色数据
 */
export const createRole = (roleData) => {
    return axiosInstance.post('/api/admin/roles', roleData);
};

/**
 * 更新角色
 * @param {number} roleId - 角色ID
 * @param {object} roleData - 角色数据
 */
export const updateRole = (roleId, roleData) => {
    return axiosInstance.put(`/api/admin/roles/${roleId}`, roleData);
};

/**
 * 删除角色
 * @param {number} roleId - 角色ID
 */
export const deleteRole = (roleId) => {
    return axiosInstance.delete(`/api/admin/roles/${roleId}`);
};

/**
 * 获取角色的权限列表
 * @param {number} roleId - 角色ID
 */
export const getRolePermissions = (roleId) => {
    return axiosInstance.get(`/api/admin/roles/${roleId}/permissions`);
};

/**
 * 为角色分配权限
 * @param {number} roleId - 角色ID
 * @param {Array<number>} permissionIds - 权限ID列表
 */
export const assignPermissionsToRole = (roleId, permissionIds) => {
    return axiosInstance.post(`/api/admin/roles/${roleId}/permissions`, permissionIds);
};


// ================== 权限管理 ==================

/**
 * 获取权限列表（分页）
 * @param {object} params - 查询参数 { page, pageSize, keyword }
 */
export const getPermissions = (params) => {
    return axiosInstance.get('/api/admin/permissions', { params });
};

/**
 * 创建权限
 * @param {object} permissionData - 权限数据
 */
export const createPermission = (permissionData) => {
    return axiosInstance.post('/api/admin/permissions', permissionData);
};

/**
 * 更新权限
 * @param {number} permissionId - 权限ID
 * @param {object} permissionData - 权限数据
 */
export const updatePermission = (permissionId, permissionData) => {
    return axiosInstance.put(`/api/admin/permissions/${permissionId}`, permissionData);
};

/**
 * 删除权限
 * @param {number} permissionId - 权限ID
 */
export const deletePermission = (permissionId) => {
    return axiosInstance.delete(`/api/admin/permissions/${permissionId}`);
};

// ================== 系统管理 ==================

/**
 * 获取系统统计信息
 */
export const getSystemStatistics = () => {
    return axiosInstance.get('/api/admin/statistics');
};

/**
 * 获取系统日志（分页）
 * @param {object} params - 查询参数 { page, pageSize }
 */
export const getSystemLogs = (params) => {
    return axiosInstance.get('/api/admin/logs', { params });
};

export default {
    getUsers,
    banUser,
    unbanUser,
    assignRoleToUser,
    removeRoleFromUser,
    getRoles,
    createRole,
    updateRole,
    deleteRole,
    getRolePermissions,
    assignPermissionsToRole,
    getPermissions,
    createPermission,
    updatePermission,
    deletePermission,
    getSystemStatistics,
    getSystemLogs
};
