package com.web.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch Repository配置
 * 只有在启用Elasticsearch时才加载
 */
@Configuration
@ConditionalOnProperty(
    value = "elasticsearch.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@EnableElasticsearchRepositories(basePackages = "com.web.repository")
public class ElasticsearchRepositoryConfig {
}