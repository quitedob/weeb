package com.web.service;

import com.web.model.elasticsearch.MessageDocument;

import java.util.List;
import java.util.Map;

/**
 * Elasticsearch搜索服务接口
 * 提供基于Elasticsearch的全文搜索功能
 */
public interface ElasticsearchSearchService {

    // ==================== 消息搜索 ====================

    /**
     * 索引消息到Elasticsearch
     * @param messageDocument 消息文档
     */
    void indexMessage(MessageDocument messageDocument);

    /**
     * 批量索引消息到Elasticsearch
     * @param messageDocuments 消息文档列表
     */
    void bulkIndexMessages(List<MessageDocument> messageDocuments);

    /**
     * 搜索消息内容
     * @param keyword 搜索关键词
     * @param fromUserId 发送者ID（可选）
     * @param chatListId 聊天ID（可选）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 搜索结果
     */
    Map<String, Object> searchMessages(String keyword, Long fromUserId, Long chatListId, int page, int size);

    /**
     * 根据消息ID搜索
     * @param messageId 消息ID
     * @return 消息文档
     */
    MessageDocument searchMessageById(Long messageId);

    /**
     * 删除消息索引
     * @param messageId 消息ID
     */
    void deleteMessage(Long messageId);

    /**
     * 根据聊天ID删除所有消息索引
     * @param chatListId 聊天ID
     */
    void deleteMessagesByChatList(Long chatListId);

    // ==================== 索引管理 ====================

    /**
     * 创建消息索引
     */
    void createMessageIndex();

    /**
     * 删除消息索引
     */
    void deleteMessageIndex();

    /**
     * 检查索引是否存在
     * @return 索引是否存在
     */
    boolean messageIndexExists();

    /**
     * 重建消息索引（重新索引所有消息）
     */
    void rebuildMessageIndex();

    // ==================== 高级搜索 ====================

    /**
     * 高级消息搜索（支持复杂查询条件）
     * @param keyword 搜索关键词
     * @param filters 过滤条件
     * @param sort 排序条件
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Map<String, Object> advancedSearchMessages(String keyword, Map<String, Object> filters,
                                               Map<String, String> sort, int page, int size);

    /**
     * 搜索建议（自动补全）
     * @param prefix 前缀
     * @param size 建议数量
     * @return 搜索建议列表
     */
    List<String> searchSuggestions(String prefix, int size);

    /**
     * 获取搜索统计信息
     * @return 统计信息
     */
    Map<String, Object> getSearchStatistics();
}