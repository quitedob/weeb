import axiosInstance from '../axiosInstance';

/**
 * 消息API模块
 * 与后端MessageController接口对齐
 */

// 发送消息
export function sendMessage(messageData) {
  return axiosInstance.post('/api/v1/message/send', messageData);
}

// 获取聊天记录
export function getChatRecord(targetId, index = 0, num = 50) {
  return axiosInstance.post('/api/v1/message/record', {
    targetId,
    index,
    num
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

export default {
  sendMessage,
  getChatRecord,
  recallMessage,
  handleReaction
};