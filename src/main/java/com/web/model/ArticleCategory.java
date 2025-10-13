package com.web.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章分类实体类
 */
public class ArticleCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String categoryName;
    private Long parentId;
    private List<ArticleCategory> children; // 子分类列表

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // Default constructor
    public ArticleCategory() {}

    // Constructor with all fields
    public ArticleCategory(Long id, String categoryName, Long parentId, LocalDateTime createdAt) {
        this.id = id;
        this.categoryName = categoryName;
        this.parentId = parentId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ArticleCategory> getChildren() {
        return children;
    }

    public void setChildren(List<ArticleCategory> children) {
        this.children = children;
    }
}
