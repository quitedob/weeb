package com.web.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 用户完整信息DTO类，包含用户基本信息和统计数据
 * 用于需要同时返回用户信息和统计数据的场景
 */
public class UserWithStats implements Serializable {
    private static final long serialVersionUID = 1L;

    // 用户基本信息
    private User user;
    
    // 用户统计数据
    private UserStats userStats;

    // 无参构造函数
    public UserWithStats() {
    }

    // 构造函数
    public UserWithStats(User user, UserStats userStats) {
        this.user = user;
        this.userStats = userStats;
    }

    // Getter 和 Setter 方法
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserStats getUserStats() {
        return userStats;
    }

    public void setUserStats(UserStats userStats) {
        this.userStats = userStats;
    }

    // 便利方法：获取用户ID
    public Long getId() {
        return user != null ? user.getId() : null;
    }

    // 便利方法：获取用户名
    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }

    // 便利方法：获取昵称
    public String getNickname() {
        return user != null ? user.getNickname() : null;
    }

    // 便利方法：获取头像
    public String getAvatar() {
        return user != null ? user.getAvatar() : null;
    }

    // 便利方法：获取粉丝数
    public Long getFansCount() {
        return userStats != null ? userStats.getFansCount() : 0L;
    }

    // 便利方法：获取总点赞数
    public Long getTotalLikes() {
        return userStats != null ? userStats.getTotalLikes() : 0L;
    }

    // 便利方法：获取总收藏数
    public Long getTotalFavorites() {
        return userStats != null ? userStats.getTotalFavorites() : 0L;
    }

    // 便利方法：获取网站金币
    public Long getWebsiteCoins() {
        return userStats != null ? userStats.getWebsiteCoins() : 0L;
    }

    // 便利方法：获取文章总曝光数
    public Long getTotalArticleExposure() {
        return userStats != null ? userStats.getTotalArticleExposure() : 0L;
    }

    // 便利方法：获取总赞助数
    public Long getTotalSponsorship() {
        return userStats != null ? userStats.getTotalSponsorship() : 0L;
    }

    // 便利方法：检查是否有统计数据
    public boolean hasStats() {
        return userStats != null;
    }

    // 便利方法：确保统计数据存在
    public void ensureStats() {
        if (userStats == null && user != null) {
            userStats = new UserStats(user.getId());
        }
    }

    @Override
    public String toString() {
        return "UserWithStats{" +
                "user=" + user +
                ", userStats=" + userStats +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserWithStats that = (UserWithStats) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(userStats, that.userStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, userStats);
    }
}