package com.web.repository;

import com.web.model.elasticsearch.MessageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 消息搜索的Elasticsearch Repository
 * 提供MessageDocument的CRUD和搜索操作
 * 简化注释：ES消息Repository
 */
@Repository
public interface MessageSearchRepository extends ElasticsearchRepository<MessageDocument, Long> {
    // Spring Data Elasticsearch会自动实现基本的CRUD操作
    // 如果需要，还可以基于方法名创建派生查询方法
    // 例如：
    // Page<MessageDocument> findByContentContaining(String content, Pageable pageable);
}
