package com.web.mapper.elasticsearch;

import com.web.model.elasticsearch.MessageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository; // Standard Spring annotation for repositories

/**
 * 消息搜索的Elasticsearch Repository
 * Provides CRUD and search operations for MessageDocument.
 * 简化注释：ES消息Repository
 */
@Repository
public interface MessageSearchRepository extends ElasticsearchRepository<MessageDocument, Long> {
    // Spring Data Elasticsearch will automatically implement basic CRUD operations
    // and allow for derived query methods based on method names if needed later.
    // For example:
    // Page<MessageDocument> findByContentContaining(String content, Pageable pageable);
}
