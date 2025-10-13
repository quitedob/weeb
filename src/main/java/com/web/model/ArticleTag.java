package com.web.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章标签实体类
 */
public class ArticleTag implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String tagName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // Default constructor
    public ArticleTag() {}

    // Constructor with all fields
    public ArticleTag(Long id, String tagName, LocalDateTime createdAt) {
        this.id = id;
        this.tagName = tagName;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
