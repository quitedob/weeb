package com.web.service.Impl;

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

    @Override
    public Map<String, Object> searchArticlesAdvanced(ArticleSearchAdvancedVo searchVo) {
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
        Map<String, Object> result = new HashMap<>();

        try {
            // 验证分页参数
            if (page < 1) page = 1;
            if (pageSize < 1 || pageSize > 50) pageSize = 20; // 审核页面默认20条

            int offset = (page - 1) * pageSize;

            // 构建查询条件
            Map<String, Object> params = new HashMap<>();
            params.put("offset", offset);
            params.put("pageSize", pageSize);
            params.put("keyword", keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null);

            // 根据状态过滤
            if (status != null) {
                params.put("status", status);
            } else {
                // 默认查询待审核和审核中的文章 (假设 0=待审核, 3=审核中)
                params.put("status", 0); // 主要查询待审核的文章
            }

            // 查询文章列表
            List<Article> articles = articleMapper.getAllArticles(offset, pageSize, "created_at", "desc");

            // 统计总数
            int totalCount = 0;
            if (status != null) {
                totalCount = articleMapper.countByStatus(status);
            } else {
                totalCount = articleMapper.countByStatus(0); // 统计待审核文章总数
            }

            // 如果有关键词，过滤文章列表
            if (keyword != null && !keyword.trim().isEmpty()) {
                articles = articles.stream()
                    .filter(article ->
                        article.getArticleTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                        (article.getArticleContent() != null &&
                         article.getArticleContent().toLowerCase().contains(keyword.toLowerCase()))
                    )
                    .toList();

                // 重新计算总数（近似值）
                totalCount = articles.size();
            }

            result.put("list", articles);
            result.put("total", totalCount);
            result.put("currentPage", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));

            log.info("获取待审核文章列表成功: page={}, pageSize={}, status={}, keyword={}, total={}",
                    page, pageSize, status, keyword, totalCount);

        } catch (Exception e) {
            log.error("获取待审核文章列表失败: page={}, pageSize={}, status={}, keyword={}",
                    page, pageSize, status, keyword, e);
            result.put("error", "获取待审核文章列表失败: " + e.getMessage());
            result.put("list", new ArrayList<>());
            result.put("total", 0);
        }

        return result;
    }

    @Override
    public Map<String, Object> getContentModerationStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 待审核文章数量
            int pendingCount = articleMapper.countByStatus(0); // 假设0表示待审核
            statistics.put("pendingArticles", pendingCount);

            // 已审核文章数量
            int approvedCount = articleMapper.countByStatus(1); // 假设1表示已审核
            statistics.put("approvedArticles", approvedCount);

            // 被拒绝文章数量
            int rejectedCount = articleMapper.countByStatus(2); // 假设2表示被拒绝
            statistics.put("rejectedArticles", rejectedCount);

            // 总文章数量
            int totalArticles = articleMapper.countAllArticles();
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

    @Override
    public boolean approveArticle(Long articleId) {
        try {
            // 记录文章通过审核的操作日志
            log.info("通过文章审核: articleId={}", articleId);

            // 更新文章状态为已通过（假设状态码 1 表示已通过）
            Article article = articleMapper.selectArticleById(articleId);
            if (article == null) {
                log.warn("要审核的文章不存在: articleId={}", articleId);
                return false;
            }

            // 将文章状态设置为已通过
            articleMapper.updateArticleStatus(articleId, 1);

            log.info("文章审核通过成功: articleId={}", articleId);
            return true;

        } catch (Exception e) {
            log.error("通过文章审核失败: articleId={}", articleId, e);
            return false;
        }
    }

    @Override
    public boolean rejectArticle(Long articleId, String reason) {
        try {
            // 记录文章拒绝操作的日志
            log.info("拒绝文章审核: articleId={}, reason={}", articleId, reason);

            // 更新文章状态为已拒绝（假设状态码 2 表示已拒绝）
            Article article = articleMapper.selectArticleById(articleId);
            if (article == null) {
                log.warn("要拒绝的文章不存在: articleId={}", articleId);
                return false;
            }

            // 将文章状态设置为已拒绝
            articleMapper.updateArticleStatus(articleId, 2);

            log.info("文章拒绝成功: articleId={}", articleId);
            return true;

        } catch (Exception e) {
            log.error("拒绝文章失败: articleId={}, reason={}", articleId, reason, e);
            return false;
        }
    }

    @Override
    public boolean deleteArticleByAdmin(Long articleId, String reason) {
        try {
            // 记录管理员删除操作的日志
            log.info("管理员删除文章: articleId={}, reason={}", articleId, reason);

            // 更新文章状态为已删除（逻辑删除）
            Article article = articleMapper.selectArticleById(articleId);
            if (article == null) {
                log.warn("要删除的文章不存在: articleId={}", articleId);
                return false;
            }

            // 将文章状态设置为已删除（假设状态码 3 表示已删除）
            articleMapper.updateArticleStatus(articleId, 3);

            // 可以在这里添加其他清理逻辑，比如删除相关的评论、收藏等
            // 但为了简单起见，这里只做文章状态更新

            log.info("文章删除成功: articleId={}", articleId);
            return true;

        } catch (Exception e) {
            log.error("删除文章失败: articleId={}, reason={}", articleId, reason, e);
            return false;
        }
    }
}