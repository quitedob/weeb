package com.web.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 配置类
 * 在 Spring Boot 3.x 中，ElasticsearchTemplate 已被弃用，使用 ElasticsearchOperations
 * Spring Boot 会自动配置 ElasticsearchOperations，无需手动创建
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.web.repository")
public class ElasticsearchConfig {
    // Spring Boot 会自动配置 ElasticsearchOperations
    // 无需手动创建 elasticsearchTemplate Bean
} 