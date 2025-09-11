// File path: /Vue/src/api/modules/search.js
import { instance } from '../axiosInstance'; // Corrected import

export default {
  /**
   * Search for public groups.
   * @param {string} keyword - The search keyword.
   * @returns {Promise} Axios promise
   */
  searchGroups(keyword) {
    // 后端暂未实现，先调用占位路径，后续后端实现 /api/search/group
    return instance.get('/api/search/group', { params: { keyword } });
  },

  /**
   * Search for users (e.g., for inviting to a group).
   * @param {string} keyword - The search keyword.
   * @returns {Promise} Axios promise
   */
  searchUsers(keyword) {
    // 与消息搜索保持一致的 /api 前缀
    return instance.get('/api/search/users', { params: { keyword } });
  }
};
