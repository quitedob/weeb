import axiosInstance from '../axiosInstance';

const followApi = {
  // 关注用户
  followUser(userId) {
    return axiosInstance.post(`/api/follow/${userId}`);
  },

  // 取消关注
  unfollowUser(userId) {
    return axiosInstance.delete(`/api/follow/${userId}`);
  },

  // 检查是否已关注
  checkFollowing(userId) {
    return axiosInstance.get(`/api/follow/check/${userId}`);
  },

  // 获取关注列表
  getFollowingList(page = 1, size = 10) {
    return axiosInstance.get('/api/follow/following', {
      params: { page, size }
    });
  },

  // 获取粉丝列表
  getFollowersList(page = 1, size = 10) {
    return axiosInstance.get('/api/follow/followers', {
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