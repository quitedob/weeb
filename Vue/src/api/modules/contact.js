// File path: /Vue/src/api/modules/contact.js
import axiosInstance from '../axiosInstance';

/**
 * 联系人（好友）管理API模块
 * 对应后端 ContactController (/api/contacts)
 */

export default {
  /**
   * 获取联系人列表
   * GET /api/contacts
   * @param {String} status - 关系状态 (ACCEPTED, PENDING, BLOCKED, REJECTED)
   */
  getContacts(status) {
    return axiosInstance.get('/api/contacts', {
      params: { status }
    });
  },

  /**
   * 获取待处理的好友申请列表
   * GET /api/contacts/requests
   */
  getPendingApplications() {
    return axiosInstance.get('/api/contacts/requests');
  },

  /**
   * 发送好友申请
   * POST /api/contacts/apply
   * @param {Object} data - { friendId: Long, remarks: String }
   */
  applyContact(data) {
    return axiosInstance.post('/api/contacts/apply', data);
  },

  /**
   * 同意好友申请
   * POST /api/contacts/accept/{contactId}
   * @param {Number} contactId - 申请记录ID
   */
  acceptContact(contactId) {
    return axiosInstance.post(`/api/contacts/accept/${contactId}`);
  },

  /**
   * 拒绝好友申请
   * POST /api/contacts/decline/{contactId}
   * @param {Number} contactId - 申请记录ID
   */
  declineContact(contactId) {
    return axiosInstance.post(`/api/contacts/decline/${contactId}`);
  },

  /**
   * 拉黑联系人
   * POST /api/contacts/block/{contactId}
   * @param {Number} contactId - 关系记录ID
   */
  blockContact(contactId) {
    return axiosInstance.post(`/api/contacts/block/${contactId}`);
  }
};