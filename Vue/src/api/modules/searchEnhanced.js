// File path: /Vue/src/api/modules/searchEnhanced.js
// 增强的搜索API模块，支持Redis缓存和Elasticsearch全文搜索

import axiosInstance from '../axiosInstance';

/**
 * 搜索相关API - 增强版
 * 支持Redis缓存和Elasticsearch全文搜索
 */

// ==================== 消息搜索 ====================

/**
 * 使用Elasticsearch搜索消息
 * @param {Object} params - 搜索参数 { keyword, fromUserId, chatListId, page, size }
 * @returns {Promise} 搜索结果
 */
export const searchMessagesWithES = (params) => {
  return axiosInstance.get('/api/search/messages/es', { params });
};

/**
 * 获取搜索建议（自动补全）
 * @param {string} prefix - 搜索前缀
 * @param {number} size - 建议数量
 * @returns {Promise} 建议列表
 */
export const getSearchSuggestions = (prefix, size = 10) => {
  return axiosInstance.get('/api/search/suggestions', {
    params: { prefix, size }
  });
};

/**
 * 高级搜索消息（支持复杂过滤条件）
 * @param {Object} searchParams - 搜索参数
 * @returns {Promise} 搜索结果
 */
export const advancedSearchMessages = (searchParams) => {
  return axiosInstance.post('/api/search/messages/advanced', searchParams);
};

/**
 * 获取搜索统计信息
 * @returns {Promise} 统计信息
 */
export const getSearchStatistics = () => {
  return axiosInstance.get('/api/search/statistics');
};

// ==================== 缓存相关API ====================

/**
 * 清除搜索缓存
 * @param {string} cacheType - 缓存类型 (messages, articles, users, groups)
 * @returns {Promise} 操作结果
 */
export const clearSearchCache = (cacheType) => {
  return axiosInstance.delete(`/api/search/cache/${cacheType}`);
};

/**
 * 预热搜索缓存
 * @param {string} cacheType - 缓存类型
 * @returns {Promise} 操作结果
 */
export const warmupSearchCache = (cacheType) => {
  return axiosInstance.post(`/api/search/cache/warmup/${cacheType}`);
};

/**
 * 获取缓存状态
 * @returns {Promise} 缓存状态信息
 */
export const getCacheStatus = () => {
  return axiosInstance.get('/api/search/cache/status');
};

// ==================== 增强的搜索功能 ====================

/**
 * 综合搜索（支持消息、用户、群组、文章）
 * @param {Object} params - 搜索参数 { keyword, type, page, size, filters }
 * @returns {Promise} 综合搜索结果
 */
export const comprehensiveSearch = (params) => {
  return axiosInstance.get('/api/search/comprehensive', { params });
};

/**
 * 搜索热门关键词
 * @param {number} limit - 限制数量
 * @returns {Promise} 热门关键词列表
 */
export const getHotKeywords = (limit = 20) => {
  return axiosInstance.get('/api/search/keywords/hot', { params: { limit } });
};

/**
 * 记录搜索历史
 * @param {string} keyword - 搜索关键词
 * @param {string} type - 搜索类型
 * @returns {Promise} 操作结果
 */
export const recordSearchHistory = (keyword, type) => {
  return axiosInstance.post('/api/search/history', { keyword, type });
};

/**
 * 获取搜索历史
 * @param {number} limit - 限制数量
 * @returns {Promise} 搜索历史列表
 */
export const getSearchHistory = (limit = 10) => {
  return axiosInstance.get('/api/search/history', { params: { limit } });
};

/**
 * 清除搜索历史
 * @returns {Promise} 操作结果
 */
export const clearSearchHistory = () => {
  return axiosInstance.delete('/api/search/history');
};

// ==================== 搜索设置 ====================

/**
 * 获取用户搜索偏好设置
 * @returns {Promise} 搜索偏好设置
 */
export const getSearchPreferences = () => {
  return axiosInstance.get('/api/search/preferences');
};

/**
 * 更新用户搜索偏好设置
 * @param {Object} preferences - 搜索偏好设置
 * @returns {Promise} 操作结果
 */
export const updateSearchPreferences = (preferences) => {
  return axiosInstance.put('/api/search/preferences', preferences);
};

/**
 * 保存搜索过滤器
 * @param {Object} filter - 搜索过滤器
 * @returns {Promise} 操作结果
 */
export const saveSearchFilter = (filter) => {
  return axiosInstance.post('/api/search/filters', filter);
};

/**
 * 获取保存的搜索过滤器列表
 * @returns {Promise} 搜索过滤器列表
 */
export const getSearchFilters = () => {
  return axiosInstance.get('/api/search/filters');
};

/**
 * 删除搜索过滤器
 * @param {string} filterId - 过滤器ID
 * @returns {Promise} 操作结果
 */
export const deleteSearchFilter = (filterId) => {
  return axiosInstance.delete(`/api/search/filters/${filterId}`);
};

// ==================== 实时搜索 ====================

/**
 * 创建实时搜索连接
 * @param {Function} onResult - 结果回调函数
 * @param {Function} onError - 错误回调函数
 * @returns {WebSocket} WebSocket连接
 */
export const createRealTimeSearch = (onResult, onError) => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}/api/search/realtime`;

  const ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    console.log('实时搜索连接已建立');
  };

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      onResult(data);
    } catch (error) {
      console.error('解析实时搜索结果失败:', error);
      onError(error);
    }
  };

  ws.onerror = (error) => {
    console.error('实时搜索连接错误:', error);
    onError(error);
  };

  ws.onclose = () => {
    console.log('实时搜索连接已关闭');
  };

  return ws;
};

/**
 * 发送实时搜索查询
 * @param {WebSocket} ws - WebSocket连接
 * @param {Object} query - 搜索查询
 */
export const sendRealTimeSearchQuery = (ws, query) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(query));
  }
};

/**
 * 关闭实时搜索连接
 * @param {WebSocket} ws - WebSocket连接
 */
export const closeRealTimeSearch = (ws) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.close();
  }
};

export default {
  // 消息搜索
  searchMessagesWithES,
  getSearchSuggestions,
  advancedSearchMessages,
  getSearchStatistics,

  // 缓存相关
  clearSearchCache,
  warmupSearchCache,
  getCacheStatus,

  // 增强搜索
  comprehensiveSearch,
  getHotKeywords,

  // 搜索历史
  recordSearchHistory,
  getSearchHistory,
  clearSearchHistory,

  // 搜索设置
  getSearchPreferences,
  updateSearchPreferences,
  saveSearchFilter,
  getSearchFilters,
  deleteSearchFilter,

  // 实时搜索
  createRealTimeSearch,
  sendRealTimeSearchQuery,
  closeRealTimeSearch
};