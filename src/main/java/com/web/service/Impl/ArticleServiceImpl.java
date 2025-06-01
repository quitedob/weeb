package com.web.service.Impl;

import com.web.mapper.ArticleMapper;
import com.web.model.Article;
import com.web.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ArticleServiceImpl implements ArticleService {
    @Override
    public boolean deleteArticle(Long id) {
        // 调用 Mapper 层的方法, 返回影响行数大于 0 则说明删除成功
        return articleMapper.deleteArticleById(id) > 0;
    }

    private final ArticleMapper articleMapper;

    @Autowired
    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public int createArticle(Article article) {
        return articleMapper.insertArticle(article);
    }

    @Override
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
    public Map<String, Object> getUserInformation(Long userId) {
        // 更新指定用户在 auth 表中的文章统计数据
        articleMapper.updateAuthTotals(userId);
        // 查询并返回更新后的用户信息
        return articleMapper.selectUserInformation(userId);
    }

    @Override
    public boolean likeArticle(Long id) {
        return articleMapper.increaseLikeCount(id) > 0;
    }

    @Override
    public boolean subscribeUser(Long userId, Long targetUserId) {
        return articleMapper.subscribeUser(userId, targetUserId) > 0;
    }

    @Override
    public List<Article> getAllArticles() {
        return articleMapper.getAllArticles();
    }
    @Override
    public boolean editArticle(Long id, Article article) {
        return articleMapper.updateArticleContent(id, article) > 0;
    }

    @Override
    public boolean addCoin(Long id, Double amount) {
        return articleMapper.addCoin(id, amount) > 0;
    }

    @Override
    public boolean increaseReadCount(Long id) {
        return articleMapper.increaseReadCount(id) > 0;
    }
}
