import axiosInstance from '../axiosInstance';

/**
 * 管理员相关API
 */

// 获取系统统计信息
export const getSystemStats = () => {
  return axiosInstance({
    url: '/api/admin/stats',
    method: 'get'
  });
};

// 获取用户列表（管理员�?
export const getUsers = (params) => {
  return axiosInstance({
    url: '/api/admin/users',
    method: 'get',
    params
  });
};

// 获取用户详情（管理员�?
export const getUserDetail = (userId) => {
  return axiosInstance({
    url: `/api/admin/users/${userId}`,
    method: 'get'
  });
};

// 更新用户状�?
export const updateUserStatus = (userId, status) => {
  return axiosInstance({
    url: `/api/admin/users/${userId}/status`,
    method: 'put',
    data: { status }
  });
};

// 封禁用户
export const banUser = (userId, reason, duration) => {
  return axiosInstance({
    url: `/api/admin/users/${userId}/ban`,
    method: 'post',
    data: { reason, duration }
  });
};

// 解封用户
export const unbanUser = (userId) => {
  return axiosInstance({
    url: `/api/admin/users/${userId}/unban`,
    method: 'post'
  });
};

// 删除用户
export const deleteUser = (userId) => {
  return axiosInstance({
    url: `/api/admin/users/${userId}`,
    method: 'delete'
  });
};

// 获取内容举报列表
export const getReports = (params) => {
  return axiosInstance({
    url: '/api/admin/reports',
    method: 'get',
    params
  });
};

// 处理举报
export const handleReport = (reportId, action, reason) => {
  return axiosInstance({
    url: `/api/admin/reports/${reportId}/handle`,
    method: 'post',
    data: { action, reason }
  });
};

// 获取系统日志
export const getSystemLogs = (params) => {
  return axiosInstance({
    url: '/api/admin/logs',
    method: 'get',
    params
  });
};

// 获取操作日志
export const getOperationLogs = (params) => {
  return axiosInstance({
    url: '/api/admin/operation-logs',
    method: 'get',
    params
  });
};

// 获取系统配置
export const getSystemConfig = () => {
  return axiosInstance({
    url: '/api/admin/config',
    method: 'get'
  });
};

// 更新系统配置
export const updateSystemConfig = (config) => {
  return axiosInstance({
    url: '/api/admin/config',
    method: 'put',
    data: config
  });
};

// 获取数据库统�?
export const getDatabaseStats = () => {
  return axiosInstance({
    url: '/api/admin/database/stats',
    method: 'get'
  });
};

// 清理缓存
export const clearCache = (cacheType) => {
  return axiosInstance({
    url: '/api/admin/cache/clear',
    method: 'post',
    data: { cacheType }
  });
};

// 获取在线用户列表
export const getOnlineUsers = () => {
  return axiosInstance({
    url: '/api/admin/online-users',
    method: 'get'
  });
};

// 强制用户下线
export const forceUserOffline = (userId) => {
  return axiosInstance({
    url: `/api/admin/users/${userId}/force-offline`,
    method: 'post'
  });
};

// 获取消息统计
export const getMessageStats = (params) => {
  return axiosInstance({
    url: '/api/admin/messages/stats',
    method: 'get',
    params
  });
};

// 获取文章统计
export const getArticleStats = (params) => {
  return axiosInstance({
    url: '/api/admin/articles/stats',
    method: 'get',
    params
  });
};

// 审核文章
export const reviewArticle = (articleId, action, reason) => {
  return axiosInstance({
    url: `/api/admin/articles/${articleId}/review`,
    method: 'post',
    data: { action, reason }
  });
};

// 删除文章
export const deleteArticle = (articleId) => {
  return axiosInstance({
    url: `/api/admin/articles/${articleId}`,
    method: 'delete'
  });
};

// 获取群组列表
export const getGroups = (params) => {
  return axiosInstance({
    url: '/api/admin/groups',
    method: 'get',
    params
  });
};

// 解散群组
export const dissolveGroup = (groupId, reason) => {
  return axiosInstance({
    url: `/api/admin/groups/${groupId}/dissolve`,
    method: 'post',
    data: { reason }
  });
};

// 获取敏感词列�?
export const getSensitiveWords = () => {
  return axiosInstance({
    url: '/api/admin/sensitive-words',
    method: 'get'
  });
};

// 添加敏感�?
export const addSensitiveWord = (word) => {
  return axiosInstance({
    url: '/api/admin/sensitive-words',
    method: 'post',
    data: { word }
  });
};

// 删除敏感�?
export const deleteSensitiveWord = (wordId) => {
  return axiosInstance({
    url: `/api/admin/sensitive-words/${wordId}`,
    method: 'delete'
  });
};

// 批量导入敏感�?
export const importSensitiveWords = (words) => {
  return axiosInstance({
    url: '/api/admin/sensitive-words/import',
    method: 'post',
    data: { words }
  });
};

// 导出数据
export const exportData = (dataType, params) => {
  return axiosInstance({
    url: `/api/admin/export/${dataType}`,
    method: 'get',
    params,
    responseType: 'blob'
  });
};

// 备份数据�?
export const backupDatabase = () => {
  return axiosInstance({
    url: '/api/admin/database/backup',
    method: 'post'
  });
};

// 获取备份列表
export const getBackupList = () => {
  return axiosInstance({
    url: '/api/admin/database/backups',
    method: 'get'
  });
};

// 恢复数据�?
export const restoreDatabase = (backupId) => {
  return axiosInstance({
    url: `/api/admin/database/restore/${backupId}`,
    method: 'post'
  });
};

// 发送系统通知
export const sendSystemNotification = (data) => {
  return axiosInstance({
    url: '/api/admin/notifications/send',
    method: 'post',
    data
  });
};

// 获取系统公告列表
export const getAnnouncements = () => {
  return axiosInstance({
    url: '/api/admin/announcements',
    method: 'get'
  });
};

// 创建系统公告
export const createAnnouncement = (data) => {
  return axiosInstance({
    url: '/api/admin/announcements',
    method: 'post',
    data
  });
};

// 更新系统公告
export const updateAnnouncement = (announcementId, data) => {
  return axiosInstance({
    url: `/api/admin/announcements/${announcementId}`,
    method: 'put',
    data
  });
};

// 删除系统公告
export const deleteAnnouncement = (announcementId) => {
  return axiosInstance({
    url: `/api/admin/announcements/${announcementId}`,
    method: 'delete'
  });
};

export default {
  getSystemStats,
  getUsers,
  getUserDetail,
  updateUserStatus,
  banUser,
  unbanUser,
  deleteUser,
  getReports,
  handleReport,
  getSystemLogs,
  getOperationLogs,
  getSystemConfig,
  updateSystemConfig,
  getDatabaseStats,
  clearCache,
  getOnlineUsers,
  forceUserOffline,
  getMessageStats,
  getArticleStats,
  reviewArticle,
  deleteArticle,
  getGroups,
  dissolveGroup,
  getSensitiveWords,
  addSensitiveWord,
  deleteSensitiveWord,
  importSensitiveWords,
  exportData,
  backupDatabase,
  getBackupList,
  restoreDatabase,
  sendSystemNotification,
  getAnnouncements,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement
};

