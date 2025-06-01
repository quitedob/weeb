package com.web;

import com.web.service.ArticleService;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootTest
class WeebApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ArticleService articleService;

	/**
	 * 测试数据库连接是否成功
	 */
	@PostConstruct
	public void testConnection() {
		try (Connection connection = dataSource.getConnection()) {
			System.out.println("数据库连接成功！");
		} catch (Exception e) {
			System.err.println("数据库连接失败：" + e.getMessage());
		}
	}

	/**
	 * 测试订阅功能
	 */


	@Test
	void contextLoads() {
		// 其他上下文测试逻辑
	}
}
