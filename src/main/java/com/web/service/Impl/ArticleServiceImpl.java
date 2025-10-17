package com.web.service.Impl;

import com.web.mapper.ArticleMapper;
import com.web.mapper.ArticleCategoryMapper;
import com.web.model.Article;
import com.web.model.ArticleCategory;
import com.web.service.ArticleService;
import com.web.util.SqlInjectionUtils;
import com.web.exception.WeebException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文章服务实现类
 * 实现文章相关的业务逻辑
 */
@Service
@Transactional
public class ArticleServiceImpl implements ArticleService {

    // 允许的排序字段白名单
    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "created_at", "updated_at", "likes_count", "favorites_count",
        "exposure_count", "sponsors_count", "article_title"
    );

    // 允许的排序方向白名单
    private static final Set<String> ALLOWED_SORT_ORDERS = Set.of("asc", "desc");

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleCategoryMapper articleCategoryMapper;

    /**
     * 验证排序参数
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 验证后的排序参数数组
     */
    private String[] validateSortParams(String sortBy, String sortOrder) {
        // 验证并设置默认排序字段
        if (sortBy == null || sortBy.trim().isEmpty() || !ALLOWED_SORT_COLUMNS.contains(sortBy.toLowerCase())) {
            sortBy = "created_at";
        }

        // 验证并设置默认排序方向
        if (sortOrder == null || sortOrder.trim().isEmpty() || !ALLOWED_SORT_ORDERS.contains(sortOrder.toLowerCase())) {
            sortOrder = "desc";
        }

        return new String[]{sortBy, sortOrder};
    }

    @Override
    @Transactional(readOnly = true)
    public boolean deleteArticle(Long id, Long authenticatedUserId) {
        // 先检查文章是否存在且属于当前用户
        Article article = articleMapper.selectArticleById(id);
        if (article == null || !article.getUserId().equals(authenticatedUserId)) {
            return false;
        }
        
        int result = articleMapper.deleteArticleById(id);
        if (result > 0) {
            // 更新用户统计数据
            articleMapper.updateUserStatsTotals(authenticatedUserId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Article getArticleById(Long id) {
        return articleMapper.selectArticleById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserInformation(Long userId) {
        return articleMapper.selectUserStatsInformation(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserCompleteInformationByUsername(String username) {
        return articleMapper.selectUserCompleteInformationByUsername(username);
    }

    @Override
    public boolean likeArticle(Long id) {
        int result = articleMapper.increaseLikeCount(id);
        if (result > 0) {
            // 获取文章作者ID并更新其统计数据
            Article article = articleMapper.selectArticleById(id);
            if (article != null) {
                articleMapper.updateUserStatsTotals(article.getUserId());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean subscribeUser(Long userId, Long targetUserId) {
        int result = articleMapper.subscribeUser(userId, targetUserId);
        return result > 0;
    }

    @Override
    public boolean editArticle(Long id, Article article, Long authenticatedUserId) {
        // 先检查文章是否存在且属于当前用户
        Article existingArticle = articleMapper.selectArticleById(id);
        if (existingArticle == null || !existingArticle.getUserId().equals(authenticatedUserId)) {
            return false;
        }
        
        // 设置更新时间
        article.setUpdatedAt(LocalDateTime.now());
        int result = articleMapper.updateArticleContent(id, article);
        return result > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllArticles(int page, int pageSize, String sortBy, String sortOrder) {
        // 验证排序参数
        String[] validatedParams = validateSortParams(sortBy, sortOrder);
        sortBy = validatedParams[0];
        sortOrder = validatedParams[1];

        // 验证分页参数
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        int offset = (page - 1) * pageSize;
        List<Article> articles = articleMapper.getAllArticles(offset, pageSize, sortBy, sortOrder);
        int totalCount = articleMapper.countAllArticles();

        Map<String, Object> result = new HashMap<>();
        result.put("list", articles);  // 改为 list 以匹配前端期望
        result.put("total", totalCount);  // 改为 total 以匹配前端期望
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));

        return result;
    }

    @Override
    public boolean favoriteArticle(Long articleId, Long userId) {
        // 检查文章是否存在
        Article article = articleMapper.selectArticleById(articleId);
        if (article == null) {
            return false;
        }
        
        // 检查是否已收藏
        if (isArticleFavoritedByUser(articleId, userId)) {
            return true; // 已收藏，直接返回成功
        }
        
        // 添加收藏记录
        int result = articleMapper.insertFavorite(articleId, userId);
        if (result > 0) {
            // 增加文章收藏数
            articleMapper.increaseFavoriteCount(articleId);
            // 更新用户统计数据
            articleMapper.updateUserStatsTotals(userId);
            return true;
        }
        return false;
    }

    @Override
    public boolean unfavoriteArticle(Long articleId, Long userId) {
        // 检查是否已收藏
        if (!isArticleFavoritedByUser(articleId, userId)) {
            return true; // 未收藏，直接返回成功
        }
        
        // 删除收藏记录
        int result = articleMapper.deleteFavorite(articleId, userId);
        if (result > 0) {
            // 减少文章收藏数
            articleMapper.decreaseFavoriteCount(articleId);
            // 更新用户统计数据
            articleMapper.updateUserStatsTotals(userId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isArticleFavoritedByUser(Long articleId, Long userId) {
        Integer count = articleMapper.countFavorite(articleId, userId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserFavoriteArticles(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Article> articles = articleMapper.getUserFavoriteArticles(userId, offset, pageSize);
        int totalCount = articleMapper.countUserFavoriteArticles(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", articles);
        result.put("total", totalCount);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRecommendedArticles(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Article> articles = articleMapper.getRecommendedArticles(offset, pageSize);
        int totalCount = articleMapper.countAllArticles(); // 推荐文章总数使用所有文章数
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", articles);
        result.put("total", totalCount);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchArticles(String query, int page, int pageSize, String sortBy, String sortOrder) {
        // 验证搜索关键词
        if (query == null || query.trim().isEmpty()) {
            throw new WeebException("搜索关键词不能为空");
        }
        query = query.trim();
        if (query.length() > 100) {
            throw new WeebException("搜索关键词长度不能超过100个字符");
        }

        // 验证排序参数
        String[] validatedParams = validateSortParams(sortBy, sortOrder);
        sortBy = validatedParams[0];
        sortOrder = validatedParams[1];

        // 验证分页参数
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        int offset = (page - 1) * pageSize;
        List<Article> articles = articleMapper.searchArticles(query, offset, pageSize, sortBy, sortOrder);
        int totalCount = articleMapper.countSearchResults(query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", articles);
        result.put("total", totalCount);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchArticlesWithFilters(String query, int page, int pageSize, String startDate, String endDate, String sortBy, String sortOrder) {
        // 验证搜索关键词
        if (query == null || query.trim().isEmpty()) {
            throw new WeebException("搜索关键词不能为空");
        }
        query = query.trim();
        if (query.length() > 100) {
            throw new WeebException("搜索关键词长度不能超过100个字符");
        }

        // 验证排序参数
        String[] validatedParams = validateSortParams(sortBy, sortOrder);
        sortBy = validatedParams[0];
        sortOrder = validatedParams[1];

        // 验证分页参数
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        // 验证日期格式 (简单的YYYY-MM-DD格式验证)
        if (startDate != null && !startDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new WeebException("开始日期格式不正确，请使用YYYY-MM-DD格式");
        }
        if (endDate != null && !endDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new WeebException("结束日期格式不正确，请使用YYYY-MM-DD格式");
        }

        int offset = (page - 1) * pageSize;
        List<Article> articles = articleMapper.searchArticlesWithFilters(query, offset, pageSize, startDate, endDate, sortBy, sortOrder);
        int totalCount = articleMapper.countSearchResultsWithFilters(query, startDate, endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("list", articles);
        result.put("total", totalCount);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));

        return result;
    }

    @Override
    public boolean addCoin(Long id, Double amount) {
        int result = articleMapper.addCoin(id, amount);
        if (result > 0) {
            // 获取文章作者ID并更新其统计数据
            Article article = articleMapper.selectArticleById(id);
            if (article != null) {
                articleMapper.updateUserStatsTotals(article.getUserId());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean increaseReadCount(Long id) {
        int result = articleMapper.increaseReadCount(id);
        if (result > 0) {
            // 获取文章作者ID并更新其统计数据
            Article article = articleMapper.selectArticleById(id);
            if (article != null) {
                articleMapper.updateUserStatsTotals(article.getUserId());
            }
            return true;
        }
        return false;
    }

    @Override
    public int createArticle(Article article) {
        // 设置创建时间
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        
        // 初始化status字段：如果未设置，默认为草稿状态
        if (article.getStatus() == null) {
            article.setStatus(0); // 0: 草稿, 1: 已发布
        }
        
        // 初始化统计字段
        if (article.getLikesCount() == null) {
            article.setLikesCount(0);
        }
        if (article.getFavoritesCount() == null) {
            article.setFavoritesCount(0);
        }
        if (article.getSponsorsCount() == null) {
            article.setSponsorsCount(0.0);
        }
        if (article.getExposureCount() == null) {
            article.setExposureCount(0L);
        }
        
        int result = articleMapper.insertArticle(article);
        if (result > 0 && article.getUserId() != null) {
            // 更新用户统计数据
            articleMapper.updateUserStatsTotals(article.getUserId());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserAllArticlesStats(Long userId) {
        return articleMapper.selectAggregatedStatsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Article> getArticlesByUserId(Long userId) {
        return articleMapper.selectArticlesByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleCategory> getAllCategories() {
        return articleCategoryMapper.getAllCategories();
    }
}