import axiosInstance from '../axiosInstance';

/**
 * 消息API模块
 * 与后端MessageController接口对齐
 */

// 发送消息 - 修复API路径
export function sendMessage(messageData) {
  return axiosInstance.post('/api/v1/message/send', messageData);
}

// 获取聊天记录 - 修复API路径和参数格式
export function getChatRecord(chatId, page = 1, size = 20) {
  return axiosInstance.get(`/api/v1/chats/${chatId}/messages`, {
    params: { page, size }
  });
}

// 获取聊天历史 - 新增函数，用于NewChatWindow.vue
export function getChatHistoryApi(params) {
  const { recipientId, recipientType, page = 1, pageSize = 20 } = params;
  
  // 根据后端API文档，使用chatId而不是recipientId
  // 这里假设recipientId就是chatId
  return axiosInstance.get(`/api/v1/chats/${recipientId}/messages`, {
    params: { page, size: pageSize }
  });
}

// 撤回消息
export function recallMessage(msgId) {
  return axiosInstance.post('/api/v1/message/recall', {
    msgId
  });
}

// 对消息添加反应
export function handleReaction(reactionData) {
  return axiosInstance.post('/api/v1/message/react', reactionData);
}

// 创建新的聊天会话 - 新增函数
export function createChat(targetId) {
  return axiosInstance.post('/api/v1/chats', { targetId });
}

// 标记消息为已读 - 新增函数
export function markAsRead(chatId) {
  return axiosInstance.post(`/api/v1/chats/${chatId}/read`);
}

// 删除聊天会话 - 新增函数
export function deleteChat(chatId) {
  return axiosInstance.delete(`/api/v1/chats/${chatId}`);
}

// 对消息添加反应 - 新增函数，使用正确的API路径
export function addReaction(messageId, reactionType) {
  return axiosInstance.post(`/api/v1/chats/messages/${messageId}/react`, { reactionType });
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