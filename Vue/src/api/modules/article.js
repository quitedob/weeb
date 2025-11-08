// File path: /Vue/src/api/modules/article.js

import axiosInstance from '../axiosInstance';
import { getUserById, getUserByUsername } from './user';  // 导入用户相关API

/**
 * 文章服务 API
 * 负责所有与文章相关的操作
 */

// 文章状态常量
export const ARTICLE_STATUS = {
    DRAFT: 0,           // 草稿
    PENDING_REVIEW: 1,  // 待审核
    PUBLISHED: 2,       // 已发布
    REJECTED: 3         // 已拒绝
};

// 文章状态标签映射
export const getStatusTag = (status) => {
    const statusMap = {
        0: { type: 'info', text: '草稿' },
        1: { type: 'warning', text: '待审核' },
        2: { type: 'success', text: '已发布' },
        3: { type: 'danger', text: '已拒绝' }
    };
    return statusMap[status] || { type: '', text: '未知状态' };
};

// [修改] 获取所有文章列表（分页）
export const getAllArticles = (page, pageSize) => {
    return axiosInstance.get('/api/articles/getall', { params: { page, pageSize } });
};

// 根据文章 ID 获取文章详情
export const getArticleById = (id) => {
    return axiosInstance.get(`/api/articles/${id}`);
};

// 根据文章 ID 获取文章详情（别名）
export const getArticle = (id) => {
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
// ❌ 已废弃 - 违规端点已删除
// 用户信息应通过 /api/users/* 端点获取
// 请使用 Vue/src/api/modules/user.js 中的方法：
// - getUserById(userId) 替代 getUserInformation(userId)
// - 暂无通过用户名获取的端点，请先通过搜索API获取userId

// 迁移示例：
// import { getUserById, getUserStats } from '@/api/modules/user';
// const response = await getUserById(userId);
// const { user, stats } = response.data;

// 获取文章分类列表
export const getCategories = () => {
    return axiosInstance.get('/api/articles/categories');
};

// 获取推荐文章列表 - 修复为正确的推荐接口
export const getRecommendedArticles = (page, pageSize) => {
    return axiosInstance.get('/api/articles/recommended', { params: { page, pageSize } });
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

// ==================== 文章审核相关API ====================

// 获取待审核文章列表
export const getPendingArticles = (page, pageSize, status, keyword) => {
    return axiosInstance.get('/api/articles/moderation/pending', {
        params: { page, pageSize, status, keyword }
    });
};

// 审核通过文章
export const approveArticle = (articleId) => {
    return axiosInstance.post(`/api/articles/${articleId}/approve`);
};

// 审核拒绝文章
export const rejectArticle = (articleId, reason) => {
    return axiosInstance.post(`/api/articles/${articleId}/reject`, null, {
        params: { reason }
    });
};

// 获取审核统计数据
export const getModerationStatistics = () => {
    return axiosInstance.get('/api/articles/moderation/statistics');
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
    getUserById,  // 使用正确的函数名
    getUserByUsername,  // 使用正确的函数名
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
    getArticleCommentCount,  // 获取评论总数API
    // 文章审核相关
    getPendingArticles,  // 获取待审核文章列表
    approveArticle,  // 审核通过文章
    rejectArticle,  // 审核拒绝文章
    getModerationStatistics  // 获取审核统计数据
};
