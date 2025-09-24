import axiosInstance from '../axiosInstance';

const commentApi = {
  // 创建评论
  createComment(commentData) {
    return axiosInstance.post('/api/comments', commentData);
  },

  // 删除评论
  deleteComment(commentId) {
    return axiosInstance.delete(`/api/comments/${commentId}`);
  },

  // 获取文章评论列表
  getComments(articleId, page = 1, size = 10) {
    return axiosInstance.get(`/api/comments/article/${articleId}`, {
      params: { page, size }
    });
  }
};

export default commentApi;