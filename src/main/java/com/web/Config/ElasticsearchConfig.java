package com.web.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 配置类
 * 只有在启用Elasticsearch功能时才加载
 */
@Configuration
@ConditionalOnProperty(
    value = "elasticsearch.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@EnableElasticsearchRepositories(basePackages = "com.web.repository")
public class ElasticsearchConfig {
    // Spring Boot 会自动配置 ElasticsearchOperations
    // 无需手动创建 elasticsearchTemplate Bean
} 