package com.web.service;

import com.web.model.Message;
import com.web.vo.message.SendMessageVo;

/**
 * 消息重试服务接口
 */
public interface MessageRetryService {

    /**
     * 记录失败的消息发送
     * @param sendMessageVo 发送消息VO
     * @param userId 用户ID
     * @param errorMessage 错误信息
     * @return 重试记录ID
     */
    String recordFailedMessage(SendMessageVo sendMessageVo, Long userId, String errorMessage);

    /**
     * 重试发送失败的消息
     * @param retryId 重试记录ID
     * @return 是否成功
     */
    boolean retryFailedMessage(String retryId);

    /**
     * 获取用户的失败消息列表
     * @param userId 用户ID
     * @return 失败消息列表
     */
    java.util.List<java.util.Map<String, Object>> getFailedMessages(Long userId);

    /**
     * 自动重试所有失败的消息
     * @return 重试成功的数量
     */
    int autoRetryFailedMessages();

    /**
     * 清理过期的失败记录
     * @return 清理的数量
     */
    int cleanExpiredFailedRecords();

    /**
     * 获取重试统计信息
     * @return 统计信息
     */
    java.util.Map<String, Object> getRetryStatistics();
}
