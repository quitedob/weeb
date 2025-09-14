package com.web.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 通知实体类
 * 对应数据库notifications表
 */
@TableName("notifications")
public class Notification {
    
    @TableId(type = IdType.AUTO)
    private Long id; // 通知的唯一ID
    
    private Long recipientId; // 接收通知的用户ID
    
    private Long actorId; // 触发通知的用户的ID (如点赞者、关注者)
    
    private String type; // 通知类型 (如ARTICLE_LIKE, NEW_FOLLOWER, COMMENT_MENTION)
    
    private String entityType; // 关联实体的类型 (如article, user, comment)
    
    private Long entityId; // 关联实体的ID
    
    private Boolean isRead; // 通知是否已读 (0: 未读, 1: 已读)
    
    private LocalDateTime createdAt; // 通知创建时间
    
    // 构造函数
    public Notification() {}
    
    public Notification(Long recipientId, Long actorId, String type, String entityType, Long entityId) {
        this.recipientId = recipientId;
        this.actorId = actorId;
        this.type = type;
        this.entityType = entityType;
        this.entityId = entityId;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getRecipientId() {
        return recipientId;
    }
    
    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }
    
    public Long getActorId() {
        return actorId;
    }
    
    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public Long getEntityId() {
        return entityId;
    }
    
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", recipientId=" + recipientId +
                ", actorId=" + actorId +
                ", type='" + type + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
} 