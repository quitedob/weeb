package com.web.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.mapper.NotificationMapper;
import com.web.mapper.UserMapper;
import com.web.mapper.ArticleMapper;
import com.web.model.Notification;
import com.web.model.User;
import com.web.model.Article;
import com.web.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务实现类
 * 处理用户通知的创建、查询、标记已读等业务逻辑
 */
@Service
@Transactional
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public void createAndPublishNotification(Long recipientId, Long actorId, String type, String entityType, Long entityId) {
        // 不给自己发通知
        if (recipientId.equals(actorId)) {
            return;
        }
        
        User actor = userMapper.selectById(actorId);
        if (actor == null) {
            return;
        }
        
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        // Note: Notification model doesn't have title and content fields
        // The notification details are determined by type, entityType, and entityId
        
        save(notification);
    }

    @Override
    public Map<String, Object> getNotificationsForUser(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Notification> notifications = notificationMapper.findNotificationsByRecipientId(userId, offset, size);
        int totalCount = notificationMapper.countTotalNotifications(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("notifications", notifications);
        result.put("totalCount", totalCount);
        result.put("currentPage", page);
        result.put("pageSize", size);
        result.put("totalPages", (int) Math.ceil((double) totalCount / size));
        
        return result;
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnreadNotifications(userId);
    }

    @Override
    public boolean markAllAsRead(Long userId) {
        int result = notificationMapper.markAllAsRead(userId);
        return result > 0;
    }

    @Override
    public boolean markAsRead(Long notificationId, Long userId) {
        int result = notificationMapper.markAsRead(notificationId, userId);
        return result > 0;
    }

    @Override
    public int deleteReadNotifications(Long userId) {
        return notificationMapper.deleteReadNotifications(userId);
    }

    @Override
    public int getTotalCount(Long userId) {
        return notificationMapper.countTotalNotifications(userId);
    }

    @Override
    public void createArticleLikeNotification(Long articleId, Long actorId, Long recipientId) {
        createAndPublishNotification(recipientId, actorId, "like", "article", articleId);
    }

    @Override
    public void createNewFollowerNotification(Long actorId, Long recipientId) {
        createAndPublishNotification(recipientId, actorId, "follow", "user", actorId);
    }

    @Override
    public void createCommentNotification(Long articleId, Long commentId, Long actorId, Long recipientId) {
        createAndPublishNotification(recipientId, actorId, "comment", "article", articleId);
    }
}