// File path: /Vue/src/api/modules/ai.js

import axiosInstance from '../axiosInstance';

/**
 * AI 服务 API
 * 负责所有与 AI 模型的交互
 */

// ================== 文章处理 ==================

/**
 * 生成文章摘要
 * @param {string} content - 文章内容
 * @param {number} maxLength - 摘要最大长度（可选）
 */
export const summarizeArticle = (content, maxLength) => {
  return axiosInstance.post('/api/ai/article/summary', {
    content,
    maxLength
  });
};

/**
 * 生成文章标题建议
 * @param {string} content - 文章内容
 * @param {number} count - 生成数量，默认5
 */
export const generateTitles = (content, count = 5) => {
  return axiosInstance.post('/api/ai/article/titles', {
    content,
    count
  });
};

/**
 * 生成文章标签
 * @param {string} content - 文章内容
 * @param {number} count - 生成数量，默认5
 */
export const generateTags = (content, count = 5) => {
  return axiosInstance.post('/api/ai/article/tags', {
    content,
    count
  });
};

// ================== 文本处理 ==================

/**
 * 通用 AI 聊天接口
 * @param {object} payload - 请求数据
 * @param {Array<object>} payload.messages - 对话历史
 * @param {boolean} [payload.stream=false] - 是否使用流式响应
 */
export const chat = (payload) => {
  return axiosInstance.post('/api/ai/chat', payload);
};

/**
 * 文本润色接口
 * @param {object} payload - 请求数据
 * @param {string} payload.content - 需要润色的文本
 * @param {string} [payload.tone='professional'] - 润色风格
 */
export const refineText = (payload) => {
  return axiosInstance.post('/api/ai/text/refine', payload);
};

/**
 * 翻译文本
 * @param {string} content - 原文内容
 * @param {string} targetLanguage - 目标语言
 */
export const translateText = (content, targetLanguage) => {
  return axiosInstance.post('/api/ai/text/translate', {
    content,
    targetLanguage
  });
};

/**
 * 校对和修正文本
 * @param {string} content - 待校对文本
 */
export const proofreadText = (content) => {
  return axiosInstance.post('/api/ai/text/proofread', { content });
};

// ================== AI对话 ==================

/**
 * 生成回复建议
 * @param {string} originalMessage - 原始消息
 * @param {string} context - 上下文（可选）
 */
export const generateReplySuggestions = (originalMessage, context) => {
  return axiosInstance.post('/api/ai/reply/suggestions', {
    originalMessage,
    context
  });
};

/**
 * 总结对话历史
 * @param {Array<object>} messages - 消息历史
 * @param {number} maxLength - 摘要最大长度，默认200
 */
export const summarizeConversation = (messages, maxLength = 200) => {
  return axiosInstance.post('/api/ai/conversation/summary', {
    messages,
    maxLength
  });
};

// ================== 内容分析 ==================

/**
 * 分析内容情感
 * @param {string} content - 分析内容
 */
export const analyzeSentiment = (content) => {
  return axiosInstance.post('/api/ai/sentiment/analyze', { content });
};

/**
 * 提取关键词
 * @param {string} content - 提取内容
 * @param {number} count - 关键词数量，默认10
 */
export const extractKeywords = (content, count = 10) => {
  return axiosInstance.post('/api/ai/keywords/extract', {
    content,
    count
  });
};

/**
 * 检查内容合规性
 * @param {string} content - 检查内容
 */
export const checkContentCompliance = (content) => {
  return axiosInstance.post('/api/ai/content/compliance', { content });
};

// ================== 内容创作 ==================

/**
 * 生成内容创作建议
 * @param {string} topic - 主题
 * @param {string} contentType - 内容类型，默认"article"
 */
export const generateContentSuggestions = (topic, contentType = 'article') => {
  return axiosInstance.post('/api/ai/content/suggestions', {
    topic,
    contentType
  });
};

/**
 * 生成内容大纲
 * @param {string} topic - 主题
 * @param {string} structure - 结构，默认"introduction-body-conclusion"
 */
export const generateContentOutline = (topic, structure = 'introduction-body-conclusion') => {
  return axiosInstance.post('/api/ai/content/outline', {
    topic,
    structure
  });
};

// ================== 配置管理 ==================

/**
 * 获取AI配置信息
 */
export const getAIConfig = () => {
  return axiosInstance.get('/api/ai/config');
};

// 导出所有API方法
export default {
  // 文章处理
  summarizeArticle,
  generateTitles,
  generateTags,

  // 文本处理
  chat,
  refineText,
  translateText,
  proofreadText,

  // AI对话
  generateReplySuggestions,
  summarizeConversation,

  // 内容分析
  analyzeSentiment,
  extractKeywords,
  checkContentCompliance,

  // 内容创作
  generateContentSuggestions,
  generateContentOutline,

  // 配置管理
  getAIConfig
};
