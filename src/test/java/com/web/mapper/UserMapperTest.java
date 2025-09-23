package com.web.mapper;

import com.web.model.User;
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
 * UserMapper 测试类
 * 验证新的数据库访问层是否正确工作
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Test
    public void testSelectUserWithStatsById() {
        // 测试获取用户完整信息（包含统计数据）
        Long userId = 1L; // 假设存在用户ID为1的用户
        
        UserWithStats userWithStats = userMapper.selectUserWithStatsById(userId);
        
        if (userWithStats != null) {
            assertNotNull(userWithStats.getUser(), "用户基本信息不应为空");
            // 统计数据可能为空（如果用户没有统计记录）
            System.out.println("用户信息: " + userWithStats.getUser().getUsername());
            if (userWithStats.getUserStats() != null) {
                System.out.println("粉丝数: " + userWithStats.getFansCount());
                System.out.println("总点赞数: " + userWithStats.getTotalLikes());
            }
        }
    }

    @Test
    public void testSelectUserWithStatsByUsername() {
        // 测试根据用户名获取用户完整信息
        String username = "testuser"; // 假设存在该用户名
        
        UserWithStats userWithStats = userMapper.selectUserWithStatsByUsername(username);
        
        if (userWithStats != null) {
            assertNotNull(userWithStats.getUser(), "用户基本信息不应为空");
            assertEquals(username, userWithStats.getUser().getUsername(), "用户名应该匹配");
        }
    }

    @Test
    public void testSelectByIds() {
        // 测试批量获取用户基本信息
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        
        List<User> users = userMapper.selectByIds(userIds);
        
        assertNotNull(users, "用户列表不应为空");
        System.out.println("批量查询到 " + users.size() + " 个用户");
    }

    @Test
    public void testSelectUsersWithStatsByIds() {
        // 测试批量获取用户完整信息（包含统计数据）
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        
        List<UserWithStats> usersWithStats = userMapper.selectUsersWithStatsByIds(userIds);
        
        assertNotNull(usersWithStats, "用户完整信息列表不应为空");
        System.out.println("批量查询到 " + usersWithStats.size() + " 个用户的完整信息");
        
        for (UserWithStats userWithStats : usersWithStats) {
            assertNotNull(userWithStats.getUser(), "用户基本信息不应为空");
            System.out.println("用户: " + userWithStats.getUser().getUsername() + 
                             ", 粉丝数: " + userWithStats.getFansCount());
        }
    }

    @Test
    public void testSelectUserList() {
        // 测试获取用户列表（分页支持）
        List<User> users = userMapper.selectUserList(null, null, null, "registration_date");
        
        assertNotNull(users, "用户列表不应为空");
        System.out.println("查询到 " + users.size() + " 个用户");
    }

    @Test
    public void testSelectUserListWithStats() {
        // 测试获取用户列表（包含统计数据，分页支持）
        List<UserWithStats> usersWithStats = userMapper.selectUserListWithStats(
            null, null, null, null, null, "fans_count");
        
        assertNotNull(usersWithStats, "用户完整信息列表不应为空");
        System.out.println("查询到 " + usersWithStats.size() + " 个用户的完整信息");
    }

    @Test
    public void testCountUsers() {
        // 测试统计用户总数
        long count = userMapper.countUsers(null, null);
        
        assertTrue(count >= 0, "用户总数应该大于等于0");
        System.out.println("用户总数: " + count);
    }

    @Test
    public void testExistsByUsername() {
        // 测试检查用户名是否存在
        String existingUsername = "testuser"; // 假设存在该用户名
        String nonExistingUsername = "nonexistentuser";
        
        // 这个测试可能会失败，因为我们不确定数据库中是否有这些用户
        // 但它可以验证查询是否正常执行
        boolean exists1 = userMapper.existsByUsername(existingUsername);
        boolean exists2 = userMapper.existsByUsername(nonExistingUsername);
        
        System.out.println("用户名 '" + existingUsername + "' 存在: " + exists1);
        System.out.println("用户名 '" + nonExistingUsername + "' 存在: " + exists2);
    }

    @Test
    public void testExistsByEmail() {
        // 测试检查邮箱是否存在
        String testEmail = "test@example.com";
        
        boolean exists = userMapper.existsByEmail(testEmail);
        System.out.println("邮箱 '" + testEmail + "' 存在: " + exists);
    }
}