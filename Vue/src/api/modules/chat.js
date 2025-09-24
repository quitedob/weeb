// /Vue/src/api/modules/chat.js
// 简短描述：聊天相关后端接口封装

import { instance } from '../axiosInstance';

// 获取会话列表
export function getChatList(params) {
  return instance.get('/api/chats', { params });
}

// 创建新会话
export function createChat(data) {
  return instance.post('/api/chats', data);
}

// 获取会话消息
export function getChatMessages(chatId, params) {
  return instance.get(`/api/chats/${chatId}/messages`, { params });
}

// 发送消息
export function sendMessage(chatId, data) {
  return instance.post(`/api/chats/${chatId}/messages`, data);
}

export default {
  getChatList,
  createChat,
  getChatMessages,
  sendMessage
};

