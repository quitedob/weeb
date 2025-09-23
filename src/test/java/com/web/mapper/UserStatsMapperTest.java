package com.web.mapper;

import com.web.model.UserStats;
import com.web.model.UserWithStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserStatsMapper 测试类
 * 验证用户统计数据的数据库访问层是否正确工作
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserStatsMapperTest {

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Test
    public void testSelectByUserId() {
        // 测试根据用户ID获取统计数据
        Long userId = 1L; // 假设存在用户ID为1的统计数据
        
        UserStats userStats = userStatsMapper.selectByUserId(userId);
        
        if (userStats != null) {
            assertEquals(userId, userStats.getUserId(), "用户ID应该匹配");
            assertTrue(userStats.getFansCount() >= 0, "粉丝数应该大于等于0");
            assertTrue(userStats.getTotalLikes() >= 0, "总点赞数应该大于等于0");
            System.out.println("用户 " + userId + " 的统计数据:");
            System.out.println("  粉丝数: " + userStats.getFansCount());
            System.out.println("  总点赞数: " + userStats.getTotalLikes());
            System.out.println("  总收藏数: " + userStats.getTotalFavorites());
            System.out.println("  网站金币: " + userStats.getWebsiteCoins());
        } else {
            System.out.println("用户 " + userId + " 没有统计数据");
        }
    }

    @Test
    public void testSelectUserWithStats() {
        // 测试获取用户完整信息（JOIN查询）
        Long userId = 1L;
        
        UserWithStats userWithStats = userStatsMapper.selectUserWithStats(userId);
        
        if (userWithStats != null) {
            assertNotNull(userWithStats.getUser(), "用户基本信息不应为空");
            assertEquals(userId, userWithStats.getUser().getId(), "用户ID应该匹配");
            
            if (userWithStats.getUserStats() != null) {
                assertEquals(userId, userWithStats.getUserStats().getUserId(), "统计数据的用户ID应该匹配");
                System.out.println("用户: " + userWithStats.getUser().getUsername());
                System.out.println("粉丝数: " + userWithStats.getFansCount());
            }
        }
    }

    @Test
    public void testSelectByUserIds() {
        // 测试批量获取用户统计数据
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        
        List<UserStats> userStatsList = userStatsMapper.selectByUserIds(userIds);
        
        assertNotNull(userStatsList, "用户统计数据列表不应为空");
        System.out.println("批量查询到 " + userStatsList.size() + " 个用户的统计数据");
        
        for (UserStats userStats : userStatsList) {
            assertTrue(userIds.contains(userStats.getUserId()), "用户ID应该在查询列表中");
            System.out.println("用户 " + userStats.getUserId() + " 粉丝数: " + userStats.getFansCount());
        }
    }

    @Test
    public void testCountByUserId() {
        // 测试检查用户统计数据是否存在
        Long userId = 1L;
        
        int count = userStatsMapper.countByUserId(userId);
        
        assertTrue(count >= 0 && count <= 1, "统计数据记录数应该是0或1");
        System.out.println("用户 " + userId + " 的统计数据记录数: " + count);
    }

    @Test
    public void testStatisticalOperations() {
        // 测试统计数据的增减操作
        Long userId = 1L;
        
        // 首先检查用户是否有统计数据
        int count = userStatsMapper.countByUserId(userId);
        if (count == 0) {
            // 如果没有统计数据，先创建一个
            UserStats newStats = new UserStats(userId);
            userStatsMapper.insertUserStats(newStats);
            System.out.println("为用户 " + userId + " 创建了新的统计数据记录");
        }
        
        // 获取操作前的数据
        UserStats beforeStats = userStatsMapper.selectByUserId(userId);
        if (beforeStats != null) {
            Long beforeFansCount = beforeStats.getFansCount();
            Long beforeTotalLikes = beforeStats.getTotalLikes();
            
            System.out.println("操作前 - 粉丝数: " + beforeFansCount + ", 总点赞数: " + beforeTotalLikes);
            
            // 测试增加粉丝数
            int result1 = userStatsMapper.incrementFansCount(userId);
            assertTrue(result1 > 0, "增加粉丝数操作应该成功");
            
            // 测试增加点赞数
            int result2 = userStatsMapper.incrementTotalLikes(userId, 5L);
            assertTrue(result2 > 0, "增加点赞数操作应该成功");
            
            // 验证操作结果
            UserStats afterStats = userStatsMapper.selectByUserId(userId);
            if (afterStats != null) {
                assertEquals(beforeFansCount + 1, afterStats.getFansCount(), "粉丝数应该增加1");
                assertEquals(beforeTotalLikes + 5, afterStats.getTotalLikes(), "总点赞数应该增加5");
                
                System.out.println("操作后 - 粉丝数: " + afterStats.getFansCount() + 
                                 ", 总点赞数: " + afterStats.getTotalLikes());
            }
            
            // 测试减少粉丝数
            int result3 = userStatsMapper.decrementFansCount(userId);
            assertTrue(result3 > 0, "减少粉丝数操作应该成功");
            
            UserStats finalStats = userStatsMapper.selectByUserId(userId);
            if (finalStats != null) {
                assertEquals(beforeFansCount, finalStats.getFansCount(), "粉丝数应该回到原来的值");
                System.out.println("最终 - 粉丝数: " + finalStats.getFansCount());
            }
        }
    }

    @Test
    public void testWebsiteCoinsOperations() {
        // 测试网站金币操作
        Long userId = 1L;
        
        // 确保用户有统计数据
        int count = userStatsMapper.countByUserId(userId);
        if (count == 0) {
            UserStats newStats = new UserStats(userId);
            userStatsMapper.insertUserStats(newStats);
        }
        
        UserStats beforeStats = userStatsMapper.selectByUserId(userId);
        if (beforeStats != null) {
            Long beforeCoins = beforeStats.getWebsiteCoins();
            System.out.println("操作前金币数: " + beforeCoins);
            
            // 测试增加金币
            int result1 = userStatsMapper.addWebsiteCoins(userId, 100L);
            assertTrue(result1 > 0, "增加金币操作应该成功");
            
            UserStats afterAddStats = userStatsMapper.selectByUserId(userId);
            if (afterAddStats != null) {
                assertEquals(beforeCoins + 100, afterAddStats.getWebsiteCoins(), "金币数应该增加100");
                System.out.println("增加后金币数: " + afterAddStats.getWebsiteCoins());
                
                // 测试扣除金币（仅在余额足够时）
                int result2 = userStatsMapper.deductWebsiteCoins(userId, 50L);
                if (result2 > 0) {
                    UserStats afterDeductStats = userStatsMapper.selectByUserId(userId);
                    if (afterDeductStats != null) {
                        assertEquals(beforeCoins + 50, afterDeductStats.getWebsiteCoins(), "金币数应该减少50");
                        System.out.println("扣除后金币数: " + afterDeductStats.getWebsiteCoins());
                    }
                }
            }
        }
    }
}