package com.web.service.impl;

import com.web.mapper.ArticleMapper;
import com.web.mapper.ArticleCategoryMapper;
import com.web.model.Article;
import com.web.model.ArticleCategory;
import com.web.service.ArticleService;
import com.web.service.UserTypeSecurityService;
import com.web.service.UserService;
import com.web.exception.WeebException;
import com.web.util.ValidationUtils;
import com.web.util.SqlInjectionUtils;
import com.web.vo.article.ArticleSearchAdvancedVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文章服务实现类
 * 实现文章相关的业务逻辑
 */
@Slf4j
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
    private com.web.mapper.UserStatsMapper userStatsMapper;

    @Autowired
    private ArticleCategoryMapper articleCategoryMapper;

    @Autowired
    private UserTypeSecurityService userTypeSecurityService;

    @Autowired
    private com.web.service.UserService userService;

    /**
     * 验证排序参数
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 验证后的排序参数数组
     */
    private String[] validateSortParams(String sortBy, String sortOrder) {
        // 使用SqlInjectionUtils验证排序参数以防止SQL注入
        if (!SqlInjectionUtils.validateSortParams(sortBy, sortOrder)) {
            sortBy = "created_at";
            sortOrder = "desc";
        } else {
            // 进一步验证排序字段是否在允许的列表中
            if (sortBy != null && ALLOWED_SORT_COLUMNS.contains(sortBy.toLowerCase())) {
                sortBy = sortBy.toLowerCase();
            } else {
                sortBy = "created_at";
            }

            // 验证排序方向
            if (sortOrder != null && ALLOWED_SORT_ORDERS.contains(sortOrder.toLowerCase())) {
                sortOrder = sortOrder.toLowerCase();
            } else {
                sortOrder = "desc";
            }
        }

        return new String[]{sortBy, sortOrder};
    }

    @Override
    @Transactional
    public boolean deleteArticle(Long id, Long authenticatedUserId) {
        try {
            // 先检查文章是否存在
            Article article = articleMapper.selectArticleById(id);
            if (article == null) {
                log.warn("文章不存在: articleId={}", id);
                return false;
            }

            // 获取当前用户信息
            com.web.model.User currentUser = userService.getUserBasicInfo(authenticatedUserId);
            if (currentUser == null || currentUser.getUsername() == null) {
                log.error("无法获取用户信息: userId={}", authenticatedUserId);
                return false;
            }

            // 检查权限：文章作者或管理员可以删除
            boolean isAuthor = article.getUserId().equals(authenticatedUserId);
            boolean isAdmin = userTypeSecurityService.isAdmin(currentUser.getUsername());

            if (!isAuthor && !isAdmin) {
                log.warn("用户无权删除文章: articleId={}, userId={}, isAuthor={}, isAdmin={}",
                    id, authenticatedUserId, isAuthor, isAdmin);
                return false;
            }

            // 执行删除操作
            int result = articleMapper.deleteArticleById(id);
            if (result > 0) {
                // 更新文章作者的统计数据
                articleMapper.updateUserStatsTotals(article.getUserId());

                // 记录删除日志
                if (isAdmin && !isAuthor) {
                    log.info("管理员删除用户文章: articleId={}, adminId={}, authorId={}, articleTitle={}",
                        id, authenticatedUserId, article.getUserId(), article.getArticleTitle());
                } else {
                    log.info("用户删除自己的文章: articleId={}, userId={}, articleTitle={}",
                        id, authenticatedUserId, article.getArticleTitle());
                }

                return true;
            } else {
                log.error("删除文章失败: articleId={}, userId={}", id, authenticatedUserId);
                return false;
            }
        } catch (Exception e) {
            log.error("删除文章时发生异常: articleId={}, userId={}", id, authenticatedUserId, e);
            throw new WeebException("删除文章失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Article getArticleById(Long id) {
        Article article = articleMapper.selectArticleById(id);
        if (article != null && !canViewArticle(article)) {
            throw new org.springframework.security.access.AccessDeniedException("Article is not public");
        }
        return article;
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
    @Transactional
    public boolean likeArticle(Long articleId, Long userId) {
        // 检查文章是否存在
        Article article = articleMapper.selectArticleById(articleId);
        if (article == null) {
            log.warn("文章不存在: articleId={}", articleId);
            return false;
        }
        
        // 检查是否已点赞
        if (isArticleLikedByUser(articleId, userId)) {
            log.info("用户已点赞该文章: articleId={}, userId={}", articleId, userId);
            return true; // 已点赞，直接返回成功
        }
        
        // 插入点赞记录
        int result = articleMapper.insertArticleLike(userId, articleId);
        if (result > 0) {
            // 更新文章点赞数
            articleMapper.increaseLikeCount(articleId);
            
            // 更新作者统计
            if (article.getUserId() != null) {
                articleMapper.updateUserStatsTotals(article.getUserId());
            }
            
            log.info("点赞成功: articleId={}, userId={}", articleId, userId);
            return true;
        }
        
        log.error("点赞失败: articleId={}, userId={}", articleId, userId);
        return false;
    }

    @Override
    @Transactional
    public boolean unlikeArticle(Long articleId, Long userId) {
        // 检查是否已点赞
        if (!isArticleLikedByUser(articleId, userId)) {
            log.info("用户未点赞该文章: articleId={}, userId={}", articleId, userId);
            return true; // 未点赞，直接返回成功
        }
        
        // 删除点赞记录
        int result = articleMapper.deleteArticleLike(userId, articleId);
        if (result > 0) {
            // 减少文章点赞数
            articleMapper.decreaseLikeCount(articleId);
            
            // 更新作者统计
            Article article = articleMapper.selectArticleById(articleId);
            if (article != null && article.getUserId() != null) {
                articleMapper.updateUserStatsTotals(article.getUserId());
            }
            
            log.info("取消点赞成功: articleId={}, userId={}", articleId, userId);
            return true;
        }
        
        log.error("取消点赞失败: articleId={}, userId={}", articleId, userId);
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isArticleLikedByUser(Long articleId, Long userId) {
        return articleMapper.isArticleLikedByUser(userId, articleId);
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
        article.setStatus(Integer.valueOf(0).equals(article.getStatus()) ? 0 : 1);
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
        
        if (userId == null || userId <= 0 || !canViewArticle(article)) {
            throw new org.springframework.security.access.AccessDeniedException("Article is not accessible");
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
        if (userId == null || userId <= 0 || page < 1 || pageSize < 1 || pageSize > 100) {
            throw new WeebException("Invalid favorites pagination or user");
        }
        int offset = Math.toIntExact((long) (page - 1) * pageSize);
        boolean canViewAll = com.web.security.SecurityUtils.isAdmin();
        List<Article> articles = articleMapper.getUserFavoriteArticles(userId, offset, pageSize, canViewAll);
        int totalCount = articleMapper.countUserFavoriteArticles(userId, canViewAll);
        
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
        // 使用ValidationUtils验证搜索关键词
        if (!ValidationUtils.validateSearchKeyword(query)) {
            throw new WeebException("搜索关键词不能为空或格式不正确");
        }

        // 使用ValidationUtils进行安全清理
        query = ValidationUtils.sanitizeSearchKeyword(query.trim());

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
        // 使用ValidationUtils验证搜索关键词
        if (!ValidationUtils.validateSearchKeyword(query)) {
            throw new WeebException("搜索关键词不能为空或格式不正确");
        }

        // 使用ValidationUtils进行安全清理
        query = ValidationUtils.sanitizeSearchKeyword(query.trim());

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
    public boolean addCoin(Long id, Double amount, Long userId) {
        if (userId == null || userId <= 0 || amount == null || !Double.isFinite(amount)
                || amount < 1 || amount > 10000 || amount != Math.rint(amount)) {
            throw new WeebException("投币数量必须是1至10000之间的整数");
        }
        Article article = articleMapper.selectArticleById(id);
        if (article == null || !Integer.valueOf(2).equals(article.getStatus())) {
            throw new WeebException("文章不存在或尚未发布");
        }
        if (userId.equals(article.getUserId())) throw new WeebException("不能给自己的文章投币");
        if (userStatsMapper.deductWebsiteCoins(userId, amount.longValue()) != 1) {
            throw new WeebException("网站币余额不足");
        }
        if (articleMapper.addCoin(id, amount) != 1) throw new WeebException("投币失败");
        articleMapper.updateUserStatsTotals(article.getUserId());
        return true;
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
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        article.setStatus(article.getStatus() == null || article.getStatus() == 0 ? 0 : 1);

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
        return articleMapper.selectArticlesByUserId(userId).stream().filter(this::canViewArticle).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleCategory> getAllCategories() {
        return articleCategoryMapper.getAllCategories();
    }

    @Override
    public Map<String, Object> searchArticlesAdvanced(ArticleSearchAdvancedVo searchVo) {
        Long actor = com.web.security.SecurityUtils.getCurrentUserId();
        if (!com.web.security.SecurityUtils.isAdmin() && (actor == null || !actor.equals(searchVo.getAuthorId()))) {
            searchVo.setStatus(2);
        }
        // 参数验证
        String[] validatedSortParams = validateSortParams(searchVo.getSortBy(), searchVo.getSortOrder());
        searchVo.setSortBy(validatedSortParams[0]);
        searchVo.setSortOrder(validatedSortParams[1]);

        int offset = (searchVo.getPage() - 1) * searchVo.getPageSize();

        // 调用Mapper进行高级搜索
        List<Article> articles = articleMapper.searchArticlesAdvanced(searchVo, offset);
        int totalCount = articleMapper.countAdvancedSearchResults(searchVo);

        // 组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", articles);
        result.put("total", totalCount);
        result.put("currentPage", searchVo.getPage());
        result.put("pageSize", searchVo.getPageSize());
        result.put("totalPages", (int) Math.ceil((double) totalCount / searchVo.getPageSize()));

        return result;
    }

    @Override
    public Map<String, Object> getPendingArticlesForModeration(int page, int pageSize, Integer status, String keyword) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 50) pageSize = 20;
        if (status != null && (status < 0 || status > 3)) throw new WeebException("Invalid article status");
        ArticleSearchAdvancedVo query = new ArticleSearchAdvancedVo();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setQuery(keyword);
        query.setStatus(status);
        List<Article> articles = articleMapper.searchArticlesAdvanced(query, (page - 1) * pageSize);
        int total = articleMapper.countAdvancedSearchResults(query);
        return Map.of("list", articles, "total", total, "currentPage", page, "pageSize", pageSize,
                "totalPages", (int) Math.ceil((double) total / pageSize));
    }

    @Override
    public Map<String, Object> getContentModerationStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 待审核文章数量
            int pendingCount = articleMapper.countByStatus(1); // 假设0表示待审核
            statistics.put("pendingArticles", pendingCount);

            // 已审核文章数量
            int approvedCount = articleMapper.countByStatus(2); // 假设1表示已审核
            statistics.put("approvedArticles", approvedCount);

            // 被拒绝文章数量
            int rejectedCount = articleMapper.countByStatus(3); // 假设2表示被拒绝
            statistics.put("rejectedArticles", rejectedCount);

            // 总文章数量
            int totalArticles = pendingCount + approvedCount + rejectedCount + articleMapper.countByStatus(0);
            statistics.put("totalArticles", totalArticles);

            // 审核通过率
            if (totalArticles > 0) {
                double approvalRate = (double) approvedCount / totalArticles * 100;
                statistics.put("approvalRate", Math.round(approvalRate * 100.0) / 100.0);
            } else {
                statistics.put("approvalRate", 0.0);
            }

            // 今日审核数量
            String today = java.time.LocalDate.now().toString();
            int todayReviewed = articleMapper.countReviewedToday(today);
            statistics.put("todayReviewed", todayReviewed);

            // 本周审核数量
            String weekAgo = java.time.LocalDate.now().minusDays(7).toString();
            int weekReviewed = articleMapper.countReviewedInPeriod(weekAgo, today);
            statistics.put("weekReviewed", weekReviewed);

            statistics.put("lastUpdated", java.time.LocalDateTime.now().toString());

        } catch (Exception e) {
            // 如果出现异常，返回基本统计信息
            statistics.put("error", "获取审核统计失败: " + e.getMessage());
            statistics.put("pendingArticles", 0);
            statistics.put("approvedArticles", 0);
            statistics.put("rejectedArticles", 0);
            statistics.put("totalArticles", 0);
            statistics.put("approvalRate", 0.0);
            statistics.put("todayReviewed", 0);
            statistics.put("weekReviewed", 0);
        }

        return statistics;
    }

    private boolean canViewArticle(Article article) {
        return Integer.valueOf(2).equals(article.getStatus())
                || article.getUserId().equals(com.web.security.SecurityUtils.getCurrentUserId())
                || com.web.security.SecurityUtils.isAdmin();
    }

    @Override
    public boolean approveArticle(Long articleId) {
        return articleMapper.reviewArticle(articleId, 2, com.web.security.SecurityUtils.getCurrentUserId(), null) == 1;
    }

    @Override
    public boolean rejectArticle(Long articleId, String reason) {
        return articleMapper.reviewArticle(articleId, 3, com.web.security.SecurityUtils.getCurrentUserId(), reason) == 1;
    }

    @Override
    public boolean deleteArticleByAdmin(Long articleId, String reason) {
        return deleteArticle(articleId, com.web.security.SecurityUtils.getCurrentUserId());
    }
}
