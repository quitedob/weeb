package com.web.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import com.web.model.elasticsearch.MessageDocument;
import com.web.repository.MessageSearchRepository;
import com.web.service.ElasticsearchSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch搜索服务实现类
 * 提供基于Elasticsearch的全文搜索功能
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchSearchServiceImpl implements ElasticsearchSearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private MessageSearchRepository messageSearchRepository;

  
    private static final String MESSAGE_INDEX = "message";

    // ==================== 消息搜索 ====================

    @Override
    public void indexMessage(MessageDocument messageDocument) {
        try {
            IndexRequest<MessageDocument> request = IndexRequest.of(i -> i
                    .index(MESSAGE_INDEX)
                    .id(messageDocument.getId().toString())
                    .document(messageDocument));

            IndexResponse response = elasticsearchClient.index(request);
            log.debug("消息索引成功: messageId={}, result={}",
                    messageDocument.getId(), response.result());
        } catch (IOException e) {
            log.error("消息索引失败: messageId={}", messageDocument.getId(), e);
        }
    }

    @Override
    public void bulkIndexMessages(List<MessageDocument> messageDocuments) {
        if (messageDocuments == null || messageDocuments.isEmpty()) {
            return;
        }

        try {
            List<BulkOperation> operations = messageDocuments.stream()
                    .map(doc -> BulkOperation.of(op -> op
                            .index(i -> i
                                    .index(MESSAGE_INDEX)
                                    .id(doc.getId().toString())
                                    .document(doc))))
                    .collect(Collectors.toList());

            BulkRequest request = BulkRequest.of(br -> br.operations(operations));
            BulkResponse response = elasticsearchClient.bulk(request);

            if (response.errors()) {
                log.warn("批量索引消息时出现错误: {}", response.toString());
            } else {
                log.debug("批量索引消息成功: count={}", messageDocuments.size());
            }
        } catch (IOException e) {
            log.error("批量索引消息失败: count={}", messageDocuments.size(), e);
        }
    }

    @Override
    public Map<String, Object> searchMessages(String keyword, Long fromUserId, Long chatListId,
                                              int page, int size) {
        try {
            // 构建查询条件
            BoolQuery boolQuery = BoolQuery.of(b -> {
                // 关键词搜索
                if (keyword != null && !keyword.trim().isEmpty()) {
                    b.must(m -> m.match(MatchQuery.of(mq -> mq
                            .field("content")
                            .query(keyword.trim())
                            .operator(Operator.And))));
                }

                // 发送者过滤
                if (fromUserId != null) {
                    b.filter(f -> f.term(TermQuery.of(t -> t
                            .field("fromId")
                            .value(fromUserId))));
                }

                // 聊天过滤
                if (chatListId != null) {
                    b.filter(f -> f.term(TermQuery.of(t -> t
                            .field("chatListId")
                            .value(chatListId))));
                }

                return b;
            });

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(MESSAGE_INDEX)
                    .query(boolQuery._toQuery())
                    .from(page * size)
                    .size(size)
                    .sort(SortOptions.of(so -> so
                            .field(f -> f.field("sendTime").order(SortOrder.Desc)))));

            SearchResponse<MessageDocument> response = elasticsearchClient.search(searchRequest, MessageDocument.class);

            // 处理搜索结果
            List<MessageDocument> documents = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("documents", documents);
            result.put("total", response.hits().total().value());
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (long) Math.ceil((double) response.hits().total().value() / size));

            log.debug("消息搜索完成: keyword={}, total={}", keyword, response.hits().total().value());
            return result;

        } catch (IOException e) {
            log.error("消息搜索失败: keyword={}", keyword, e);
            return createEmptySearchResult(page, size);
        }
    }

    @Override
    public MessageDocument searchMessageById(Long messageId) {
        try {
            GetRequest getRequest = GetRequest.of(g -> g
                    .index(MESSAGE_INDEX)
                    .id(messageId.toString()));

            GetResponse<MessageDocument> response = elasticsearchClient.get(getRequest, MessageDocument.class);

            if (response.found()) {
                return response.source();
            }
        } catch (IOException e) {
            log.error("根据ID搜索消息失败: messageId={}", messageId, e);
        }
        return null;
    }

    @Override
    public void deleteMessage(Long messageId) {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(d -> d
                    .index(MESSAGE_INDEX)
                    .id(messageId.toString()));

            DeleteResponse response = elasticsearchClient.delete(deleteRequest);
            log.debug("删除消息索引: messageId={}, result={}", messageId, response.result());
        } catch (IOException e) {
            log.error("删除消息索引失败: messageId={}", messageId, e);
        }
    }

    @Override
    public void deleteMessagesByChatList(Long chatListId) {
        try {
            // 先搜索该聊天的所有消息
            Map<String, Object> searchResult = searchMessages(null, null, chatListId, 0, 10000);

            @SuppressWarnings("unchecked")
            List<MessageDocument> documents = (List<MessageDocument>) searchResult.get("documents");

            if (!documents.isEmpty()) {
                // 批量删除
                List<BulkOperation> operations = documents.stream()
                        .map(doc -> BulkOperation.of(op -> op
                                .delete(d -> d
                                        .index(MESSAGE_INDEX)
                                        .id(doc.getId().toString()))))
                        .collect(Collectors.toList());

                BulkRequest bulkRequest = BulkRequest.of(br -> br.operations(operations));
                BulkResponse response = elasticsearchClient.bulk(bulkRequest);

                log.debug("批量删除消息索引: chatListId={}, count={}, success={}",
                        chatListId, documents.size(), !response.errors());
            }
        } catch (IOException e) {
            log.error("批量删除消息索引失败: chatListId={}", chatListId, e);
        }
    }

    // ==================== 索引管理 ====================

    @Override
    public void createMessageIndex() {
        try {
            CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                    .index(MESSAGE_INDEX)
                    .mappings(m -> m
                            .properties("id", p -> p.long_(l -> l))
                            .properties("fromId", p -> p.long_(l -> l))
                            .properties("chatListId", p -> p.long_(l -> l))
                            .properties("content", p -> p.text(t -> t
                                    .analyzer("standard")
                                    .searchAnalyzer("standard")))
                            .properties("sendTime", p -> p.date(d -> d))));

            elasticsearchClient.indices().create(createRequest);
            log.info("创建消息索引成功: {}", MESSAGE_INDEX);
        } catch (IOException e) {
            log.error("创建消息索引失败: {}", MESSAGE_INDEX, e);
        }
    }

    @Override
    public void deleteMessageIndex() {
        try {
            DeleteIndexRequest deleteRequest = DeleteIndexRequest.of(d -> d.index(MESSAGE_INDEX));
            elasticsearchClient.indices().delete(deleteRequest);
            log.info("删除消息索引成功: {}", MESSAGE_INDEX);
        } catch (IOException e) {
            log.error("删除消息索引失败: {}", MESSAGE_INDEX, e);
        }
    }

    @Override
    public boolean messageIndexExists() {
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(MESSAGE_INDEX));
            return elasticsearchClient.indices().exists(existsRequest).value();
        } catch (IOException e) {
            log.error("检查索引存在性失败: {}", MESSAGE_INDEX, e);
            return false;
        }
    }

    @Override
    public void rebuildMessageIndex() {
        log.info("开始重建消息索引...");

        try {
            // 删除旧索引
            if (messageIndexExists()) {
                deleteMessageIndex();
            }

            // 创建新索引
            createMessageIndex();

            // 这里可以从数据库重新加载所有消息并索引
            // 为了避免内存问题，建议分批处理
            log.info("消息索引重建完成");

        } catch (Exception e) {
            log.error("重建消息索引失败", e);
        }
    }

    // ==================== 高级搜索 ====================

    @Override
    public Map<String, Object> advancedSearchMessages(String keyword, Map<String, Object> filters,
                                                        Map<String, String> sort, int page, int size) {
        // 实现高级搜索逻辑
        // 这里可以根据需要扩展更复杂的查询条件
        return searchMessages(keyword,
                filters != null ? (Long) filters.get("fromUserId") : null,
                filters != null ? (Long) filters.get("chatListId") : null,
                page, size);
    }

    @Override
    public List<String> searchSuggestions(String prefix, int size) {
        // 实现搜索建议功能
        // 可以使用ES的完成建议器
        return List.of();
    }

    @Override
    public Map<String, Object> getSearchStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            if (messageIndexExists()) {
                // 获取索引统计信息
                stats.put("indexExists", true);
                // 这里可以添加更多统计信息
            } else {
                stats.put("indexExists", false);
            }
        } catch (Exception e) {
            log.error("获取搜索统计信息失败", e);
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> createEmptySearchResult(int page, int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("documents", List.of());
        result.put("total", 0L);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", 0L);
        return result;
    }
}