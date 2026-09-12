package com.web.security;

import com.web.exception.WeebException;
import com.web.mapper.ArticleMapper;
import com.web.mapper.UserStatsMapper;
import com.web.model.Article;
import com.web.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArticleCoinTransferTest {
    private ArticleMapper articles;
    private UserStatsMapper stats;
    private ArticleServiceImpl service;

    @BeforeEach void setUp() {
        articles = mock(ArticleMapper.class); stats = mock(UserStatsMapper.class);
        service = new ArticleServiceImpl();
        ReflectionTestUtils.setField(service, "articleMapper", articles);
        ReflectionTestUtils.setField(service, "userStatsMapper", stats);
        Article article = new Article(); article.setArticleId(3L); article.setUserId(2L); article.setStatus(2);
        when(articles.selectArticleById(3L)).thenReturn(article);
    }

    @Test void invalidAmountsNeverChangeBalances() {
        for (Double amount : new Double[]{null, -1d, 0d, Double.NaN, Double.POSITIVE_INFINITY, 0.5d, 10001d}) {
            assertThrows(WeebException.class, () -> service.addCoin(3L, amount, 1L));
        }
        verifyNoInteractions(stats);
    }

    @Test void insufficientFundsCannotCreditAnArticle() {
        when(stats.deductWebsiteCoins(1L, 5L)).thenReturn(0);
        assertThrows(WeebException.class, () -> service.addCoin(3L, 5d, 1L));
        verify(articles, never()).addCoin(anyLong(), anyDouble());
    }

    @Test void debitPrecedesCreditAndFailedCreditRaisesRollbackException() {
        when(stats.deductWebsiteCoins(1L, 5L)).thenReturn(1);
        when(articles.addCoin(3L, 5d)).thenReturn(1);
        assertTrue(service.addCoin(3L, 5d, 1L));
        var order = inOrder(stats, articles);
        order.verify(stats).deductWebsiteCoins(1L, 5L);
        order.verify(articles).addCoin(3L, 5d);
        when(articles.addCoin(3L, 5d)).thenReturn(0);
        assertThrows(WeebException.class, () -> service.addCoin(3L, 5d, 1L));
    }
}
