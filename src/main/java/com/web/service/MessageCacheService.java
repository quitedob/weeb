package com.web.service;

import com.web.model.Message;
import java.util.List;

/**
 * 消息缓存服务接口
 */
public interface MessageCacheService {

    /**
     * 缓存消息
     * @param message 消息对象
     */
    void cacheMessage(Message message);

    /**
     * 获取缓存的消息
     * @param messageId 消息ID
     * @return 消息对象
     */
    Message getCachedMessage(Long messageId);

    /**
     * 缓存消息列表
     * @param chatId 会话ID（可以是String或Long）
     * @param messages 消息列表
     */
    void cacheMessageList(Object chatId, List<Message> messages);

    /**
     * 获取缓存的消息列表
     * @param chatId 会话ID（可以是String或Long）
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    List<Message> getCachedMessageList(Object chatId, int page, int size);

    /**
     * 清除消息缓存
     * @param messageId 消息ID
     */
    void evictMessage(Long messageId);

    /**
     * 清除会话消息列表缓存
     * @param chatId 会话ID（可以是String或Long）
     */
    void evictMessageList(Object chatId);

    /**
     * 预加载消息
     * @param chatId 会话ID（可以是String或Long）
     * @param page 页码
     * @param size 每页大小
     */
    void preloadMessages(Object chatId, int page, int size);

    /**
     * 获取缓存统计信息
     * @return 统计信息
     */
    java.util.Map<String, Object> getCacheStatistics();
}
