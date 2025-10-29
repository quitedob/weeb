// File path: /Vue/src/api/modules/contact.js
import axiosInstance from '../axiosInstance';

/**
 * 联系人（好友）管理API模块
 * 对应后端 ContactController (/api/contacts)
 */

export default {
  /**
   * 获取联系人列表 - 返回 ContactDto 对象
   * GET /api/contacts
   * @param {String} status - 关系状态 (ACCEPTED, PENDING, BLOCKED, REJECTED)
   */
  getContacts(status = 'ACCEPTED') {
    return axiosInstance.get('/api/contacts', {
      params: { status }
    });
  },

  /**
   * 获取待处理的好友申请列表
   * GET /api/contacts/requests
   */
  getFriendRequests() {
    return axiosInstance.get('/api/contacts/requests');
  },

  /**
   * 获取待处理的好友申请列表 (别名)
   * GET /api/contacts/requests
   */
  getPendingApplications() {
    return axiosInstance.get('/api/contacts/requests');
  },

  /**
   * 通过用户名发送好友申请
   * POST /api/contacts/request/by-username
   * @param {String} username - 目标用户名
   * @param {String} message - 申请消息
   */
  sendRequestByUsername(username, message = '您好，我想添加您为好友') {
    return axiosInstance.post('/api/contacts/request/by-username', {
      username: username.trim(),
      message: message
    });
  },

  /**
   * 通过用户ID发送好友申请
   * POST /api/contacts/request
   * @param {Number} targetUserId - 目标用户ID
   * @param {String} message - 申请消息
   */
  sendRequest(targetUserId, message = '您好，我想添加您为好友') {
    return axiosInstance.post('/api/contacts/request', {
      targetUserId: targetUserId,
      message: message
    });
  },

  /**
   * 发送好友申请 (兼容旧版本)
   * POST /api/contacts/apply
   * @param {Object} data - { friendId: Long, remarks: String }
   */
  applyContact(data) {
    return axiosInstance.post('/api/contacts/apply', data);
  },

  /**
   * 接受好友申请
   * POST /api/contacts/request/{requestId}/accept
   * @param {Number} requestId - 申请记录ID
   */
  acceptRequest(requestId) {
    return axiosInstance.post(`/api/contacts/request/${requestId}/accept`);
  },

  /**
   * 同意好友申请 (别名)
   * POST /api/contacts/accept/{contactId}
   * @param {Number} contactId - 申请记录ID
   */
  acceptContact(contactId) {
    return axiosInstance.post(`/api/contacts/accept/${contactId}`);
  },

  /**
   * 拒绝好友申请
   * POST /api/contacts/request/{requestId}/reject
   * @param {Number} requestId - 申请记录ID
   */
  rejectRequest(requestId) {
    return axiosInstance.post(`/api/contacts/request/${requestId}/reject`);
  },

  /**
   * 拒绝好友申请 (别名)
   * POST /api/contacts/decline/{contactId}
   * @param {Number} contactId - 申请记录ID
   */
  declineContact(contactId) {
    return axiosInstance.post(`/api/contacts/decline/${contactId}`);
  },

  /**
   * 删除联系人
   * DELETE /api/contacts/{contactId}
   * @param {Number} contactId - 联系人记录ID
   */
  deleteContact(contactId) {
    return axiosInstance.delete(`/api/contacts/${contactId}`);
  },

  /**
   * 拉黑联系人
   * POST /api/contacts/block/{contactId}
   * @param {Number} contactId - 关系记录ID
   */
  blockContact(contactId) {
    return axiosInstance.post(`/api/contacts/block/${contactId}`);
  },

  /**
   * 解除拉黑联系人
   * POST /api/contacts/unblock/{contactId}
   * @param {Number} contactId - 关系记录ID
   */
  unblockContact(contactId) {
    return axiosInstance.post(`/api/contacts/unblock/${contactId}`);
  },

  // Contact Group Management Methods

  /**
   * 获取联系人分组列表
   * GET /api/contacts/groups
   */
  getContactGroups() {
    return axiosInstance.get('/api/contacts/groups');
  },

  /**
   * 创建联系人分组
   * POST /api/contacts/groups
   * @param {Object} data - { name: String, description?: String }
   */
  createContactGroup(data) {
    return axiosInstance.post('/api/contacts/groups', data);
  },

  /**
   * 更新联系人分组
   * PUT /api/contacts/groups/{groupId}
   * @param {Number} groupId - 分组ID
   * @param {Object} data - { name: String, description?: String }
   */
  updateContactGroup(groupId, data) {
    return axiosInstance.put(`/api/contacts/groups/${groupId}`, data);
  },

  /**
   * 删除联系人分组
   * DELETE /api/contacts/groups/{groupId}
   * @param {Number} groupId - 分组ID
   */
  deleteContactGroup(groupId) {
    return axiosInstance.delete(`/api/contacts/groups/${groupId}`);
  },

  /**
   * 将联系人添加到分组
   * POST /api/contacts/groups/{groupId}/members
   * @param {Number} groupId - 分组ID
   * @param {Array} contactIds - 联系人ID数组
   */
  addContactsToGroup(groupId, contactIds) {
    return axiosInstance.post(`/api/contacts/groups/${groupId}/members`, {
      contactIds: contactIds
    });
  },

  /**
   * 从分组中移除联系人
   * DELETE /api/contacts/groups/{groupId}/members/{contactId}
   * @param {Number} groupId - 分组ID
   * @param {Number} contactId - 联系人ID
   */
  removeContactFromGroup(groupId, contactId) {
    return axiosInstance.delete(`/api/contacts/groups/${groupId}/members/${contactId}`);
  },

  /**
   * 获取分组中的联系人列表
   * GET /api/contacts/groups/{groupId}/members
   * @param {Number} groupId - 分组ID
   */
  getContactsInGroup(groupId) {
    return axiosInstance.get(`/api/contacts/groups/${groupId}/members`);
  },

  /**
   * 搜索用户
   * GET /api/search/users
   * @param {String} query - 搜索关键词
   */
  searchUsers(query) {
    return axiosInstance.get('/api/search/users', {
      params: { keyword: query.trim() }
    });
  }
};