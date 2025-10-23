package com.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class PasswordHashTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testPasswordHash() {
        // 测试用户密码哈希生成
        String[] testPasswords = {"test123", "password100", "password200", "password300", "password400", "password500"};

        System.out.println("=== BCrypt密码哈希生成测试 ===");

        for (String password : testPasswords) {
            String hash = passwordEncoder.encode(password);
            boolean matches = passwordEncoder.matches(password, hash);

            System.out.println("密码: " + password);
            System.out.println("哈希: " + hash);
            System.out.println("验证: " + (matches ? "✓ 正确" : "✗ 错误"));
            System.out.println("---");
        }

        // 验证错误的哈希值
        String wrongHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKV6biekKXsE6X5w5R2fK5U2gO6";
        boolean wrongMatches = passwordEncoder.matches("test123", wrongHash);
        System.out.println("错误哈希验证 test123: " + (wrongMatches ? "✗ 意外匹配" : "✓ 正确不匹配"));
    }
}
