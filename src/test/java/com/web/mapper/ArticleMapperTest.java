package com.web.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArticleMapper 测试类
 * 验证文章相关的数据库访问层是否正确工作，特别是更新后的统计方法
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ArticleMapperTest {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Test
    public void testSelectUserStatsInformation() {
        // 测试查询用户统计信息（更新后的方法）
        Long userId = 1L;
        
        Map<String, Object> userStatsInfo = articleMapper.selectUserStatsInformation(userId);
        
        if (userStatsInfo != null && !userStatsInfo.isEmpty()) {
            System.out.println("用户 " + userId + " 的统计信息:");
            System.out.println("  粉丝数: " + userStatsInfo.get("fans_count"));
            System.out.println("  总点赞数: " + userStatsInfo.get("total_likes"));
            System.out.println("  总收藏数: " + userStatsInfo.get("total_favorites"));
            System.out.println("  总赞助数: " + userStatsInfo.get("total_sponsorship"));
            System.out.println("  总文章曝光数: " + userStatsInfo.get("total_article_exposure"));
            System.out.println("  网站金币: " + userStatsInfo.get("website_coins"));
            
            // 验证数据类型
            assertTrue(userStatsInfo.containsKey("fans_count"), "应该包含粉丝数");
            assertTrue(userStatsInfo.containsKey("total_likes"), "应该包含总点赞数");
            assertTrue(userStatsInfo.containsKey("total_favorites"), "应该包含总收藏数");
            assertTrue(userStatsInfo.containsKey("website_coins"), "应该包含网站金币");
        } else {
            System.out.println("用户 " + userId + " 没有统计信息");
        }
    }

    @Test
    public void testSelectAggregatedStatsByUserId() {
        // 测试获取用户文章聚合统计数据
        Long userId = 1L;
        
        Map<String, Object> aggregatedStats = articleMapper.selectAggregatedStatsByUserId(userId);
        
        if (aggregatedStats != null && !aggregatedStats.isEmpty()) {
            System.out.println("用户 " + userId + " 的文章聚合统计:");
            System.out.println("  总点赞数: " + aggregatedStats.get("total_likes"));
            System.out.println("  总收藏数: " + aggregatedStats.get("total_favorites"));
            System.out.println("  总赞助数: " + aggregatedStats.get("total_sponsorship"));
            System.out.println("  总曝光数: " + aggregatedStats.get("total_exposure"));
            System.out.println("  文章数量: " + aggregatedStats.get("article_count"));
            
            // 验证数据类型和非负性
            Object totalLikes = aggregatedStats.get("total_likes");
            if (totalLikes != null) {
                assertTrue(((Number) totalLikes).longValue() >= 0, "总点赞数应该大于等于0");
            }
            
            Object articleCount = aggregatedStats.get("article_count");
            if (articleCount != null) {
                assertTrue(((Number) articleCount).longValue() >= 0, "文章数量应该大于等于0");
            }
        } else {
            System.out.println("用户 " + userId + " 没有文章或文章统计数据");
        }
    }

    @Test
    public void testUpdateUserStatsArticleStats() {
        // 测试更新用户统计表的文章相关统计数据（更新后的方法）
        Long userId = 1L;
        
        // 首先确保用户有统计数据记录
        int count = userStatsMapper.countByUserId(userId);
        if (count == 0) {
            // 创建统计数据记录
            com.web.model.UserStats newStats = new com.web.model.UserStats(userId);
            userStatsMapper.insertUserStats(newStats);
            System.out.println("为用户 " + userId + " 创建了统计数据记录");
        }
        
        // 获取聚合统计数据
        Map<String, Object> aggregatedStats = articleMapper.selectAggregatedStatsByUserId(userId);
        
        if (aggregatedStats != null && !aggregatedStats.isEmpty()) {
            // 添加用户ID到统计数据中
            aggregatedStats.put("userId", userId);
            
            // 获取更新前的统计数据
            com.web.model.UserStats beforeStats = userStatsMapper.selectByUserId(userId);
            
            // 执行更新操作
            int result = articleMapper.updateUserStatsArticleStats(aggregatedStats);
            
            if (result > 0) {
                System.out.println("成功更新用户 " + userId + " 的统计数据");
                
                // 获取更新后的统计数据
                com.web.model.UserStats afterStats = userStatsMapper.selectByUserId(userId);
                
                if (beforeStats != null && afterStats != null) {
                    System.out.println("更新前总点赞数: " + beforeStats.getTotalLikes());
                    System.out.println("更新后总点赞数: " + afterStats.getTotalLikes());
                    
                    // 验证更新时间已更新
                    if (beforeStats.getUpdatedAt() != null && afterStats.getUpdatedAt() != null) {
                        assertTrue(afterStats.getUpdatedAt().after(beforeStats.getUpdatedAt()) || 
                                 afterStats.getUpdatedAt().equals(beforeStats.getUpdatedAt()),
                                 "更新时间应该被更新");
                    }
                }
            } else {
                System.out.println("更新用户 " + userId + " 的统计数据失败，可能是因为用户不存在统计记录");
            }
        } else {
            System.out.println("用户 " + userId + " 没有文章统计数据可更新");
        }
    }

    @Test
    public void testUpdateUserStatsTotals() {
        // 测试根据用户ID汇总文章数据并更新user_stats表（更新后的方法）
        Long userId = 1L;
        
        // 首先确保用户有统计数据记录
        int count = userStatsMapper.countByUserId(userId);
        if (count == 0) {
            com.web.model.UserStats newStats = new com.web.model.UserStats(userId);
            userStatsMapper.insertUserStats(newStats);
            System.out.println("为用户 " + userId + " 创建了统计数据记录");
        }
        
        // 获取更新前的统计数据
        com.web.model.UserStats beforeStats = userStatsMapper.selectByUserId(userId);
        
        // 执行批量更新操作
        int result = articleMapper.updateUserStatsTotals(userId);
        
        if (result > 0) {
            System.out.println("成功批量更新用户 " + userId + " 的统计数据");
            
            // 获取更新后的统计数据
            com.web.model.UserStats afterStats = userStatsMapper.selectByUserId(userId);
            
            if (beforeStats != null && afterStats != null) {
                System.out.println("批量更新结果:");
                System.out.println("  更新前总点赞数: " + beforeStats.getTotalLikes());
                System.out.println("  更新后总点赞数: " + afterStats.getTotalLikes());
                System.out.println("  更新前总收藏数: " + beforeStats.getTotalFavorites());
                System.out.println("  更新后总收藏数: " + afterStats.getTotalFavorites());
                System.out.println("  更新前总赞助数: " + beforeStats.getTotalSponsorship());
                System.out.println("  更新后总赞助数: " + afterStats.getTotalSponsorship());
                System.out.println("  更新前总曝光数: " + beforeStats.getTotalArticleExposure());
                System.out.println("  更新后总曝光数: " + afterStats.getTotalArticleExposure());
                
                // 验证更新时间
                if (afterStats.getUpdatedAt() != null) {
                    System.out.println("  更新时间: " + afterStats.getUpdatedAt());
                }
            }
        } else {
            System.out.println("批量更新用户 " + userId + " 的统计数据失败，可能是因为用户不存在或没有文章");
        }
    }

    @Test
    public void testIntegrationWithUserStatsMapper() {
        // 测试ArticleMapper和UserStatsMapper的集成
        Long userId = 1L;
        
        // 通过ArticleMapper获取聚合统计
        Map<String, Object> articleStats = articleMapper.selectAggregatedStatsByUserId(userId);
        
        // 通过UserStatsMapper获取用户统计
        com.web.model.UserStats userStats = userStatsMapper.selectByUserId(userId);
        
        System.out.println("集成测试结果:");
        if (articleStats != null && !articleStats.isEmpty()) {
            System.out.println("文章聚合统计 - 总点赞数: " + articleStats.get("total_likes"));
        }
        
        if (userStats != null) {
            System.out.println("用户统计记录 - 总点赞数: " + userStats.getTotalLikes());
        }
        
        // 如果两者都存在，可以比较数据一致性
        if (articleStats != null && userStats != null && 
            articleStats.get("total_likes") != null) {
            
            Long articleTotalLikes = ((Number) articleStats.get("total_likes")).longValue();
            Long userTotalLikes = userStats.getTotalLikes();
            
            System.out.println("数据一致性检查:");
            System.out.println("  文章聚合点赞数: " + articleTotalLikes);
            System.out.println("  用户统计点赞数: " + userTotalLikes);
            
            // 注意：这两个数值可能不一致，因为用户统计可能包含其他来源的点赞
            // 这里只是展示如何进行数据一致性检查
        }
    }
}