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
   * @param {Object} data - { groupId, memberIds }
   * @returns {Promise}
   */
  inviteMembers(data) {
    const { groupId, memberIds } = data;
    return axiosInstance.post(`/api/groups/${groupId}/members`, { userIds: memberIds });
  },

  /**
   * 移除群成员（踢出成员）
   * @param {Object} data - { groupId, userIdToKick }
   * @returns {Promise}
   */
  kickMember(data) {
    const { groupId, userIdToKick } = data;
    return axiosInstance.delete(`/api/groups/${groupId}/members/${userIdToKick}`);
  },

  /**
   * 移除群成员（旧方法，保持兼容）
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
   * @param {Object} data - { id, groupName, description }
   * @returns {Promise}
   */
  updateGroup(data) {
    const { id, ...updateData } = data;
    return axiosInstance.put(`/api/groups/${id}`, updateData);
  },

  /**
   * 解散群组
   * @param {number} groupId - 群组ID
   * @returns {Promise}
   */
  disbandGroup(groupId) {
    return axiosInstance.delete(`/api/groups/${groupId}`);
  },

  /**
   * 解散群组（旧方法，保持兼容）
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
    return axiosInstance.get('/api/groups/my-groups');
  },

  /**
   * 获取用户创建的群组列表
   * @returns {Promise}
   */
  getMyCreatedGroups() {
    return axiosInstance.get('/api/groups/my-created');
  },

  /**
   * 搜索群组
   * @param {string} keyword - 搜索关键词
   * @returns {Promise}
   */
  searchGroups(keyword) {
    return axiosInstance.get('/api/groups/search', { params: { keyword } });
  },

  /**
   * 获取用户加入的群组列表（别名）
   * @returns {Promise}
   */
  getUserJoinedGroups() {
    return this.getMyGroups();
  },

  /**
   * 申请加入群组
   * @param {Object} data - 申请数据 { groupId, message? }
   * @returns {Promise}
   */
  applyToJoinGroup(data) {
    return axiosInstance.post(`/api/groups/${data.groupId}/applications`, data);
  },

  /**
   * 获取群组申请列表
   * @param {number} groupId - 群组ID
   * @param {string} status - 申请状态 ('pending', 'all')
   * @returns {Promise}
   */
  getGroupApplications(groupId, status = 'pending') {
    return axiosInstance.get(`/api/groups/${groupId}/applications`, {
      params: { status }
    });
  },

  /**
   * 批准群组申请
   * @param {number} groupId - 群组ID
   * @param {number} applicationId - 申请ID
   * @param {string} message - 批准消息
   * @returns {Promise}
   */
  approveApplication(groupId, applicationId, message) {
    return axiosInstance.post(`/api/groups/${groupId}/applications/${applicationId}/approve`, {
      message
    });
  },

  /**
   * 拒绝群组申请
   * @param {number} groupId - 群组ID
   * @param {number} applicationId - 申请ID
   * @param {string} message - 拒绝原因
   * @returns {Promise}
   */
  rejectApplication(groupId, applicationId, message) {
    return axiosInstance.post(`/api/groups/${groupId}/applications/${applicationId}/reject`, {
      message
    });
  }
};
