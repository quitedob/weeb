package com.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = {"com.web"})
@MapperScan("com.web.mapper")
public class WeebApplication {

	public static void main(String[] args) {
		// 生产环境配置检查
		validateProductionEnvironment();

		SpringApplication.run(WeebApplication.class, args);
	}

	/**
	 * 生产环境配置验证
	 * 在生产环境中强制验证关键环境变量是否已设置，避免安全风险
	 */
	private static void validateProductionEnvironment() {
		String activeProfile = System.getProperty("spring.profiles.active",
			System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));

		if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
			System.out.println("检测到生产环境，开始验证关键配置...");

			// 验证JWT密钥
			String jwtSecret = System.getenv("JWT_SECRET");
			if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
				throw new IllegalStateException(
					"生产环境缺少必要的JWT密钥环境变量: JWT_SECRET。请设置环境变量 JWT_SECRET=your_secure_jwt_secret_key");
			}

			// 验证数据库密码
			String dbPassword = System.getenv("MYSQL_PASSWORD");
			if (dbPassword == null || dbPassword.trim().isEmpty()) {
				throw new IllegalStateException(
					"生产环境缺少必要的数据库密码环境变量: MYSQL_PASSWORD。请设置环境变量 MYSQL_PASSWORD=your_mysql_password");
			}

			// 验证数据库URL（可选，但推荐设置）
			String dbUrl = System.getenv("MYSQL_URL");
			if (dbUrl == null || dbUrl.trim().isEmpty()) {
				System.out.println("警告: 未设置MYSQL_URL环境变量，将使用application.yml中的默认配置");
			}

			// 验证数据库用户名（可选，但推荐设置）
			String dbUsername = System.getenv("MYSQL_USERNAME");
			if (dbUsername == null || dbUsername.trim().isEmpty()) {
				System.out.println("警告: 未设置MYSQL_USERNAME环境变量，将使用application.yml中的默认配置");
			}

			System.out.println("✅ 生产环境配置验证通过");
		} else {
			System.out.println("开发环境，跳过生产环境配置验证");
		}
	}

}
