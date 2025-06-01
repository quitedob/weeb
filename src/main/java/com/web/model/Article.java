package com.web.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * 文章实体类，对应数据库中的文章记录
 */
public class Article implements Serializable {
    private static final long serialVersionUID = 1L; // 序列化 UID

    // 文章ID，主键
    private Long id;
    // 用户ID
    private Long userId;
    // 文章标题
    private String articleTitle;
    // 文章链接，唯一标识
    private String articleLink;
    // 点赞数
    private Integer likesCount;
    // 收藏数
    private Integer favoritesCount;
    // 赞助金额
    private Double sponsorsCount;
    // 文章曝光总数
    private Long exposureCount;
    // 创建时间
    private String createdAt;
    // 最后更新时间
    private String updatedAt;

    // 无参构造函数
    public Article() {
    }

    // 全参构造函数
    public Article(Long id, Long userId, String articleTitle, String articleLink, Integer likesCount, Integer favoritesCount,
                   Double sponsorsCount, Long exposureCount, String createdAt, String updatedAt) {
        this.id = id;                             // 设置文章ID
        this.userId = userId;                     // 设置用户ID
        this.articleTitle = articleTitle;         // 设置文章标题
        this.articleLink = articleLink;           // 设置文章链接
        this.likesCount = likesCount;             // 设置点赞数
        this.favoritesCount = favoritesCount;     // 设置收藏数
        this.sponsorsCount = sponsorsCount;       // 设置赞助金额
        this.exposureCount = exposureCount;       // 设置文章曝光总数
        this.createdAt = createdAt;               // 设置创建时间
        this.updatedAt = updatedAt;               // 设置最后更新时间
    }

    // getter 和 setter 方法
    public Long getId() {
        return id; // 返回文章ID
    }

    public void setId(Long id) {
        this.id = id; // 设置文章ID
    }

    public Long getUserId() {
        return userId; // 返回用户ID
    }

    public void setUserId(Long userId) {
        this.userId = userId; // 设置用户ID
    }

    public String getArticleTitle() {
        return articleTitle; // 返回文章标题
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle; // 设置文章标题
    }

    public String getArticleLink() {
        return articleLink; // 返回文章链接
    }

    public void setArticleLink(String articleLink) {
        this.articleLink = articleLink; // 设置文章链接
    }

    public Integer getLikesCount() {
        return likesCount; // 返回点赞数
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount; // 设置点赞数
    }

    public Integer getFavoritesCount() {
        return favoritesCount; // 返回收藏数
    }

    public void setFavoritesCount(Integer favoritesCount) {
        this.favoritesCount = favoritesCount; // 设置收藏数
    }

    public Double getSponsorsCount() {
        return sponsorsCount; // 返回赞助金额
    }

    public void setSponsorsCount(Double sponsorsCount) {
        this.sponsorsCount = sponsorsCount; // 设置赞助金额
    }

    public Long getExposureCount() {
        return exposureCount; // 返回文章曝光总数
    }

    public void setExposureCount(Long exposureCount) {
        this.exposureCount = exposureCount; // 设置文章曝光总数
    }

    public String getCreatedAt() {
        return createdAt; // 返回创建时间
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt; // 设置创建时间
    }

    public String getUpdatedAt() {
        return updatedAt; // 返回最后更新时间
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt; // 设置最后更新时间
    }

    // 重写 toString 方法
    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", userId=" + userId +
                ", articleTitle='" + articleTitle + '\'' +
                ", articleLink='" + articleLink + '\'' +
                ", likesCount=" + likesCount +
                ", favoritesCount=" + favoritesCount +
                ", sponsorsCount=" + sponsorsCount +
                ", exposureCount=" + exposureCount +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }

    // 重写 equals 方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // 如果对象相同，返回true
        if (o == null || getClass() != o.getClass()) return false; // 如果对象为空或类型不同，返回false
        Article article = (Article) o;
        return Objects.equals(id, article.id) &&
                Objects.equals(userId, article.userId) &&
                Objects.equals(articleTitle, article.articleTitle) &&
                Objects.equals(articleLink, article.articleLink) &&
                Objects.equals(likesCount, article.likesCount) &&
                Objects.equals(favoritesCount, article.favoritesCount) &&
                Objects.equals(sponsorsCount, article.sponsorsCount) &&
                Objects.equals(exposureCount, article.exposureCount) &&
                Objects.equals(createdAt, article.createdAt) &&
                Objects.equals(updatedAt, article.updatedAt);
    }

    // 重写 hashCode 方法
    @Override
    public int hashCode() {
        return Objects.hash(id, userId, articleTitle, articleLink, likesCount, favoritesCount, sponsorsCount, exposureCount, createdAt, updatedAt);
    }
}
