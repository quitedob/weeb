package com.web.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime; // Import LocalDateTime
import java.util.Objects; // Keep Objects if equals/hashCode are implemented
import java.util.List;

/**
 * 文章实体类，对应数据库中的文章记录
 */
public class Article implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long articleId;
    private Long userId;
    private Long categoryId; // 新增分类ID
    private String articleTitle;
    private String articleContent; // 文章内容
    private String articleLink;    // 文章外部链接（可选）
    private Integer status;        // 文章状态：0-草稿，1-已发布
    private Integer likesCount;
    private Integer favoritesCount; // Assuming this field exists or will be used
    private Double sponsorsCount;  // Assuming this field exists or will be used
    private Long exposureCount;
    private List<ArticleTag> tags; // 新增标签列表

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Default constructor
    public Article() {}

    // Backward compatibility method - maps to articleId for existing code
    public Long getId() {
        return this.articleId;
    }

    // Constructor with all fields (example provided by user)
    public Article(Long articleId, Long userId, Long categoryId, String articleTitle, String articleContent, String articleLink,
                   Integer status, Integer likesCount, Integer favoritesCount, Double sponsorsCount,
                   Long exposureCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.articleId = articleId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.articleTitle = articleTitle;
        this.articleContent = articleContent;
        this.articleLink = articleLink;
        this.status = status;
        this.likesCount = likesCount;
        this.favoritesCount = favoritesCount;
        this.sponsorsCount = sponsorsCount;
        this.exposureCount = exposureCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters for all fields

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public String getArticleContent() {
        return articleContent;
    }

    public void setArticleContent(String articleContent) {
        this.articleContent = articleContent;
    }

    public Integer getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    public Integer getFavoritesCount() {
        return favoritesCount;
    }

    public void setFavoritesCount(Integer favoritesCount) {
        this.favoritesCount = favoritesCount;
    }

    public Double getSponsorsCount() {
        return sponsorsCount;
    }

    public void setSponsorsCount(Double sponsorsCount) {
        this.sponsorsCount = sponsorsCount;
    }

    public Long getExposureCount() {
        return exposureCount;
    }

    public void setExposureCount(Long exposureCount) {
        this.exposureCount = exposureCount;
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

    public String getArticleLink() {
        return articleLink;
    }

    public void setArticleLink(String articleLink) {
        this.articleLink = articleLink;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<ArticleTag> getTags() {
        return tags;
    }

    public void setTags(List<ArticleTag> tags) {
        this.tags = tags;
    }

}
