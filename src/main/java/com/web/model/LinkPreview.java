package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 链接预览实体类
 * 用于在消息中显示链接的预览信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LinkPreview extends BaseEntity {

    /**
     * 原始链接URL
     */
    private String originalUrl;

    /**
     * 最终重定向URL
     */
    private String finalUrl;

    /**
     * 页面标题
     */
    private String title;

    /**
     * 页面描述
     */
    private String description;

    /**
     * 网站名称
     */
    private String siteName;

    /**
     * 网站图标URL
     */
    private String siteIcon;

    /**
     * 预览图片URL
     */
    private String imageUrl;

    /**
     * 图片宽度
     */
    private Integer imageWidth;

    /**
     * 图片高度
     */
    private Integer imageHeight;

    /**
     * 内容类型（image, video, article, website等）
     */
    private String contentType;

    /**
     * 预览状态（pending, success, failed, no_preview）
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 预览生成时间（毫秒）
     */
    private Long generationTime;

    /**
     * 预览缓存过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 提取的Open Graph标签
     */
    private String ogTags;

    /**
     * 提取的Twitter Card标签
     */
    private String twitterTags;

    /**
     * 关联的消息ID
     */
    private Long messageId;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /**
     * 是否被禁用（用户可以禁用预览）
     */
    private Boolean isDisabled;

    /**
     * 用户评分（1-5星）
     */
    private Integer userRating;

    /**
     * 额外元数据（JSON格式）
     */
    private String metadata;

    public LinkPreview() {
        this.status = "pending";
        this.isDisabled = false;
        this.userRating = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // 设置默认过期时间24小时
        this.expiresAt = LocalDateTime.now().plusHours(24);
    }

    public LinkPreview(String originalUrl) {
        this();
        this.originalUrl = originalUrl;
        this.finalUrl = originalUrl;
    }

    /**
     * 预览状态枚举
     */
    public static class Status {
        public static final String PENDING = "pending";
        public static final String SUCCESS = "success";
        public static final String FAILED = "failed";
        public static final String NO_PREVIEW = "no_preview";
    }

    /**
     * 内容类型枚举
     */
    public static class ContentType {
        public static final String WEBSITE = "website";
        public static final String ARTICLE = "article";
        public static final String IMAGE = "image";
        public static final String VIDEO = "video";
        public static final String AUDIO = "audio";
        public static final String PRODUCT = "product";
        public static final String SOCIAL = "social";
        public static final String OTHER = "other";
    }

    /**
     * 检查预览是否有效
     */
    public boolean isValid() {
        return Status.SUCCESS.equals(this.status) && finalUrl != null && !finalUrl.trim().isEmpty();
    }

    /**
     * 检查预览是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查是否有图片
     */
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    /**
     * 检查是否有描述
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * 获取显示尺寸信息
     */
    public String getDisplaySize() {
        if (imageWidth != null && imageHeight != null) {
            return imageWidth + "x" + imageHeight;
        }
        return "unknown";
    }

    /**
     * 标记为成功
     */
    public void markAsSuccess() {
        this.status = Status.SUCCESS;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为无预览
     */
    public void markAsNoPreview() {
        this.status = Status.NO_PREVIEW;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 禁用预览
     */
    public void disable() {
        this.isDisabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 启用预览
     */
    public void enable() {
        this.isDisabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置用户评分
     */
    public void setUserRating(Integer rating) {
        if (rating != null && rating >= 1 && rating <= 5) {
            this.userRating = rating;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 延长过期时间
     */
    public void extendExpiration(int hours) {
        this.expiresAt = LocalDateTime.now().plusHours(hours);
        this.updatedAt = LocalDateTime.now();
    }
}