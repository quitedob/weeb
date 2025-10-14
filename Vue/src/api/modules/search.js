// File path: /Vue/src/api/modules/search.js
import axiosInstance from '../axiosInstance';

export default {
  /**
   * Search for public groups.
   * @param {string} keyword - The search keyword.
   * @param {number} page - Page number (default: 0)
   * @param {number} size - Page size (default: 10)
   * @returns {Promise} Axios promise
   */
  searchGroups(keyword, page = 0, size = 10) {
    return axiosInstance.get('/api/search/group', {
      params: { keyword, page, size }
    });
  },

  /**
   * Search for users.
   * @param {string} keyword - The search keyword.
   * @param {number} page - Page number (default: 0)
   * @param {number} size - Page size (default: 10)
   * @returns {Promise} Axios promise
   */
  searchUsers(keyword, page = 0, size = 10) {
    return axiosInstance.get('/api/search/users', {
      params: { keyword, page, size }
    });
  },

  /**
   * Search for messages.
   * @param {string} q - The search query.
   * @param {number} page - Page number (default: 0)
   * @param {number} size - Page size (default: 10)
   * @returns {Promise} Axios promise
   */
  searchMessages(q, page = 0, size = 10) {
    return axiosInstance.get('/api/search/messages', {
      params: { q, page, size }
    });
  },

  /**
   * Search for articles.
   * @param {string} query - The search query.
   * @param {number} page - Page number (default: 1)
   * @param {number} pageSize - Page size (default: 10)
   * @param {string} sortBy - Sort field (default: 'created_at')
   * @param {string} sortOrder - Sort order (default: 'desc')
   * @returns {Promise} Axios promise
   */
  searchArticles(query, page = 1, pageSize = 10, sortBy = 'created_at', sortOrder = 'desc') {
    return axiosInstance.get('/api/search/articles', {
      params: { query, page, pageSize, sortBy, sortOrder }
    });
  },

  /**
   * Comprehensive search - search all types at once.
   * @param {string} q - The search query.
   * @param {number} page - Page number (default: 0)
   * @param {number} size - Page size (default: 5)
   * @returns {Promise} Axios promise
   */
  searchAll(q, page = 0, size = 5) {
    return axiosInstance.get('/api/search/all', {
      params: { q, page, size }
    });
  }
};
