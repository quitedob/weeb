// File path: /Vue/src/api/modules/search.js
import { instance } from '../axiosInstance'; // Corrected import

export default {
  /**
   * Search for public groups.
   * @param {string} keyword - The search keyword.
   * @returns {Promise} Axios promise
   */
  searchGroups(keyword) {
    // Path matches original api/index.js: GET /api/search/group?keyword=...
    return instance.get('/search/group', { params: { keyword } });
  },

  /**
   * Search for users (e.g., for inviting to a group).
   * @param {string} keyword - The search keyword.
   * @returns {Promise} Axios promise
   */
  searchUsers(keyword) {
    // Assuming a similar endpoint for users: GET /api/search/users?keyword=...
    return instance.get('/search/users', { params: { keyword } });
  }
};
