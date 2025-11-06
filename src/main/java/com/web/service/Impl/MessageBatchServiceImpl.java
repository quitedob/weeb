package com.web.service.Impl;

import com.web.mapper.MessageMapper;
import com.web.model.Message;
import com.web.service.MessageBatchService;
import com.web.service.MessageBroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 消息批处理服务实现
 * 实现消息的批量存储和推送，提高系统吞吐量
 */
@Slf4j
@Service
public class MessageBatchServiceImpl implements MessageBatchService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageBroadcastService messageBroadcastService;

    // 批处理队列，容量2000
    private final BlockingQueue<Message> batchQueue = new LinkedBlockingQueue<>(2000);

    // 批处理阈值：100条消息触发批量保存
    private static final int BATCH_THRESHOLD = 100;

    // 批处理锁，防止并发问题
    private final ReentrantLock batchLock = new ReentrantLock();

    /**
     * 批量保存消息到数据库
     * 使用MyBatis-Plus的批量插入功能
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        try {
            // 使用MyBatis-Plus的批量插入
            int savedCount = 0;
            for (Message message : messages) {
                int result = messageMapper.insert(message);
                if (result > 0) {
                    savedCount++;
                }
            }

            log.info("批量保存消息成功，共保存 {} 条消息", savedCount);
            return savedCount;

        } catch (Exception e) {
            log.error("批量保存消息失败", e);
            throw new RuntimeException("批量保存消息失败: " + e.getMessage());
        }
    }

    /**
     * 批量推送群组消息
     * 使用WebSocket批量推送，避免循环单独推送
     */
    @Override
    public void batchPushGroupMessage(Long groupId, Message message, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            log.warn("群组成员列表为空，无法推送消息");
            return;
        }

        try {
            // 使用MessageBroadcastService的批量推送功能
            messageBroadcastService.broadcastMessageToGroup(message, groupId);
            
            log.info("批量推送群组消息成功，群组ID: {}, 成员数: {}", groupId, memberIds.size());

        } catch (Exception e) {
            log.error("批量推送群组消息失败，群组ID: {}", groupId, e);
        }
    }

    /**
     * 添加消息到批处理队列
     * 当队列达到阈值时自动触发批量保存
     */
    @Override
    public void addToBatchQueue(Message message) {
        try {
            boolean added = batchQueue.offer(message);
            
            if (!added) {
                log.warn("批处理队列已满，立即触发批量保存");
                flushBatchQueue();
                // 重试添加
                batchQueue.offer(message);
            }

            // 检查是否达到批处理阈值
            if (batchQueue.size() >= BATCH_THRESHOLD) {
                log.info("批处理队列达到阈值 {}，触发批量保存", BATCH_THRESHOLD);
                flushBatchQueue();
            }

        } catch (Exception e) {
            log.error("添加消息到批处理队列失败", e);
        }
    }

    /**
     * 强制刷新批处理队列
     * 立即保存队列中的所有消息
     */
    @Override
    public int flushBatchQueue() {
        batchLock.lock();
        try {
            if (batchQueue.isEmpty()) {
                return 0;
            }

            // 从队列中取出所有消息
            List<Message> messagesToSave = new ArrayList<>();
            batchQueue.drainTo(messagesToSave);

            if (messagesToSave.isEmpty()) {
                return 0;
            }

            // 批量保存
            int savedCount = batchSaveMessages(messagesToSave);
            
            log.info("刷新批处理队列完成，保存 {} 条消息", savedCount);
            return savedCount;

        } catch (Exception e) {
            log.error("刷新批处理队列失败", e);
            return 0;
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * 获取当前批处理队列大小
     */
    @Override
    public int getBatchQueueSize() {
        return batchQueue.size();
    }

    /**
     * 定时任务：每1秒检查并刷新批处理队列
     * 确保消息不会在队列中停留太久
     */
    @Scheduled(fixedDelay = 1000)
    public void scheduledFlush() {
        if (!batchQueue.isEmpty()) {
            log.debug("定时任务触发批处理队列刷新，当前队列大小: {}", batchQueue.size());
            flushBatchQueue();
        }
    }
}
