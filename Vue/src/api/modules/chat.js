// File path: /Vue/src/api/modules/chat.js
import axiosInstance from '../axiosInstance';
import messageApi from './message';

/**
 * 聊天相关的API模块
 */

// 获取聊天列表 (复用获取好友列表的接口)
const getChatList = () => {
  // 假设聊天列表就是好友列表
  return axiosInstance.get('/api/contacts', {
    params: {
      status: 'ACCEPTED'
    }
  });
};

// 获取聊天消息记录 (复用message模块的getChatRecord)
const getChatMessages = (chatId, index = 0, num = 50) => {
  return messageApi.getChatRecord(chatId, index, num);
};

// 发送消息 (复用message模块的sendMessage)
const sendMessage = (chatId, messageData) => {
  // chatId 可能是私聊用户ID或群聊ID
  const payload = {
    targetId: chatId,
    ...messageData
  };
  return messageApi.sendMessage(payload);
};


export default {
  getChatList,
  getChatMessages,
  sendMessage
};
