package com.web.task;

import com.web.service.UserLevelHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 用户等级历史清理定时任务
 * 定期清理过期的等级变更历史记录
 * 
 * 配置说明：
 * - weeb.level-history.retention-days: 保留天数，默认180天（6个月）
 * - weeb.level-history.cleanup-enabled: 是否启用清理任务，默认true
 */
@Slf4j
@Component
public class UserLevelHistoryCleanupTask {

    @Autowired
    private UserLevelHistoryService userLevelHistoryService;

    /**
     * 历史记录保留天数，默认180天（6个月）
     */
    @Value("${weeb.level-history.retention-days:180}")
    private int retentionDays;

    /**
     * 是否启用清理任务，默认启用
     */
    @Value("${weeb.level-history.cleanup-enabled:true}")
    private boolean cleanupEnabled;

    /**
     * 定时清理过期的等级历史记录
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "${weeb.level-history.cleanup-cron:0 0 3 * * ?}")
    public void cleanupExpiredRecords() {
        if (!cleanupEnabled) {
            log.debug("等级历史清理任务已禁用");
            return;
        }

        log.info("开始清理过期的等级历史记录，保留天数: {}", retentionDays);

        try {
            // 计算清理时间点
            LocalDateTime beforeTime = LocalDateTime.now().minusDays(retentionDays);

            // 执行清理
            int cleanedCount = userLevelHistoryService.cleanupExpiredRecords(beforeTime);

            log.info("等级历史清理完成，清理记录数: {}, 清理时间点: {}", cleanedCount, beforeTime);

        } catch (Exception e) {
            log.error("清理等级历史记录失败", e);
        }
    }

    /**
     * 手动触发清理任务（用于测试或管理员手动清理）
     * 
     * @param days 保留天数
     * @return 清理的记录数
     */
    public int manualCleanup(int days) {
        log.info("手动触发等级历史清理，保留天数: {}", days);

        try {
            LocalDateTime beforeTime = LocalDateTime.now().minusDays(days);
            int cleanedCount = userLevelHistoryService.cleanupExpiredRecords(beforeTime);

            log.info("手动清理完成，清理记录数: {}", cleanedCount);
            return cleanedCount;

        } catch (Exception e) {
            log.error("手动清理等级历史记录失败", e);
            throw new RuntimeException("清理失败: " + e.getMessage());
        }
    }

    /**
     * 获取清理任务配置信息
     */
    public String getCleanupInfo() {
        return String.format("清理任务状态: %s, 保留天数: %d",
                cleanupEnabled ? "启用" : "禁用", retentionDays);
    }
}
