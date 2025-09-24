import { instance } from '../axiosInstance';

export default {
  // 获取联系人列表
  getContacts(status) {
    return instance.get('/api/contact/list', {
      params: { status }
    });
  },

  // 获取待处理的好友申请
  getPendingApplications() {
    return instance.get('/api/contact/applications');
  },

  // 申请添加联系人
  applyContact(data) {
    return instance.post('/api/contact/apply', data);
  },

  // 接受好友申请
  acceptContact(contactId) {
    return instance.post(`/api/contact/accept/${contactId}`);
  },

  // 拒绝好友申请
  declineContact(contactId) {
    return instance.post(`/api/contact/decline/${contactId}`);
  },

  // 屏蔽/删除联系人
  blockContact(contactId) {
    return instance.post(`/api/contact/block/${contactId}`);
  }
};