import axiosInstance from '../axiosInstance';

/**
 * 角色权限相关API
 */

// 获取所有角色列�?
export const getRoles = () => {
  return axiosInstance({
    url: '/api/roles',
    method: 'get'
  });
};

// 获取角色详情
export const getRoleById = (roleId) => {
  return axiosInstance({
    url: `/api/roles/${roleId}`,
    method: 'get'
  });
};

// 创建角色
export const createRole = (data) => {
  return axiosInstance({
    url: '/api/roles',
    method: 'post',
    data
  });
};

// 更新角色
export const updateRole = (roleId, data) => {
  return axiosInstance({
    url: `/api/roles/${roleId}`,
    method: 'put',
    data
  });
};

// 删除角色
export const deleteRole = (roleId) => {
  return axiosInstance({
    url: `/api/roles/${roleId}`,
    method: 'delete'
  });
};

// 获取角色的权限列�?
export const getRolePermissions = (roleId) => {
  return axiosInstance({
    url: `/api/roles/${roleId}/permissions`,
    method: 'get'
  });
};

// 更新角色权限
export const updateRolePermissions = (roleId, permissionIds) => {
  return axiosInstance({
    url: `/api/roles/${roleId}/permissions`,
    method: 'put',
    data: { permissionIds }
  });
};

// 获取所有权限列�?
export const getPermissions = () => {
  return axiosInstance({
    url: '/api/permissions',
    method: 'get'
  });
};

// 获取权限详情
export const getPermissionById = (permissionId) => {
  return axiosInstance({
    url: `/api/permissions/${permissionId}`,
    method: 'get'
  });
};

// 创建权限
export const createPermission = (data) => {
  return axiosInstance({
    url: '/api/permissions',
    method: 'post',
    data
  });
};

// 更新权限
export const updatePermission = (permissionId, data) => {
  return axiosInstance({
    url: `/api/permissions/${permissionId}`,
    method: 'put',
    data
  });
};

// 删除权限
export const deletePermission = (permissionId) => {
  return axiosInstance({
    url: `/api/permissions/${permissionId}`,
    method: 'delete'
  });
};

// 获取用户的角�?
export const getUserRoles = (userId) => {
  return axiosInstance({
    url: `/api/users/${userId}/roles`,
    method: 'get'
  });
};

// 分配角色给用�?
export const assignRolesToUser = (userId, roleIds) => {
  return axiosInstance({
    url: `/api/users/${userId}/roles`,
    method: 'post',
    data: { roleIds }
  });
};

// 移除用户的角�?
export const removeRolesFromUser = (userId, roleIds) => {
  return axiosInstance({
    url: `/api/users/${userId}/roles`,
    method: 'delete',
    data: { roleIds }
  });
};

// 检查用户是否有某个权限
export const checkUserPermission = (userId, permissionCode) => {
  return axiosInstance({
    url: `/api/users/${userId}/permissions/check`,
    method: 'get',
    params: { permissionCode }
  });
};

// 获取用户的所有权�?
export const getUserPermissions = (userId) => {
  return axiosInstance({
    url: `/api/users/${userId}/permissions`,
    method: 'get'
  });
};

export default {
  getRoles,
  getRoleById,
  createRole,
  updateRole,
  deleteRole,
  getRolePermissions,
  updateRolePermissions,
  getPermissions,
  getPermissionById,
  createPermission,
  updatePermission,
  deletePermission,
  getUserRoles,
  assignRolesToUser,
  removeRolesFromUser,
  checkUserPermission,
  getUserPermissions
};

