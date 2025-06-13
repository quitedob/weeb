package com.web.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime; // Import LocalDateTime
import java.util.Objects; // Keep Objects if equals/hashCode are implemented

/**
 * 文章实体类，对应数据库中的文章记录
 */
public class Article implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String articleTitle;
    private String articleContent; // Renamed from articleLink
    private Integer likesCount;
    private Integer favoritesCount; // Assuming this field exists or will be used
    private Double sponsorsCount;  // Assuming this field exists or will be used
    private Long exposureCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Default constructor
    public Article() {}

    // Constructor with all fields (example provided by user)
    public Article(Long id, Long userId, String articleTitle, String articleContent,
                   Integer likesCount, Integer favoritesCount, Double sponsorsCount,
                   Long exposureCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.articleTitle = articleTitle;
        this.articleContent = articleContent;
        this.likesCount = likesCount;
        this.favoritesCount = favoritesCount;
        this.sponsorsCount = sponsorsCount;
        this.exposureCount = exposureCount;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

}
