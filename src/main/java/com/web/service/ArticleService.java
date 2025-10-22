package com.web.service;

import com.web.model.Article;
import com.web.model.ArticleCategory;
import com.web.vo.article.ArticleSearchAdvancedVo;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing articles and related user interactions.
 */
public interface ArticleService {
    /**
     * 根据文章ID删除文章
     * @param id 要删除文章的 ID
     * @param authenticatedUserId 当前认证用户的ID
     * @return 如果删除成功返回 true，否则返回 false
     */
    boolean deleteArticle(Long id, Long authenticatedUserId);

    /**
     * 根据文章ID获取文章信息。
     *
     * @param id 文章的唯一标识符
     * @return 返回对应的 Article 对象，如果未找到则返回 null
     */
    Article getArticleById(Long id);

    /**
     * 获取用户的所有信息，包括粉丝数、总点赞数、总收藏数、总赞助数、总曝光数、网站金币等统计数据。
     *
     * @param userId 用户的唯一标识符
     * @return 包含用户信息的 Map
     */
    Map<String, Object> getUserInformation(Long userId);

    /**
     * 根据用户名获取用户的完整信息，包括基本信息和统计数据，避免N+1查询问题。
     *
     * @param username 用户名
     * @return 包含用户完整信息的 Map，包含注册天数等计算字段
     */
    Map<String, Object> getUserCompleteInformationByUsername(String username);

    /**
     * 为指定文章增加点赞数。
     *
     * @param id 文章的唯一标识符
     * @return 如果点赞操作成功返回 true，否则返回 false
     */
    boolean likeArticle(Long id);

    /**
     * 实现用户订阅操作，将一个用户订阅到另一个用户。
     *
     * @param userId       订阅者的用户ID
     * @param targetUserId 被订阅者的用户ID
     * @return 如果订阅成功返回 true，否则返回 false
     */
 boolean subscribeUser(Long userId, Long targetUserId);

    /**
     * 编辑指定文章的内容和标题，并更新最后修改时间。
     *
     * @param id      文章的唯一标识符
     * @param article 包含更新内容的 Article 对象
     * @param authenticatedUserId 当前认证用户的ID
     * @return 如果修改成功返回 true，否则返回 false
     */
    boolean editArticle(Long id, Article article, Long authenticatedUserId);


    /**
     * 获取所有文章列表（支持分页和排序）
     *
     * @param page 页码
     * @param pageSize 每页大小
     * @param sortBy 排序字段
     * @param sortOrder 排序方向（asc/desc）
     * @return 包含文章列表和总数的Map
     */
    Map<String, Object> getAllArticles(int page, int pageSize, String sortBy, String sortOrder);

    /**
     * 搜索文章（支持分页和排序）
     *
     * @param query 搜索关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @param sortBy 排序字段
     * @param sortOrder 排序方向（asc/desc）
     * @return 包含搜索结果和总数的Map
     */
    Map<String, Object> searchArticles(String query, int page, int pageSize, String sortBy, String sortOrder);

    /**
     * 搜索文章（支持分页、排序和过滤）
     *
     * @param query 搜索关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @param startDate 开始日期 (可选)
     * @param endDate 结束日期 (可选)
     * @param sortBy 排序字段
     * @param sortOrder 排序方向（asc/desc）
     * @return 包含搜索结果和总数的Map
     */
    Map<String, Object> searchArticlesWithFilters(String query, int page, int pageSize, String startDate, String endDate, String sortBy, String sortOrder);

    /**
     * 高级搜索文章（支持多维度过滤）
     *
     * @param searchVo 包含搜索条件的 ArticleSearchAdvancedVo 对象
     * @return 包含搜索结果和总数的Map
     */
    Map<String, Object> searchArticlesAdvanced(ArticleSearchAdvancedVo searchVo);

    /**
     * 为指定文章增加金币或赞助金额。
     *
     * @param id     文章的唯一标识符
     * @param amount 增加的金币金额
     * @return 如果操作成功返回 true，否则返回 false
     */
    boolean addCoin(Long id, Double amount);

    /**
     * 增加指定文章的阅读数或曝光数。
     *
     * @param id 文章的唯一标识符
     * @return 如果操作成功返回 true，否则返回 false
     */
    boolean increaseReadCount(Long id);

    /**
     * 创建一篇新文章并插入到数据库中。
     *
     * @param article 包含新文章信息的 Article 对象
     * @return 返回操作结果，如新创建的文章ID或受影响的行数
     */
    int createArticle(Article article);

    Map<String, Object> getUserAllArticlesStats(Long userId);

    List<Article> getArticlesByUserId(Long userId);

    /**
     * 获取所有文章分类
     * @return 分类列表
     */
    List<ArticleCategory> getAllCategories();

    /**
     * 收藏文章
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 如果收藏成功返回 true，否则返回 false
     */
    boolean favoriteArticle(Long articleId, Long userId);

    /**
     * 取消收藏文章
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 如果取消收藏成功返回 true，否则返回 false
     */
    boolean unfavoriteArticle(Long articleId, Long userId);

    /**
     * 检查用户是否已收藏文章
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 如果已收藏返回 true，否则返回 false
     */
    boolean isArticleFavoritedByUser(Long articleId, Long userId);

    /**
     * 获取用户收藏的文章列表
     *
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 包含收藏文章列表和总数的Map
     */
    Map<String, Object> getUserFavoriteArticles(Long userId, int page, int pageSize);

    /**
     * 获取推荐文章列表
     *
     * @param page 页码
     * @param pageSize 每页大小
     * @return 包含推荐文章列表和总数的Map
     */
    Map<String, Object> getRecommendedArticles(int page, int pageSize);
}
