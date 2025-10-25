// File path: /Vue/src/api/modules/search.js
import axiosInstance from '../axiosInstance';

export default {
  /**
   * Search for public groups.
   * @param {string} keyword - The search keyword.
   * @param {number} page - Page number (default: 0)
   * @param {number} size - Page size (default: 10)
   * @param {Object} filters - Additional filter parameters
   * @returns {Promise} Axios promise
   */
  searchGroups(keyword, page = 0, size = 10, filters = {}) {
    return axiosInstance.get('/api/search', {
      params: { q: keyword, type: 'group', page, size, ...filters }
    });
  },

  /**
   * Search for users.
   * @param {string} keyword - The search keyword.
   * @param {number} page - Page number (default: 0)
   * @param {number} size - Page size (default: 10)
   * @param {Object} filters - Additional filter parameters
   * @returns {Promise} Axios promise
   */
  searchUsers(keyword, page = 0, size = 10, filters = {}) {
    return axiosInstance.get('/api/search', {
      params: { q: keyword, type: 'user', page, size, ...filters }
    });
  },

  /**
   * Search for messages.
   * @param {string} q - The search query.
   * @param {number} page - Page number (default: 0)
   * @param {number} size - Page size (default: 10)
   * @param {Object} filters - Additional filter parameters
   * @returns {Promise} Axios promise
   */
  searchMessages(q, page = 0, size = 10, filters = {}) {
    return axiosInstance.get('/api/search/messages', {
      params: { q, page, size, ...filters }
    });
  },

  /**
   * Search for articles.
   * @param {string} query - The search query.
   * @param {number} page - Page number (default: 1)
   * @param {number} pageSize - Page size (default: 10)
   * @param {Object} filters - Additional filter parameters
   * @returns {Promise} Axios promise
   */
  searchArticles(query, page = 1, pageSize = 10, filters = {}) {
    return axiosInstance.get('/api/search', {
      params: { q: query, type: 'article', page, pageSize, ...filters }
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
    return axiosInstance.get('/api/search', {
      params: { q, page, size }
    });
  }
};
