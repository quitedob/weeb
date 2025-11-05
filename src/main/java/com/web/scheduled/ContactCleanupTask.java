package com.web.scheduled;

import com.web.constant.ContactStatus;
import com.web.mapper.ContactMapper;
import com.web.model.Contact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 联系人清理定时任务
 * 每天凌晨2点清理过期的PENDING好友申请
 */
@Slf4j
@Component
public class ContactCleanupTask {

    @Autowired
    private ContactMapper contactMapper;

    /**
     * 清理过期的PENDING好友申请
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredPendingContacts() {
        log.info("开始清理过期的PENDING好友申请...");
        
        try {
            Date now = new Date();
            
            // 查询所有PENDING状态的联系人记录
            List<Contact> pendingContacts = contactMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contact>()
                    .eq("status", ContactStatus.PENDING.getCode())
            );
            
            int cleanedCount = 0;
            
            for (Contact contact : pendingContacts) {
                boolean shouldDelete = false;
                
                // 检查是否有设置过期时间
                if (contact.getExpireAt() != null) {
                    if (contact.getExpireAt().before(now)) {
                        shouldDelete = true;
                    }
                } else {
                    // 如果没有设置过期时间，检查创建时间是否超过7天
                    long daysSinceCreation = (now.getTime() - contact.getCreateTime().getTime()) / (1000 * 60 * 60 * 24);
                    if (daysSinceCreation > 7) {
                        shouldDelete = true;
                    }
                }
                
                if (shouldDelete) {
                    contactMapper.deleteById(contact.getId());
                    cleanedCount++;
                    log.debug("删除过期PENDING记录 - ID: {}, 用户: {} -> 好友: {}, 创建时间: {}", 
                             contact.getId(), contact.getUserId(), contact.getFriendId(), contact.getCreateTime());
                }
            }
            
            log.info("✅ 过期PENDING好友申请清理完成 - 共清理 {} 条记录", cleanedCount);
            
        } catch (Exception e) {
            log.error("❌ 清理过期PENDING好友申请失败", e);
        }
    }
}
