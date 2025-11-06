// File path: /Vue/src/api/modules/chat.js
import axiosInstance from '../axiosInstance';

/**
 * 聊天相关的API模块
 * 对应后端 ChatController (/api/chats)
 */

/**
 * 获取用户的聊天列表
 * GET /api/chats
 */
const getChatList = () => {
  return axiosInstance.get('/api/chats');
};

/**
 * 创建新的聊天会话
 * POST /api/chats
 * @param {Object} data - { targetId: Long }
 */
const createChat = (data) => {
  return axiosInstance.post('/api/chats', data);
};

/**
 * 获取聊天消息历史记录
 * GET /api/chats/{chatId}/messages
 * @param {Number} chatId - 聊天ID
 * @param {Object} params - { page: int, size: int }
 */
const getChatMessages = (chatId, params = {}) => {
  const { page = 1, size = 20 } = params;
  return axiosInstance.get(`/api/chats/${chatId}/messages`, {
    params: { page, size }
  });
};

/**
 * 发送聊天消息
 * POST /api/chats/{chatId}/messages
 * @param {Number} chatId - 聊天ID
 * @param {Object} data - { content: String, messageType: Integer }
 */
const sendMessage = (chatId, data) => {
  return axiosInstance.post(`/api/chats/${chatId}/messages`, data);
};

/**
 * 标记消息为已读
 * POST /api/chats/{chatId}/read
 * @param {Number} chatId - 聊天ID
 */
const markAsRead = (chatId) => {
  return axiosInstance.post(`/api/chats/${chatId}/read`);
};

/**
 * 删除聊天会话
 * DELETE /api/chats/{chatId}
 * @param {Number} chatId - 聊天ID
 */
const deleteChat = (chatId) => {
  return axiosInstance.delete(`/api/chats/${chatId}`);
};

/**
 * 对消息添加反应
 * POST /api/chats/messages/{messageId}/react
 * @param {Number} messageId - 消息ID
 * @param {String} reactionType - 反应类型（如👍、❤️等）
 */
const addReaction = (messageId, reactionType) => {
  return axiosInstance.post(`/api/chats/messages/${messageId}/react`, null, {
    params: { reactionType }
  });
};

// ==================== 未读计数相关API ====================

/**
 * 获取未读消息统计
 * GET /api/chats/unread/stats
 * @returns {Promise} { totalUnread: int, unreadList: Array }
 */
const getUnreadStats = () => {
  return axiosInstance.get('/api/chats/unread/stats');
};

/**
 * 获取单个聊天的未读数
 * GET /api/chats/{chatId}/unread
 * @param {Number} chatId - 聊天ID
 * @returns {Promise} unreadCount: int
 */
const getUnreadCount = (chatId) => {
  return axiosInstance.get(`/api/chats/${chatId}/unread`);
};

/**
 * 批量标记已读
 * POST /api/chats/read/batch
 * @param {Array} chatIds - 聊天ID列表
 * @returns {Promise}
 */
const batchMarkAsRead = (chatIds) => {
  return axiosInstance.post('/api/chats/read/batch', chatIds);
};

/**
 * 获取群组未读数（优化版）
 * GET /api/chats/groups/{groupId}/unread
 * @param {Number} groupId - 群组ID
 * @returns {Promise} unreadCount: int
 */
const getGroupUnreadCount = (groupId) => {
  return axiosInstance.get(`/api/chats/groups/${groupId}/unread`);
};

export default {
  getChatList,
  createChat,
  getChatMessages,
  sendMessage,
  markAsRead,
  deleteChat,
  addReaction,
  // 未读计数相关
  getUnreadStats,
  getUnreadCount,
  batchMarkAsRead,
  getGroupUnreadCount
};
