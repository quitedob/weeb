package com.web.service.Impl;

import cn.hutool.json.JSONUtil;
import com.web.Config.RedisConfig;
import com.web.dto.NotifyDto;
import com.web.dto.RedisBroadcastMsg;
import com.web.mapper.NotificationMapper;
import com.web.model.Notification;
import com.web.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务实现类
 * 实现通知相关的业务逻辑，包括创建、查询、标记已读等功能
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public void createAndPublishNotification(Long recipientId, Long actorId, String type, String entityType, Long entityId) {
        // 1. 创建并持久化通知对象
        Notification notification = new Notification(recipientId, actorId, type, entityType, entityId);
        notificationMapper.insert(notification);

        // 2. 构建实时推送的载荷
        String payload = buildNotificationPayload(notification);

        // 3. 构建Redis广播消息
        RedisBroadcastMsg broadcastMsg = RedisBroadcastMsg.builder()
                .targetUserId(recipientId)
                .messageBody(payload)
                .build();

        // 4. 通过Redis Pub/Sub发布消息，触发实时推送
        redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, broadcastMsg);
    }

    @Override
    public Map<String, Object> getNotificationsForUser(Long userId, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        
        // 计算偏移量
        int offset = (page - 1) * size;
        
        // 查询通知列表
        List<Notification> notifications = notificationMapper.findNotificationsByRecipientId(userId, offset, size);
        
        // 获取未读数量
        int unreadCount = notificationMapper.countUnreadNotifications(userId);
        
        // 获取总数量
        int totalCount = notificationMapper.countTotalNotifications(userId);
        
        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalCount / size);
        
        result.put("notifications", notifications);
        result.put("unreadCount", unreadCount);
        result.put("totalCount", totalCount);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        
        return result;
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnreadNotifications(userId);
    }

    @Override
    @Transactional
    public boolean markAllAsRead(Long userId) {
        int updatedRows = notificationMapper.markAllAsRead(userId);
        return updatedRows > 0;
    }

    @Override
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        int updatedRows = notificationMapper.markAsRead(notificationId, userId);
        return updatedRows > 0;
    }

    @Override
    @Transactional
    public int deleteReadNotifications(Long userId) {
        return notificationMapper.deleteReadNotifications(userId);
    }

    @Override
    public int getTotalCount(Long userId) {
        return notificationMapper.countTotalNotifications(userId);
    }

    @Override
    public void createArticleLikeNotification(Long articleId, Long actorId, Long recipientId) {
        createAndPublishNotification(recipientId, actorId, "ARTICLE_LIKE", "article", articleId);
    }

    @Override
    public void createNewFollowerNotification(Long actorId, Long recipientId) {
        createAndPublishNotification(recipientId, actorId, "NEW_FOLLOWER", "user", actorId);
    }

    @Override
    public void createCommentNotification(Long articleId, Long commentId, Long actorId, Long recipientId) {
        createAndPublishNotification(recipientId, actorId, "COMMENT", "article", articleId);
    }

    /**
     * 构建通知推送载荷
     * @param notification 通知对象
     * @return JSON格式的载荷字符串
     */
    private String buildNotificationPayload(Notification notification) {
        // 创建一个包含通知类型和具体通知内容的DTO
        // 这里的 "notification_new" 是自定义的WebSocket消息类型，用于前端区分
        NotifyDto<Notification> notifyDto = new NotifyDto<>("notification_new", notification);
        return JSONUtil.toJsonStr(notifyDto);
    }
} 