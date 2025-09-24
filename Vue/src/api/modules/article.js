// File path: /Vue/src/api/modules/article.js

// Import the named 'instance' from axiosInstance.js and alias it to 'request'
// to match the user's provided code structure (request.post, request.get etc.)
import { instance as request } from '../axiosInstance';

/**
 * 文章服务 API
 * 负责所有与文章相关的操作
 */

// [修改] 获取所有文章列表（分页）
export const getAllArticles = (page, pageSize) => {
    return request.get('/articles/getall', { params: { page, pageSize } });
};

// 根据文章 ID 获取文章详情
export const getArticleById = (id) => {
    return request.get(`/articles/${id}`);
};

// 创建一篇新文章
export const createArticle = (articleData) => {
    return request.post('/articles/new', articleData);
};

// 根据文章 ID 更新文章
export const updateArticle = (id, articleData) => {
    return request.put(`/articles/${id}`, articleData);
};

// 根据文章 ID 删除文章
export const deleteArticle = (id) => {
    // 后端已改为 RESTful：DELETE /articles/{id}
    return request.delete(`/articles/${id}`);
};

// 根据用户 ID 获取该用户发布的所有文章
export const getArticlesByUserId = (userId) => {
    // 后端改为 GET /articles/myarticles?userId=
    return request.get('/articles/myarticles', { params: { userId } });
};

// 为文章点赞
export const likeArticle = (id) => {
    // Backend ArticleCenterController has:
    // @PostMapping("/{id}/like") public ApiResponse<?> likeArticle(@PathVariable String id)
    return request.post(`/articles/${id}/like`);
};

// 增加文章阅读量
export const increaseReadCount = (id) => {
    // Backend ArticleCenterController has:
    // @PostMapping("/{id}/read") public ApiResponse<?> increaseReadCount(@PathVariable String id)
    return request.post(`/articles/${id}/read`);
};

// 为文章赞助（打赏）
export const addCoinToArticle = (id, amount) => {
    // Backend ArticleCenterController has:
    // @PostMapping("/{id}/addcoin") public ApiResponse<?> addCoinToArticle(@PathVariable String id, @RequestParam int amount)
    return request.post(`/articles/${id}/addcoin?amount=${amount}`);
};

// 获取用户的统计信息（总点赞、收藏等）
// User's article.js had getUserInformation. Backend ArticleCenterController has:
// @GetMapping("/userinform") public ApiResponse<UserInformationVo> getUserInformation(@RequestParam String userId)
export const getUserInformation = (userId) => {
    return request.get(`/articles/userinform?userId=${userId}`);
};

// 根据用户名获取用户信息
// User's article.js had getUserInfoByUsername. Backend ArticleCenterController has:
// @GetMapping("/userinform-by-username") public ApiResponse<UserInformationVo> getUserInformationByUsername(@RequestParam String username)
export const getUserInfoByUsername = (username) => {
    return request.get(`/articles/userinform-by-username?username=${username}`);
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
    increaseReadCount,
    addCoinToArticle,
    getUserInformation,
    getUserInfoByUsername
};
