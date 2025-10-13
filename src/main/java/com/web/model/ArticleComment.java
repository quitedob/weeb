package com.web.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章评论实体类，对应数据库中的文章评论记录
 */
public class ArticleComment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long articleId;
    private Long userId;
    private String content;
    private Long parentId;
    private String username; // 用户名，用于显示
    private String userAvatar; // 用户头像，用于显示

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Default constructor
    public ArticleComment() {}

    // Constructor with all fields
    public ArticleComment(Long id, Long articleId, Long userId, String content, Long parentId,
                         String username, String userAvatar, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.articleId = articleId;
        this.userId = userId;
        this.content = content;
        this.parentId = parentId;
        this.username = username;
        this.userAvatar = userAvatar;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters for all fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}