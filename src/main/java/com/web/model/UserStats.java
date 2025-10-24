package com.web.model;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 用户统计数据实体类，表示数据库中的用户统计记录
 * 此表与user表分离以避免写锁竞争，提高并发性能
 */
@TableName("user_stats")
public class UserStats implements Serializable {
    private static final long serialVersionUID = 1L;

    // 用户ID（外键）
    @TableId(type = IdType.INPUT)
    private Long userId;
    
    // 统计数据字段
    private Long fansCount = 0L;             // 粉丝数量
    private Long followerCount = 0L;         // 关注者数量
    private Long followingCount = 0L;        // 关注数量
    private Long articleCount = 0L;          // 文章数量
    private Long totalViews = 0L;            // 总浏览量
    private Long loginCount = 0L;            // 登录次数
    private Long totalLikes = 0L;            // 总点赞数
    private Long totalFavorites = 0L;        // 总收藏数
    private Long totalSponsorship = 0L;      // 总赞助数
    private Long totalArticleExposure = 0L;  // 文章总曝光数
    private Long websiteCoins = 0L;          // 网站金币
    private java.time.LocalDateTime lastLoginTime; // 最后登录时间
    
    // 时间戳字段
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt = new Date();     // 创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt = new Date();     // 更新时间
    
    // 关联的用户对象（用于JOIN查询）
    @TableField(exist = false)
    private User user;

    // 无参构造函数
    public UserStats() {
    }

    // 带用户ID的构造函数
    public UserStats(Long userId) {
        this.userId = userId;
    }

    // 全参构造函数
    public UserStats(Long userId, Long fansCount, Long totalLikes, Long totalFavorites,
                    Long totalSponsorship, Long totalArticleExposure, Long websiteCoins) {
        this.userId = userId;
        this.fansCount = fansCount;
        this.totalLikes = totalLikes;
        this.totalFavorites = totalFavorites;
        this.totalSponsorship = totalSponsorship;
        this.totalArticleExposure = totalArticleExposure;
        this.websiteCoins = websiteCoins;
    }

    // Getter 和 Setter 方法

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFansCount() {
        return fansCount;
    }

    public void setFansCount(Long fansCount) {
        this.fansCount = fansCount;
    }

    public Long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(Long totalLikes) {
        this.totalLikes = totalLikes;
    }

    public Long getTotalFavorites() {
        return totalFavorites;
    }

    public void setTotalFavorites(Long totalFavorites) {
        this.totalFavorites = totalFavorites;
    }

    public Long getTotalSponsorship() {
        return totalSponsorship;
    }

    public void setTotalSponsorship(Long totalSponsorship) {
        this.totalSponsorship = totalSponsorship;
    }

    public Long getTotalArticleExposure() {
        return totalArticleExposure;
    }

    public void setTotalArticleExposure(Long totalArticleExposure) {
        this.totalArticleExposure = totalArticleExposure;
    }

    public Long getWebsiteCoins() {
        return websiteCoins;
    }

    public void setWebsiteCoins(Long websiteCoins) {
        this.websiteCoins = websiteCoins;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // 新增字段的 getter 和 setter 方法

    public Long getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Long followerCount) {
        this.followerCount = followerCount;
    }

    public Long getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(Long followingCount) {
        this.followingCount = followingCount;
    }

    public Long getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Long articleCount) {
        this.articleCount = articleCount;
    }

    public Long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(Long totalViews) {
        this.totalViews = totalViews;
    }

    public Long getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(Long loginCount) {
        this.loginCount = loginCount;
    }

    public java.time.LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(java.time.LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    // 便利方法：增加粉丝数
    public void incrementFansCount() {
        this.fansCount++;
        this.updatedAt = new Date();
    }

    // 便利方法：减少粉丝数
    public void decrementFansCount() {
        if (this.fansCount > 0) {
            this.fansCount--;
            this.updatedAt = new Date();
        }
    }

    // 便利方法：增加点赞数
    public void incrementTotalLikes() {
        this.totalLikes++;
        this.updatedAt = new Date();
    }

    // 便利方法：增加收藏数
    public void incrementTotalFavorites() {
        this.totalFavorites++;
        this.updatedAt = new Date();
    }

    // 便利方法：增加网站金币
    public void addWebsiteCoins(Long coins) {
        if (coins != null && coins > 0) {
            this.websiteCoins += coins;
            this.updatedAt = new Date();
        }
    }

    // 便利方法：减少网站金币
    public boolean deductWebsiteCoins(Long coins) {
        if (coins != null && coins > 0 && this.websiteCoins >= coins) {
            this.websiteCoins -= coins;
            this.updatedAt = new Date();
            return true;
        }
        return false;
    }

    // 便利方法：增加文章曝光数
    public void incrementArticleExposure() {
        this.totalArticleExposure++;
        this.updatedAt = new Date();
    }

    @Override
    public String toString() {
        return "UserStats{" +
                "userId=" + userId +
                ", fansCount=" + fansCount +
                ", totalLikes=" + totalLikes +
                ", totalFavorites=" + totalFavorites +
                ", totalSponsorship=" + totalSponsorship +
                ", totalArticleExposure=" + totalArticleExposure +
                ", websiteCoins=" + websiteCoins +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStats userStats = (UserStats) o;
        return Objects.equals(userId, userStats.userId) &&
                Objects.equals(fansCount, userStats.fansCount) &&
                Objects.equals(totalLikes, userStats.totalLikes) &&
                Objects.equals(totalFavorites, userStats.totalFavorites) &&
                Objects.equals(totalSponsorship, userStats.totalSponsorship) &&
                Objects.equals(totalArticleExposure, userStats.totalArticleExposure) &&
                Objects.equals(websiteCoins, userStats.websiteCoins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, fansCount, totalLikes, totalFavorites,
                totalSponsorship, totalArticleExposure, websiteCoins);
    }
}