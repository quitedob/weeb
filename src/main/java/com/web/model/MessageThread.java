package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 消息线索实体类
 * 用于支持消息的回复和讨论功能
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MessageThread extends BaseEntity {

    /**
     * 原始消息ID
     */
    private Long rootMessageId;

    /**
     * 线索标题（从原始消息自动生成）
     */
    private String title;

    /**
     * 线索描述
     */
    private String description;

    /**
     * 线索创建者ID
     */
    private Long createdBy;

    /**
     * 线索创建者用户名
     */
    private String createdByUsername;

    /**
     * 线索状态（active, archived, closed）
     */
    private String status;

    /**
     * 参与者数量
     */
    private Integer participantCount;

     /**
     * 最后回复时间
     */
    private LocalDateTime lastReplyAt;

    /**
     * 最后回复者ID
     */
    private Long lastReplyBy;

    /**
     * 最后回复者用户名
     */
    private String lastReplyByUsername;

    /**
     * 是否被置顶
     */
    private Boolean isPinned;

    /**
     * 是否被锁定（锁定后不能再回复）
     */
    private Boolean isLocked;

    /**
     * 线索标签（JSON格式）
     */
    private String tags;

    /**
     * 附加元数据（JSON格式）
     */
    private String metadata;

    public MessageThread() {
        this.status = "active";
        this.participantCount = 1;
        this.isPinned = false;
        this.isLocked = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public MessageThread(Long rootMessageId, String title, Long createdBy) {
        this();
        this.rootMessageId = rootMessageId;
        this.title = title;
        this.createdBy = createdBy;
    }

    /**
     * 线索状态枚举
     */
    public static class Status {
        public static final String ACTIVE = "active";
        public static final String ARCHIVED = "archived";
        public static final String CLOSED = "closed";
    }

    /**
     * 检查线索是否活跃
     */
    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }

    /**
     * 检查用户是否可以回复线索
     */
    public boolean canReply(Long userId) {
        return isActive() && !isLocked && participantCount > 0;
    }

    /**
     * 检查用户是否是线索创建者
     */
    public boolean isCreatedBy(Long userId) {
        return createdBy != null && createdBy.equals(userId);
    }

    /**
     * 检查用户是否是最后回复者
     */
    public boolean isLastReplier(Long userId) {
        return lastReplyBy != null && lastReplyBy.equals(userId);
    }
}