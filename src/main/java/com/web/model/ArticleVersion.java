package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 文章版本实体类
 * 用于存储文章的历史版本，支持版本回滚功能
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArticleVersion extends BaseEntity {

    /**
     * 文章ID（关联到articles表）
     */
    private Long articleId;

    /**
     * 版本号
     */
    private Integer versionNumber;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章内容
     */
    private String content;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章链接
     */
    private String articleLink;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 文章状态（draft, published, archived等）
     */
    private String status;

    /**
     * 标签（JSON格式）
     */
    private String tags;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 版本说明
     */
    private String versionNote;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /**
     * 创建者用户名
     */
    private String createdByUsername;

    /**
     * 变更类型（create, update, minor_edit, major_edit等）
     */
    private String changeType;

    /**
     * 变更摘要（主要修改点）
     */
    private String changeSummary;

    /**
     * 字符数变化
     */
    private Integer characterChange;

    /**
     * 是否为主要版本
     */
    private Boolean isMajorVersion;

    /**
     * 是否自动保存版本
     */
    private Boolean isAutoSave;

    /**
     * 版本大小（字符数）
     */
    private Integer size;

    /**
     * 版本哈希值（用于内容去重）
     */
    private String contentHash;

    public ArticleVersion() {
        this.versionNumber = 1;
        this.status = "draft";
        this.changeType = "create";
        this.isMajorVersion = true;
        this.isAutoSave = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ArticleVersion(Long articleId, Integer versionNumber, String title, String content, Long createdBy) {
        this();
        this.articleId = articleId;
        this.versionNumber = versionNumber;
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
    }

    /**
     * 变更类型枚举
     */
    public static class ChangeType {
        public static final String CREATE = "create";
        public static final String UPDATE = "update";
        public static final String MINOR_EDIT = "minor_edit";
        public static final String MAJOR_EDIT = "major_edit";
        public static final String TITLE_CHANGE = "title_change";
        public static final String CONTENT_CHANGE = "content_change";
        public static final String AUTO_SAVE = "auto_save";
    }

    /**
     * 版本状态枚举
     */
    public static class Status {
        public static final String DRAFT = "draft";
        public static final String PUBLISHED = "published";
        public static final String ARCHIVED = "archived";
        public static final String DELETED = "deleted";
    }
}