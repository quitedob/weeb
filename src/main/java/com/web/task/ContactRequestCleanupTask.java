package com.web.task;

import com.web.constant.ContactStatus;
import com.web.mapper.ContactMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 联系人请求清理任务
 * 定时清理过期的好友请求
 */
@Slf4j
@Component
public class ContactRequestCleanupTask {

    @Autowired
    private ContactMapper contactMapper;

    // 好友请求过期天数
    private static final int EXPIRE_DAYS = 7;

    /**
     * 清理过期的好友请求
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredContactRequests() {
        LocalDateTime now = LocalDateTime.now();
        // No enclosing transaction: each mapper call starts a fresh transaction after a deadlock rollback.
        for (int attempt = 1; ; attempt++) {
            try {
                int expired = contactMapper.expirePendingRequests(now, now.minusDays(EXPIRE_DAYS));
                log.info("Expired {} pending contact requests", expired);
                return;
            } catch (RuntimeException failure) {
                if (attempt >= 3 || !isMysqlDeadlock(failure)) throw failure;
                log.warn("Retrying contact expiry after a database deadlock, attempt {}", attempt + 1);
            }
        }
    }

    private boolean isMysqlDeadlock(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1213) return true;
        }
        return false;
    }

    /**
     * 清理已拒绝的旧请求
     * 每周日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanOldRejectedRequests() {
        try {
            log.info("开始清理旧的已拒绝请求...");

            // 清理30天前被拒绝的请求
            LocalDateTime cleanTime = LocalDateTime.now().minusDays(30);

            int deletedCount = contactMapper.deleteOldRejectedRequests(cleanTime);

            log.info("清理旧的已拒绝请求完成: 删除数量={}", deletedCount);

        } catch (Exception e) {
            log.error("清理旧的已拒绝请求任务执行失败", e);
        }
    }

    /**
     * 统计并记录好友请求数据
     * 每天凌晨4点执行
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void recordContactRequestStatistics() {
        try {
            log.info("开始统计好友请求数据...");

            // 统计各状态的请求数量
            int pendingCount = contactMapper.countByStatus(ContactStatus.PENDING.getCode());
            int acceptedCount = contactMapper.countByStatus(ContactStatus.ACCEPTED.getCode());
            int rejectedCount = contactMapper.countByStatus(ContactStatus.REJECTED.getCode());
            int expiredCount = contactMapper.countByStatus(ContactStatus.EXPIRED.getCode());
            int blockedCount = contactMapper.countByStatus(ContactStatus.BLOCKED.getCode());

            log.info("好友请求统计 - 待处理: {}, 已接受: {}, 已拒绝: {}, 已过期: {}, 已拉黑: {}",
                pendingCount, acceptedCount, rejectedCount, expiredCount, blockedCount);

            // TODO: 将统计数据存储到数据库或发送到监控系统
            // statisticsService.recordContactRequestStats(...);

        } catch (Exception e) {
            log.error("统计好友请求数据失败", e);
        }
    }
}
