package com.web.task;

import com.web.service.MessageRetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 消息重试定时任务
 */
@Slf4j
@Component
public class MessageRetryTask {

    @Autowired
    private MessageRetryService messageRetryService;

    /**
     * 自动重试失败的消息
     * 每2分钟执行一次
     */
    @Scheduled(fixedRate = 120000) // 每2分钟执行一次
    public void autoRetryFailedMessages() {
        try {
            log.debug("开始自动重试失败的消息...");
            int successCount = messageRetryService.autoRetryFailedMessages();

            if (successCount > 0) {
                log.info("自动重试完成: 成功数={}", successCount);
            }

        } catch (Exception e) {
            log.error("自动重试失败的消息失败", e);
        }
    }

    /**
     * 清理过期的失败记录
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanExpiredFailedRecords() {
        try {
            log.debug("开始清理过期的失败记录...");
            int cleanedCount = messageRetryService.cleanExpiredFailedRecords();

            if (cleanedCount > 0) {
                log.info("清理过期失败记录完成: 清理数={}", cleanedCount);
            }

        } catch (Exception e) {
            log.error("清理过期失败记录失败", e);
        }
    }

    /**
     * 记录重试统计信息
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 600000) // 每10分钟执行一次
    public void recordRetryStatistics() {
        try {
            var stats = messageRetryService.getRetryStatistics();
            log.info("消息重试统计: {}", stats);

        } catch (Exception e) {
            log.error("记录重试统计信息失败", e);
        }
    }
}
