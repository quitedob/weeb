package com.web.task;

import com.web.constant.ContactStatus;
import com.web.mapper.ContactMapper;
import com.web.model.Contact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
        try {
            log.info("开始清理过期的好友请求...");

            // 计算过期时间点
            LocalDateTime expireTime = LocalDateTime.now().minusDays(EXPIRE_DAYS);

            // 查询所有过期的PENDING状态请求
            List<Contact> expiredRequests = contactMapper.findExpiredPendingRequests(expireTime);

            if (expiredRequests == null || expiredRequests.isEmpty()) {
                log.info("没有过期的好友请求需要清理");
                return;
            }

            int cleanedCount = 0;
            for (Contact contact : expiredRequests) {
                try {
                    // 更新状态为EXPIRED
                    contact.setStatus(ContactStatus.EXPIRED);
                    contactMapper.updateById(contact);

                    // TODO: 发送过期通知给申请人
                    // notificationService.sendFriendRequestExpiredNotification(contact);

                    cleanedCount++;
                } catch (Exception e) {
                    log.error("清理过期请求失败: contactId={}", contact.getId(), e);
                }
            }

            log.info("清理过期好友请求完成: 总数={}, 成功={}", expiredRequests.size(), cleanedCount);

        } catch (Exception e) {
            log.error("清理过期好友请求任务执行失败", e);
        }
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
            int pendingCount = contactMapper.countByStatus(ContactStatus.PENDING);
            int acceptedCount = contactMapper.countByStatus(ContactStatus.ACCEPTED);
            int rejectedCount = contactMapper.countByStatus(ContactStatus.REJECTED);
            int expiredCount = contactMapper.countByStatus(ContactStatus.EXPIRED);
            int blockedCount = contactMapper.countByStatus(ContactStatus.BLOCKED);

            log.info("好友请求统计 - 待处理: {}, 已接受: {}, 已拒绝: {}, 已过期: {}, 已拉黑: {}",
                pendingCount, acceptedCount, rejectedCount, expiredCount, blockedCount);

            // TODO: 将统计数据存储到数据库或发送到监控系统
            // statisticsService.recordContactRequestStats(...);

        } catch (Exception e) {
            log.error("统计好友请求数据失败", e);
        }
    }
}
