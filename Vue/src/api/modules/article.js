// File path: /Vue/src/api/modules/article.js

import axiosInstance from '../axiosInstance';

/**
 * 文章服务 API
 * 负责所有与文章相关的操作
 */

// [修改] 获取所有文章列表（分页）
export const getAllArticles = (page, pageSize) => {
    return axiosInstance.get('/api/articles/getall', { params: { page, pageSize } });
};

// 根据文章 ID 获取文章详情
export const getArticleById = (id) => {
    return axiosInstance.get(`/api/articles/${id}`);
};

// 创建一篇新文章 - 使用ArticleCreateVo格式
export const createArticle = (articleData) => {
    // 确保数据格式符合ArticleCreateVo
    const createVo = {
        articleTitle: articleData.articleTitle,
        articleContent: articleData.articleContent,
        articleLink: articleData.articleLink || null,
        tags: articleData.tags || null,
        status: articleData.status || 1 // 默认为发布状态
    };
    return axiosInstance.post('/api/articles/new', createVo);
};

// 根据文章 ID 更新文章 - 使用ArticleUpdateVo格式
export const updateArticle = (id, articleData) => {
    // 确保数据格式符合ArticleUpdateVo
    const updateVo = {
        articleTitle: articleData.articleTitle,
        articleContent: articleData.articleContent,
        articleLink: articleData.articleLink || null,
        tags: articleData.tags || null,
        status: articleData.status
    };
    return axiosInstance.put(`/api/articles/${id}`, updateVo);
};

// 根据文章 ID 删除文章
export const deleteArticle = (id) => {
    // 后端已改为 RESTful：DELETE /articles/{id}
    return axiosInstance.delete(`/api/articles/${id}`);
};

// 根据用户 ID 获取该用户发布的所有文章
export const getArticlesByUserId = (userId) => {
    // 后端改为 GET /articles/myarticles?userId=
    return axiosInstance.get('/api/articles/myarticles', { params: { userId } });
};

// 为文章点赞
export const likeArticle = (id) => {
    // Backend ArticleCenterController has:
    // @PostMapping("/{id}/like") public ApiResponse<?> likeArticle(@PathVariable String id)
    return axiosInstance.post(`/api/articles/${id}/like`);
};

// 取消点赞文章
export const unlikeArticle = (id) => {
    return axiosInstance.delete(`/api/articles/${id}/like`);
};

// 检查点赞状态
export const checkLikeStatus = (articleId) => {
    return axiosInstance.get(`/api/articles/${articleId}/like/status`);
};

// 增加文章阅读量
export const increaseReadCount = (id) => {
    // Backend ArticleCenterController has:
    // @PostMapping("/{id}/read") public ApiResponse<?> increaseReadCount(@PathVariable String id)
    return axiosInstance.post(`/api/articles/${id}/read`);
};

// 为文章赞助（打赏）
export const addCoinToArticle = (id, amount) => {
    // Backend ArticleCenterController has:
    // @PostMapping("/{id}/addcoin") public ApiResponse<?> addCoinToArticle(@PathVariable String id, @RequestParam int amount)
    return axiosInstance.post(`/api/articles/${id}/addcoin?amount=${amount}`);
};

// 获取用户的统计信息（总点赞、收藏等）
// User's article.js had getUserInformation. Backend ArticleCenterController has:
// @GetMapping("/userinform") public ApiResponse<UserInformationVo> getUserInformation(@RequestParam String userId)
export const getUserInformation = (userId) => {
    return axiosInstance.get(`/api/articles/userinform?userId=${userId}`);
};

// 根据用户名获取用户信息
// User's article.js had getUserInfoByUsername. Backend ArticleCenterController has:
// @GetMapping("/userinform-by-username") public ApiResponse<UserInformationVo> getUserInformationByUsername(@RequestParam String username)
export const getUserInfoByUsername = (username) => {
    return axiosInstance.get(`/api/articles/userinform-by-username?username=${username}`);
};

// 获取文章分类列表
export const getCategories = () => {
    return axiosInstance.get('/api/articles/categories');
};

// 获取推荐文章列表 - 使用标准的文章列表接口
export const getRecommendedArticles = (page, pageSize) => {
    return axiosInstance.get('/api/articles/getall', { params: { page, pageSize } });
};

// 搜索文章
export const searchArticles = (params) => {
    // 参数包括: query, page, pageSize, sortBy, sortOrder
    return axiosInstance.get('/api/articles/search', { params });
};

// 高级搜索文章
export const searchArticlesAdvanced = (params) => {
    // 参数可以是: query, page, pageSize, startDate, endDate, categoryIds, authorId, status, minLikes, maxLikes, minExposure, maxExposure, sortBy, sortOrder
    return axiosInstance.get('/api/articles/search/advanced', { params });
};

// 收藏文章
export const favoriteArticle = (articleId) => {
    return axiosInstance.post(`/api/articles/${articleId}/favorite`);
};

// 取消收藏文章
export const unfavoriteArticle = (articleId) => {
    return axiosInstance.delete(`/api/articles/${articleId}/favorite`);
};

// 检查文章收藏状态
export const checkFavoriteStatus = (articleId) => {
    return axiosInstance.get(`/api/articles/${articleId}/favorite/status`);
};

// 获取用户收藏的文章列表
export const getUserFavoriteArticles = (page, pageSize) => {
    return axiosInstance.get('/api/articles/favorites', { params: { page, pageSize } });
};

// 获取文章评论列表
export const getArticleComments = (articleId) => {
    return axiosInstance.get(`/api/articles/${articleId}/comments`);
};

// 添加文章评论
export const addArticleComment = (articleId, commentData) => {
    return axiosInstance.post(`/api/articles/${articleId}/comments`, commentData);
};

// 删除文章评论
export const deleteArticleComment = (articleId, commentId) => {
    return axiosInstance.delete(`/api/articles/${articleId}/comments/${commentId}`);
};

// 获取文章评论总数
export const getArticleCommentCount = (articleId) => {
    return axiosInstance.get(`/api/articles/${articleId}/comments/count`);
};

// 默认导出所有API方法
export default {
    getAllArticles,
    getArticleById,
    createArticle,
    updateArticle,
    deleteArticle,
    getArticlesByUserId,
    likeArticle,
    unlikeArticle,  // 添加取消点赞API
    checkLikeStatus,  // 添加检查点赞状态API
    increaseReadCount,
    addCoinToArticle,
    getUserInformation,
    getUserInfoByUsername,
    getCategories,  // 添加获取分类API
    getRecommendedArticles,  // 添加获取推荐文章API
    searchArticles,  // 添加搜索文章API
    searchArticlesAdvanced,  // 添加高级搜索文章API
    favoriteArticle,  // 添加收藏文章API
    unfavoriteArticle,  // 添加取消收藏API
    checkFavoriteStatus,  // 添加检查收藏状态API
    getUserFavoriteArticles,  // 添加获取用户收藏文章API
    getArticleComments,  // 添加获取评论API
    addArticleComment,  // 添加评论API
    deleteArticleComment,  // 删除评论API
    getArticleCommentCount  // 获取评论总数API
};
