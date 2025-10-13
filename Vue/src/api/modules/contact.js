import axiosInstance from '../axiosInstance';

export default {
  // 获取联系人列表
  getContacts(status) {
    return axiosInstance.get('/api/contact/list', {
      params: { status }
    });
  },

  // 获取待处理的好友申请
  getPendingApplications() {
    return axiosInstance.get('/api/contact/list/pending');
  },

  // 申请添加联系人
  applyContact(data) {
    return axiosInstance.post('/api/contact/apply', data);
  },

  // 接受好友申请
  acceptContact(contactId) {
    return axiosInstance.post(`/api/contact/accept/${contactId}`);
  },

  // 拒绝好友申请
  declineContact(contactId) {
    return axiosInstance.post(`/api/contact/decline/${contactId}`);
  },

  // 屏蔽/删除联系人
  blockContact(contactId) {
    return axiosInstance.post(`/api/contact/block/${contactId}`);
  }
};