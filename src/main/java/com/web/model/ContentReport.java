package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 内容举报实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContentReport extends BaseEntity {

    /**
     * 举报人ID
     */
    private Long reporterId;

    /**
     * 被举报内容类型（article, comment, message, user等）
     */
    private String contentType;

    /**
     * 被举报内容ID
     */
    private Long contentId;

    /**
     * 举报原因（spam, harassment, inappropriate_content, violence, copyright等）
     */
    private String reason;

    /**
     * 举报描述
     */
    private String description;

    /**
     * 举报状态（pending: 待处理, reviewing: 审核中, resolved: 已处理, dismissed: 已驳回）
     */
    private String status;

    /**
     * 处理人ID（管理员）
     */
    private Long reviewerId;

    /**
     * 处理结果（remove_content: 删除内容, warn_user: 警告用户, ban_user: 封禁用户, no_action: 无需处理）
     */
    private String action;

    /**
     * 处理说明
     */
    private String reviewNote;

    /**
     * 处理时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 附加信息（JSON格式，存储截图、链接等证据）
     */
    private String metadata;

    /**
     * 举报计数（同一内容被多少人举报）
     */
    private Integer reportCount;

    /**
     * 是否为紧急举报
     */
    private Boolean isUrgent;

    public ContentReport() {
        this.status = "pending";
        this.reportCount = 1;
        this.isUrgent = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ContentReport(Long reporterId, String contentType, Long contentId, String reason) {
        this();
        this.reporterId = reporterId;
        this.contentType = contentType;
        this.contentId = contentId;
        this.reason = reason;
    }

    /**
     * 举报原因枚举
     */
    public static class Reason {
        public static final String SPAM = "spam";
        public static final String HARASSMENT = "harassment";
        public static final String INAPPROPRIATE_CONTENT = "inappropriate_content";
        public static final String VIOLENCE = "violence";
        public static final String COPYRIGHT = "copyright";
        public static final String FAKE_NEWS = "fake_news";
        public static final String PRIVACY_VIOLATION = "privacy_violation";
        public static final String HATE_SPEECH = "hate_speech";
        public static final String OTHER = "other";
    }

    /**
     * 举报状态枚举
     */
    public static class Status {
        public static final String PENDING = "pending";
        public static final String REVIEWING = "reviewing";
        public static final String RESOLVED = "resolved";
        public static final String DISMISSED = "dismissed";
    }

    /**
     * 处理结果枚举
     */
    public static class Action {
        public static final String REMOVE_CONTENT = "remove_content";
        public static final String WARN_USER = "warn_user";
        public static final String BAN_USER = "ban_user";
        public static final String NO_ACTION = "no_action";
        public static final String TEMPORARY_SUSPEND = "temporary_suspend";
    }
}