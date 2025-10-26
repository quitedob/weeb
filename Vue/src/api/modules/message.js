import axiosInstance from '../axiosInstance';

/**
 * 消息API模块
 * 使用统一的聊天API路径 /api/chats
 */

// 发送消息 - 使用新的统一API路径
export function sendMessage(chatId, messageData) {
  return axiosInstance.post(`/api/chats/${chatId}/messages`, messageData);
}

// 获取聊天记录 - 使用新的RESTful API路径
export function getChatRecord(chatId, page = 1, pageSize = 20) {
  return axiosInstance.get(`/api/chats/${chatId}/messages`, {
    params: { page, size: pageSize }
  });
}

// 获取聊天历史 - 使用新的RESTful API路径
export function getChatHistoryApi(params) {
  const { recipientId: chatId, recipientType, page = 1, pageSize = 20 } = params;

  return axiosInstance.get(`/api/chats/${chatId}/messages`, {
    params: { page, size: pageSize }
  });
}

// 撤回消息 - 使用新的DELETE API
export function recallMessage(messageId) {
  return axiosInstance.delete(`/api/messages/${messageId}`);
}

// 对消息添加反应 - 使用新的统一API路径
export function handleReaction(messageId, reactionType) {
  return axiosInstance.post(`/api/chats/messages/${messageId}/react`, {
    reactionType
  });
}

// 创建新的聊天会话 - 使用统一的聊天API
export function createChat(targetId) {
  return axiosInstance.post('/api/chats', {
    targetId
  });
}

// 标记消息为已读 - 使用新的统一API路径
export function markAsRead(chatId) {
  return axiosInstance.post(`/api/chats/${chatId}/read`);
}

// 删除聊天会话 - 使用新的统一API路径
export function deleteChat(chatId) {
  return axiosInstance.delete(`/api/chats/${chatId}`);
}

// 对消息添加反应 - 使用新的统一API路径
export function addReaction(messageId, reactionType) {
  return axiosInstance.post(`/api/chats/messages/${messageId}/react`, {
    reactionType
  });
}

export default {
  sendMessage,
  getChatRecord,
  getChatHistoryApi,
  recallMessage,
  handleReaction,
  createChat,
  markAsRead,
  deleteChat,
  addReaction
};