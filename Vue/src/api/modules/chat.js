// File path: /Vue/src/api/modules/chat.js
import axiosInstance from '../axiosInstance';

/**
 * 聊天相关的API模块
 */

// 获取聊天列表 - 修复API路径
const getChatList = () => {
  return axiosInstance.get('/api/v1/chats');
};

// 获取聊天消息记录 - 修复API路径
const getChatMessages = (chatId, params = {}) => {
  const { page = 1, size = 20 } = params;
  return axiosInstance.get(`/api/v1/chats/${chatId}/messages`, {
    params: { page, size }
  });
};

// 发送消息 - 修复API路径
const sendMessage = (chatId, messageData) => {
  const payload = {
    content: messageData.content,
    messageType: messageData.messageType || 0
  };
  return axiosInstance.post(`/api/v1/chats/${chatId}/messages`, payload);
};

// 创建新的聊天会话
const createChat = (targetId) => {
  return axiosInstance.post('/api/v1/chats', { targetId });
};

// 标记消息为已读
const markAsRead = (chatId) => {
  return axiosInstance.post(`/api/v1/chats/${chatId}/read`);
};

// 删除聊天会话
const deleteChat = (chatId) => {
  return axiosInstance.delete(`/api/v1/chats/${chatId}`);
};

// 对消息添加反应
const addReaction = (messageId, reactionType) => {
  return axiosInstance.post(`/api/v1/chats/messages/${messageId}/react`, { reactionType });
};

export default {
  getChatList,
  getChatMessages,
  sendMessage,
  createChat,
  markAsRead,
  deleteChat,
  addReaction
};
