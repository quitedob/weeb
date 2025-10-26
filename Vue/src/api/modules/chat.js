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

export default {
  getChatList,
  createChat,
  getChatMessages,
  sendMessage,
  markAsRead,
  deleteChat,
  addReaction
};
