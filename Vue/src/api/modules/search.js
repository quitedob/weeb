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
  }
};
