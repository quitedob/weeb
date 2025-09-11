package com.web.service;

import com.web.model.Notification;

import java.util.List;
import java.util.Map;

/**
 * 通知服务接口
 * 定义通知相关的业务方法
 */
public interface NotificationService {
    
    /**
     * 创建并发布通知
     * @param recipientId 接收者用户ID
     * @param actorId 触发通知的用户ID
     * @param type 通知类型
     * @param entityType 关联实体类型
     * @param entityId 关联实体ID
     */
    void createAndPublishNotification(Long recipientId, Long actorId, String type, String entityType, Long entityId);
    
    /**
     * 获取用户的通知列表（分页）
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 包含通知列表和分页信息的Map
     */
    Map<String, Object> getNotificationsForUser(Long userId, int page, int size);
    
    /**
     * 获取用户未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    int getUnreadCount(Long userId);
    
    /**
     * 将用户的所有通知标记为已读
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAllAsRead(Long userId);
    
    /**
     * 将指定通知标记为已读
     * @param notificationId 通知ID
     * @param userId 用户ID（用于安全验证）
     * @return 是否成功
     */
    boolean markAsRead(Long notificationId, Long userId);
    
    /**
     * 删除用户的所有已读通知
     * @param userId 用户ID
     * @return 删除的通知数量
     */
    int deleteReadNotifications(Long userId);
    
    /**
     * 获取用户总通知数量
     * @param userId 用户ID
     * @return 总通知数量
     */
    int getTotalCount(Long userId);
    
    /**
     * 创建文章点赞通知
     * @param articleId 文章ID
     * @param actorId 点赞者用户ID
     * @param recipientId 文章作者用户ID
     */
    void createArticleLikeNotification(Long articleId, Long actorId, Long recipientId);
    
    /**
     * 创建新关注者通知
     * @param actorId 关注者用户ID
     * @param recipientId 被关注者用户ID
     */
    void createNewFollowerNotification(Long actorId, Long recipientId);
    
    /**
     * 创建评论通知
     * @param articleId 文章ID
     * @param commentId 评论ID
     * @param actorId 评论者用户ID
     * @param recipientId 文章作者用户ID
     */
    void createCommentNotification(Long articleId, Long commentId, Long actorId, Long recipientId);
} 