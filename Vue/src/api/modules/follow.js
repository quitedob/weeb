import axiosInstance from '../axiosInstance';

const followApi = {
  // 关注用户 - 统一使用 /api/users/{userId}/follow 路径
  followUser(userId) {
    return axiosInstance.post(`/api/users/${userId}/follow`);
  },

  // 取消关注 - 统一使用 /api/users/{userId}/follow 路径
  unfollowUser(userId) {
    return axiosInstance.delete(`/api/users/${userId}/follow`);
  },

  // 检查是否已关注
  checkFollowing(userId) {
    return axiosInstance.get(`/api/users/${userId}/follow/status`);
  },

  // 获取关注列表
  getFollowingList(userId, page = 1, size = 10) {
    return axiosInstance.get(`/api/users/${userId}/following`, {
      params: { page, size }
    });
  },

  // 获取粉丝列表
  getFollowersList(userId, page = 1, size = 10) {
    return axiosInstance.get(`/api/users/${userId}/followers`, {
      params: { page, size }
    });
  },

  // 获取关注统计
  getFollowStats() {
    return axiosInstance.get('/api/follow/stats');
  },

  // 获取指定用户的关注统计
  getUserFollowStats(userId) {
    return axiosInstance.get(`/api/follow/stats/${userId}`);
  }
};

export default followApi;