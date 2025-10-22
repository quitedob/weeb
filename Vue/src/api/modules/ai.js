// File path: /Vue/src/api/modules/ai.js

import axiosInstance from '../axiosInstance';

/**
 * AI 服务 API
 * 负责所有与 AI 模型的交互
 */

/**
 * 通用 AI 聊天接口
 * @param {object} payload - 请求数据
 * @param {Array<object>} payload.messages - 对话历史
 * @param {boolean} [payload.stream=false] - 是否使用流式响应
 * @returns {Promise}
 */
export const chat = (payload) => {
    return axiosInstance.post('/api/ai/chat', payload);
};

/**
 * 文本润色接口
 * @param {object} payload - 请求数据
 * @param {string} payload.content - 需要润色的文本
 * @param {string} [payload.tone='professional'] - 润色风格 (e.g., 'professional', 'casual')
 * @returns {Promise}
 */
export const refineText = (payload) => {
    return axiosInstance.post('/api/ai/text/refine', payload);
};

// 导出所有API方法
export default {
    chat,
    refineText
};
