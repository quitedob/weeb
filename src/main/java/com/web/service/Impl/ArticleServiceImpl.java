package com.web.service.Impl;

import com.web.mapper.ArticleMapper;
import com.web.model.Article;
import com.web.service.ArticleService;
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
    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createArticle(Article article) {
        // Ensure article.getUserId() is not null if it's needed by updateAuthTotals
        // The controller should set userId in the article object before calling this service method.
        int result = articleMapper.insertArticle(article);
        if (result > 0 && article.getUserId() != null) { // Check if userId is available
            articleMapper.updateAuthTotals(article.getUserId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getUserAllArticlesStats(Long userId) {
        // 获取聚合统计数据
        Map<String, Object> stats = articleMapper.selectAggregatedStatsByUserId(userId);
        if (stats != null) {
            // 将 userId 放入 stats Map，以便更新 auth 时使用
            stats.put("userId", userId);
            // 更新 auth 表
            articleMapper.updateAuthArticleStats(stats);
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
        // 查询并返回更新后的用户信息
        return articleMapper.selectUserInformation(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeArticle(Long id) {
        Article article = articleMapper.selectArticleById(id);
        if (article == null) {
            return false; // Or throw exception
        }
        boolean success = articleMapper.increaseLikeCount(id) > 0;
        if (success) {
            articleMapper.updateAuthTotals(article.getUserId());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean subscribeUser(Long userId, Long targetUserId) {
        return articleMapper.subscribeUser(userId, targetUserId) > 0;
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
    public boolean editArticle(Long id, Article article) { // 'article' here is the DTO with new content
        Article originalArticle = articleMapper.selectArticleById(id);
        if (originalArticle == null) {
            return false; // Or throw an exception
        }
        // The article parameter for updateArticleContent might not need userId if it's not changing it.
        // The updateAuthTotals needs the userId of the article's author.
        boolean success = articleMapper.updateArticleContent(id, article) > 0; // Assuming updateArticleContent is the correct mapper method
        if (success) {
            articleMapper.updateAuthTotals(originalArticle.getUserId());
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
        if (success) {
            articleMapper.updateAuthTotals(article.getUserId());
        }
        return success;
    }

    // This is the updated deleteArticle method
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteArticle(Long id) {
        Article article = articleMapper.selectArticleById(id);
        if (article == null) {
            return false; // Or throw exception if article not found
        }
        Long userId = article.getUserId(); // Get userId before deleting
        boolean success = articleMapper.deleteArticleById(id) > 0;
        if (success && userId != null) {
            articleMapper.updateAuthTotals(userId);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseReadCount(Long id) {
        return articleMapper.increaseReadCount(id) > 0;
    }
}
