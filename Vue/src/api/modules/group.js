// File path: /Vue/src/api/modules/group.js
// 统一使用StandardGroupController的RESTful API端点
import axiosInstance from '../axiosInstance';

export default {
  // 获取用户加入的群组列表 - 对应 GET /api/groups
  getUserJoinedGroups() {
    return axiosInstance.get('/api/groups');
  },

  // 获取用户拥有的群组列表 - 对应 GET /api/groups/owned
  getUserOwnedGroups() {
    return axiosInstance.get('/api/groups/owned');
  },

  // 创建群组 - 对应 POST /api/groups
  createGroup(data) {
    return axiosInstance.post('/api/groups', data);
  },

  // 获取群组详情 - 对应 GET /api/groups/{groupId}
  getGroupDetails(groupId) {
    return axiosInstance.get(`/api/groups/${groupId}`);
  },

  // 获取群组成员列表 - 对应 GET /api/groups/{groupId}/members
  getGroupMembers(groupId) {
    return axiosInstance.get(`/api/groups/${groupId}/members`);
  },

  // 更新群组信息 - 对应 PUT /api/groups/{groupId}
  updateGroup(groupData) {
    return axiosInstance.put(`/api/groups/${groupData.id}`, groupData);
  },

  // 邀请成员加入群组 - 对应 POST /api/groups/{groupId}/invite
  inviteMembers(payload) { // payload: { groupId: string, userIds: string[] }
    return axiosInstance.post(`/api/groups/${payload.groupId}/invite`, payload);
  },

  // 移除群组成员 - 对应 DELETE /api/groups/{groupId}/members/{userId}
  kickMember(payload) { // payload: { groupId: string, userIdToKick: string }
    return axiosInstance.delete(`/api/groups/${payload.groupId}/members/${payload.userIdToKick}`);
  },

  // 退出群组 - 对应 POST /api/groups/{groupId}/leave
  leaveGroup(groupId) {
    return axiosInstance.post(`/api/groups/${groupId}/leave`);
  },

  // 解散群组 - 对应 DELETE /api/groups/{groupId}
  disbandGroup(groupId) {
    return axiosInstance.delete(`/api/groups/${groupId}`);
  },

  // 申请加入群组 - 对应 POST /api/groups/join
  applyToJoinGroup(data) {
    return axiosInstance.post('/api/groups/join', data);
  },

  // 搜索群组 - 对应 GET /api/groups/search?keyword=xxx
  searchGroups(keyword) {
    return axiosInstance.get('/api/groups/search', { params: { keyword } });
  },

  // 获取群组统计信息 - 对应 GET /api/groups/{groupId}/stats
  getGroupStats(groupId) {
    return axiosInstance.get(`/api/groups/${groupId}/stats`);
  },

  // 获取用户在群组中的权限 - 对应 GET /api/groups/{groupId}/permissions
  getUserGroupPermissions(groupId) {
    return axiosInstance.get(`/api/groups/${groupId}/permissions`);
  }
};
