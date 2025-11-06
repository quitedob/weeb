package com.web.service;

import com.web.model.Message;
import java.util.List;

/**
 * 消息批处理服务接口
 * 用于优化消息的批量存储和推送性能
 */
public interface MessageBatchService {

    /**
     * 批量保存消息到数据库
     * 使用批量INSERT语句提高性能
     * 
     * @param messages 消息列表
     * @return 成功保存的消息数量
     */
    int batchSaveMessages(List<Message> messages);

    /**
     * 批量推送群组消息
     * 避免循环单独推送，使用批量推送机制
     * 
     * @param groupId 群组ID
     * @param message 消息内容
     * @param memberIds 群组成员ID列表
     */
    void batchPushGroupMessage(Long groupId, Message message, List<Long> memberIds);

    /**
     * 添加消息到批处理队列
     * 当队列达到阈值或超时时自动触发批量保存
     * 
     * @param message 消息对象
     */
    void addToBatchQueue(Message message);

    /**
     * 强制刷新批处理队列
     * 立即保存队列中的所有消息
     * 
     * @return 保存的消息数量
     */
    int flushBatchQueue();

    /**
     * 获取当前批处理队列大小
     * 
     * @return 队列中的消息数量
     */
    int getBatchQueueSize();
}
