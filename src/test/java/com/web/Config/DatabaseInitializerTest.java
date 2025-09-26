package com.web.Config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // 使用测试环境配置
public class DatabaseInitializerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testDatabaseAndTablesExist() {
        // 测试数据库连接
        assertDoesNotThrow(() -> {
            jdbcTemplate.execute("SELECT 1");
        });

        // 测试核心表是否存在
        String[] tables = {
            "user", "user_stats", "article", "`group`", "group_member",
            "contact", "chat_list", "message", "message_reaction",
            "notifications", "file_transfer", "article_comment",
            "file_record", "user_follow", "file_share"
        };

        for (String table : tables) {
            assertDoesNotThrow(() -> {
                jdbcTemplate.execute("SELECT 1 FROM " + table + " LIMIT 1");
            }, "表 " + table + " 应该存在");
        }
    }

    @Test
    public void testAdminUserExists() {
        // 测试管理员用户是否存在
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user WHERE username = 'admin'", Integer.class);
        
        assertNotNull(count);
        assertTrue(count > 0, "管理员用户应该存在");
    }

    @Test
    public void testUserStatsIntegrity() {
        // 测试用户统计数据的完整性
        Integer userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user", Integer.class);
        Integer statsCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_stats", Integer.class);
        
        assertNotNull(userCount);
        assertNotNull(statsCount);
        
        // 每个用户都应该有对应的统计数据
        assertTrue(statsCount >= 1, "至少应该有管理员的统计数据");
    }

    @Test
    public void testForeignKeyConstraints() {
        // 测试外键约束是否正确设置
        assertDoesNotThrow(() -> {
            // 尝试插入一个引用不存在用户的统计记录，应该失败
            try {
                jdbcTemplate.update(
                    "INSERT INTO user_stats (user_id, website_coins) VALUES (99999, 100)");
                fail("外键约束应该阻止插入不存在的用户ID");
            } catch (Exception e) {
                // 预期的异常，外键约束生效
                assertTrue(e.getMessage().contains("foreign key constraint") || 
                          e.getMessage().contains("Cannot add or update"));
            }
        });
    }
}