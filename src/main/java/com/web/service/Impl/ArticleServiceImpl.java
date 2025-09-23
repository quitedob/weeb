package com.web.service.Impl;

import com.web.mapper.ArticleMapper;
import com.web.model.Article;
import com.web.service.ArticleService;
import com.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Added import

import java.util.HashMap; // Added import
import java.util.List;
import java.util.Map;

@Service
public class ArticleServiceImpl implements ArticleService {
    // Removed deleteArticle from here as it will be updated later

    private final ArticleMapper articleMapper;

    @Autowired
    private UserService userService;

    @Autowired
    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createArticle(Article article) {
        // Ensure article.getUserId() is not null if it's needed for stats update
        // The controller should set userId in the article object before calling this service method.
        int result = articleMapper.insertArticle(article);
        if (result > 0 && article.getUserId() != null) {
            // 使用UserService记录文章曝光，而不是直接更新auth表
            userService.recordArticleExposure(article.getUserId(), 1L);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getUserAllArticlesStats(Long userId) {
        // 获取聚合统计数据
        Map<String, Object> stats = articleMapper.selectAggregatedStatsByUserId(userId);
        if (stats != null) {
            // 将 userId 放入 stats Map，以便更新 user_stats 时使用
            stats.put("userId", userId);
            // 更新 user_stats 表
            articleMapper.updateUserStatsArticleStats(stats);
        }
        return stats;
    }

    @Override
    public List<Article> getArticlesByUserId(Long userId) {
        return articleMapper.selectArticlesByUserId(userId);
    }

    @Override
    public Article getArticleById(Long id) {
        return articleMapper.selectArticleById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getUserInformation(Long userId) {
        // 使用UserService获取用户完整信息，包含统计数据
        com.web.model.UserWithStats userWithStats = userService.getUserProfile(userId);
        
        Map<String, Object> result = new HashMap<>();
        if (userWithStats != null) {
            if (userWithStats.getUser() != null) {
                result.put("user", userWithStats.getUser());
            }
            if (userWithStats.getUserStats() != null) {
                result.put("stats", userWithStats.getUserStats());
                // 为了兼容现有代码，也可以将统计数据平铺到结果中
                result.put("fansCount", userWithStats.getFansCount());
                result.put("totalLikes", userWithStats.getTotalLikes());
                result.put("totalFavorites", userWithStats.getTotalFavorites());
                result.put("totalSponsorship", userWithStats.getTotalSponsorship());
                result.put("totalArticleExposure", userWithStats.getTotalArticleExposure());
                result.put("websiteCoins", userWithStats.getWebsiteCoins());
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeArticle(Long id) {
        Article article = articleMapper.selectArticleById(id);
        if (article == null) {
            return false; // Or throw exception
        }
        boolean success = articleMapper.increaseLikeCount(id) > 0;
        if (success && article.getUserId() != null) {
            // 使用UserService更新作者的点赞统计，而不是直接更新auth表
            // 这里假设点赞用户ID为null，实际应该从上下文获取
            userService.likeArticle(null, id, article.getUserId());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean subscribeUser(Long userId, Long targetUserId) {
        // 使用UserService处理关注操作，它会正确更新user_stats表
        return userService.followUser(userId, targetUserId);
    }

    @Override
    public Map<String, Object> getAllArticles(int page, int pageSize) {
       int offset = (page - 1) * pageSize;
       List<Article> articles = articleMapper.getAllArticles(offset, pageSize); // Uses new mapper method
       int total = articleMapper.countAllArticles(); // Uses new mapper method

       Map<String, Object> result = new HashMap<>();
       result.put("list", articles);
       result.put("total", total);
       return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editArticle(Long id, Article article, Long authenticatedUserId) {
        Article originalArticle = articleMapper.selectArticleById(id);
        if (originalArticle == null) {
            return false; // 文章不存在
        }
        
        // 安全验证：确保用户只能编辑自己的文章
        if (!originalArticle.getUserId().equals(authenticatedUserId)) {
            return false; // 无权编辑此文章
        }
        
        // 验证通过后，执行更新操作
        boolean success = articleMapper.updateArticleContent(id, article) > 0;
        if (success && originalArticle.getUserId() != null) {
            // 文章编辑可能影响曝光数，使用UserService更新统计
            userService.recordArticleExposure(originalArticle.getUserId(), 1L);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addCoin(Long id, Double amount) {
        Article article = articleMapper.selectArticleById(id);
        if (article == null) {
            return false; // Or throw exception
        }
        boolean success = articleMapper.addCoin(id, amount) > 0;
        if (success && article.getUserId() != null) {
            // 将金币数量转换为Long类型，并使用UserService处理赞助
            Long coinAmount = amount != null ? amount.longValue() : 0L;
            // 这里假设赞助者ID为null，实际应该从上下文获取
            userService.sponsorUser(null, article.getUserId(), coinAmount);
        }
        return success;
    }

    // This is the updated deleteArticle method
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteArticle(Long id, Long authenticatedUserId) {
        Article article = articleMapper.selectArticleById(id);
        if (article == null) {
            return false; // 文章不存在
        }
        
        // 安全验证：确保用户只能删除自己的文章
        if (!article.getUserId().equals(authenticatedUserId)) {
            return false; // 无权删除此文章
        }
        
        Long userId = article.getUserId(); // Get userId before deleting
        boolean success = articleMapper.deleteArticleById(id) > 0;
        if (success && userId != null) {
            // 文章删除后，可能需要重新计算用户统计数据
            // 这里可以调用批量重新计算方法，或者简单地减少曝光数
            // 暂时不做处理，因为删除文章通常不会影响历史统计
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseReadCount(Long id) {
        return articleMapper.increaseReadCount(id) > 0;
    }
}
