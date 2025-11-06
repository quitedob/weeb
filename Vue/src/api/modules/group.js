/**
 * 群组相关API
 */
import axiosInstance from '../axiosInstance';

export default {
  /**
   * 获取群组成员列表
   * @param {number} groupId - 群组ID
   * @returns {Promise}
   */
  getMembers(groupId) {
    return axiosInstance.get(`/api/groups/${groupId}/members`);
  },

  /**
   * 获取群组信息
   * @param {number} groupId - 群组ID
   * @returns {Promise}
   */
  getGroupInfo(groupId) {
    return axiosInstance.get(`/api/groups/${groupId}`);
  },

  /**
   * 获取群组详细信息（别名）
   * @param {number} groupId - 群组ID
   * @returns {Promise}
   */
  getGroupDetails(groupId) {
    return this.getGroupInfo(groupId);
  },

  /**
   * 创建群组
   * @param {Object} data - 群组数据
   * @returns {Promise}
   */
  createGroup(data) {
    return axiosInstance.post('/api/groups', data);
  },

  /**
   * 邀请成员加入群组
   * @param {number} groupId - 群组ID
   * @param {Array} userIds - 用户ID列表
   * @returns {Promise}
   */
  inviteMembers(groupId, userIds) {
    return axiosInstance.post(`/api/groups/${groupId}/members`, { userIds });
  },

  /**
   * 移除群成员
   * @param {number} groupId - 群组ID
   * @param {number} userId - 用户ID
   * @returns {Promise}
   */
  removeMember(groupId, userId) {
    return axiosInstance.delete(`/api/groups/${groupId}/members/${userId}`);
  },

  /**
   * 退出群组
   * @param {number} groupId - 群组ID
   * @returns {Promise}
   */
  leaveGroup(groupId) {
    return axiosInstance.post(`/api/groups/${groupId}/leave`);
  },

  /**
   * 更新群组信息
   * @param {number} groupId - 群组ID
   * @param {Object} data - 更新数据
   * @returns {Promise}
   */
  updateGroup(groupId, data) {
    return axiosInstance.put(`/api/groups/${groupId}`, data);
  },

  /**
   * 解散群组
   * @param {number} groupId - 群组ID
   * @returns {Promise}
   */
  dissolveGroup(groupId) {
    return axiosInstance.delete(`/api/groups/${groupId}`);
  },

  /**
   * 获取用户加入的群组列表
   * @returns {Promise}
   */
  getMyGroups() {
    return axiosInstance.get('/api/groups/my');
  },

  /**
   * 搜索群组
   * @param {string} keyword - 搜索关键词
   * @returns {Promise}
   */
  searchGroups(keyword) {
    return axiosInstance.get('/api/groups/search', { params: { keyword } });
  }
};
