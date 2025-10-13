import axiosInstance from '../axiosInstance';

/**
 * 聊天API模块
 * 与后端ChatController接口对齐
 */

// 获取会话列表
export function getChatList() {
  return axiosInstance.get('/api/v1/chats');
}

// 创建新会话
export function createChat(targetId) {
  return axiosInstance.post('/api/v1/chats', { targetId });
}

// 获取会话消息
export function getChatMessages(chatId, page = 1, size = 20) {
  return axiosInstance.get(`/api/v1/chats/${chatId}/messages`, {
    params: { page, size }
  });
}

// 发送消息
export function sendMessage(chatId, messageData) {
  return axiosInstance.post(`/api/v1/chats/${chatId}/messages`, messageData);
}

// 标记消息为已读
export function markAsRead(chatId) {
  return axiosInstance.post(`/api/v1/chats/${chatId}/read`);
}

// 删除聊天会话
export function deleteChat(chatId) {
  return axiosInstance.delete(`/api/v1/chats/${chatId}`);
}

// 对消息添加反应
export function addReaction(messageId, reactionType) {
  return axiosInstance.post(`/api/v1/chats/messages/${messageId}/react`, null, {
    params: { reactionType }
  });
}

export default {
  getChatList,
  createChat,
  getChatMessages,
  sendMessage,
  markAsRead,
  deleteChat,
  addReaction
};

