import axiosInstance from '../axiosInstance';

/**
 * 消息API模块
 * 与后端MessageController接口对齐
 */

// 发送消息 - 使用正确的API路径
export function sendMessage(messageData) {
  return axiosInstance.post('/api/message/send', messageData);
}

// 获取聊天记录 - 使用正确的API路径和参数格式
export function getChatRecord(targetId, index = 0, num = 20) {
  return axiosInstance.get('/api/message/record', {
    params: { targetId, index, num }
  });
}

// 获取聊天历史 - 使用正确的API路径和参数格式
export function getChatHistoryApi(params) {
  const { recipientId, recipientType, page = 1, pageSize = 20 } = params;
  
  // 计算index (从0开始的偏移量)
  const index = (page - 1) * pageSize;
  
  return axiosInstance.get('/api/message/record', {
    params: { 
      targetId: recipientId, 
      index: index, 
      num: pageSize 
    }
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

// 创建新的聊天会话 - 暂时使用现有接口
export function createChat(targetId) {
  return Promise.resolve({ code: 0, data: { id: targetId } });
}

// 标记消息为已读 - 暂时返回成功
export function markAsRead(chatId) {
  return Promise.resolve({ code: 0, message: 'success' });
}

// 删除聊天会话 - 暂时返回成功
export function deleteChat(chatId) {
  return Promise.resolve({ code: 0, message: 'success' });
}

// 对消息添加反应 - 暂时返回成功
export function addReaction(messageId, reactionType) {
  return Promise.resolve({ code: 0, message: 'success' });
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