package com.web.service;

import com.web.model.MessageThread;
import java.util.List;
import java.util.Map;

/**
 * 消息线索服务接口
 */
public interface MessageThreadService {

    /**
     * 创建消息线索
     * @param rootMessageId 原始消息ID
     * @param title 线索标题
     * @param createdBy 创建者ID
     * @return 创建的线索
     */
    MessageThread createThread(Long rootMessageId, String title, Long createdBy);

    /**
     * 获取线索详情
     * @param threadId 线索ID
     * @return 线索详情
     */
    MessageThread getThreadById(Long threadId);

    /**
     * 获取线索中的消息列表
     * @param threadId 线索ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 消息列表和分页信息
     */
    Map<String, Object> getThreadMessages(Long threadId, int page, int pageSize);

    /**
     * 回复消息到线索
     * @param threadId 线索ID
     * @param replyMessage 回复消息内容
     * @param userId 回复者ID
     * @return 新创建的消息
     */
    Map<String, Object> replyToThread(Long threadId, String replyMessage, Long userId);

    /**
     * 加入线索参与者
     * @param threadId 线索ID
     * @param userId 用户ID
     * @return 是否成功加入
     */
    boolean joinThread(Long threadId, Long userId);

    /**
     * 离开线索
     * @param threadId 线索ID
     * @param userId 用户ID
     * @return 是否成功离开
     */
    boolean leaveThread(Long threadId, Long userId);

    /**
     * 归档线索
     * @param threadId 线索ID
     * @param userId 操作者ID
     * @return 是否成功归档
     */
    boolean archiveThread(Long threadId, Long userId);

    /**
     * 关闭线索
     * @param threadId 线索ID
     * @param userId 操作者ID
     * @return 是否成功关闭
     */
    boolean closeThread(Long threadId, Long userId);

    /**
     * 置顶线索
     * @param threadId 线索ID
     * @param userId 操作者ID
     * @param isPinned 是否置顶
     * @return 是否成功
     */
    boolean pinThread(Long threadId, Long userId, boolean isPinned);

    /**
     * 锁定线索
     * @param threadId 线索ID
     * @param userId 操作者ID
     * @param isLocked 是否锁定
     * @return 是否成功
     */
    boolean lockThread(Long threadId, Long userId, boolean isLocked);

    /**
     * 获取用户参与的线索列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 线索列表和分页信息
     */
    Map<String, Object> getUserThreads(Long userId, int page, int pageSize);

    /**
     * 获取活跃线索列表
     * @param page 页码
     * @param pageSize 每页大小
     * @return 线索列表和分页信息
     */
    Map<String, Object> getActiveThreads(int page, int pageSize);

    /**
     * 获取用户创建的线索列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 线索列表和分页信息
     */
    Map<String, Object> getUserCreatedThreads(Long userId, int page, int pageSize);

    /**
     * 搜索线索
     * @param keyword 搜索关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    Map<String, Object> searchThreads(String keyword, int page, int pageSize);

    /**
     * 获取线索统计信息
     * @param threadId 线索ID
     * @return 统计信息
     */
    Map<String, Object> getThreadStatistics(Long threadId);

    /**
     * 获取用户在指定消息中的线索上下文
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 线索上下文信息
     */
    Map<String, Object> getThreadContext(Long messageId, Long userId);
}