package com.web.model;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 用户实体类，表示数据库中的用户记录
 */
@TableName("user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L; // 序列化 UID

    // 用户ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 用户名
    private String username;
    // 密码（存储加密后的密码）
    private String password;
    // 性别：0为女，1为男
    private int sex;
    // 电话
    private String phoneNumber;
    // 邮箱地址
    private String userEmail;

    // 新增字段
    private String uniqueArticleLink;        // 唯一标识文章链接
    private String uniqueVideoLink;          // 唯一标识视频链接
    private Date registrationDate = new Date(); // 注册日期

    // 新增字段：IP归属地
    private String ipOwnership; // 用户IP归属地

    // 新增字段：用户类型、头像、徽章信息、最后登录时间
    private String type;          // 用户类型
    private String avatar;        // 用户头像
    private String nickname;      // 用户昵称
    private String badge;         // 用户徽章信息
    private Date loginTime;       // 最后一次登录时间

    private String bio; // 个人简介
    private Integer onlineStatus; // 用户在线状态, stores code from UserOnlineStatus
    private Integer status; // 用户状态：0-禁用，1-启用
    private Integer userLevel = 1; // 用户等级，默认为1

    // 创建时间和更新时间
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt; // 创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt; // 更新时间

    // 关联的统计数据（一对一关系）
    @TableField(exist = false)
    private UserStats userStats;

    // 无参构造函数
    public User() {
        this.bio = null; // Or ""
        this.onlineStatus = com.web.constant.UserOnlineStatus.OFFLINE.getCode();
    }

    // 全参构造函数（包括新增字段）
    public User(Long id, String username, String password, int sex, String phoneNumber, String userEmail,
                String uniqueArticleLink, String uniqueVideoLink, Date registrationDate, String ipOwnership, 
                String type, String avatar, String nickname, String badge, Date loginTime,
                String bio, Integer onlineStatus) {
        this.id = id;                               // 设置用户ID
        this.username = username;                   // 设置用户名
        this.password = password;                   // 设置密码
        this.sex = sex;                             // 设置性别
        this.phoneNumber = phoneNumber;             // 设置电话
        this.userEmail = userEmail;                 // 设置邮箱地址
        this.uniqueArticleLink = uniqueArticleLink; // 设置唯一标识文章链接
        this.uniqueVideoLink = uniqueVideoLink;     // 设置唯一标识视频链接
        this.registrationDate = registrationDate;   // 设置注册日期
        this.ipOwnership = ipOwnership;             // 设置IP归属地
        this.type = type;                           // 设置用户类型
        this.avatar = avatar;                       // 设置用户头像
        this.nickname = nickname;                   // 设置用户昵称
        this.badge = badge;                         // 设置用户徽章信息
        this.loginTime = loginTime;                 // 设置最后一次登录时间
        this.bio = bio;                             // 设置个人简介
        this.onlineStatus = onlineStatus;           // 设置在线状态
    }

    // Getter 和 Setter 方法

    public Long getId() {
        return id; // 返回用户ID
    }

    public void setId(Long id) {
        this.id = id; // 设置用户ID
    }

    public String getUsername() {
        return username; // 返回用户名
    }

    public void setUsername(String username) {
        this.username = username; // 设置用户名
    }

    public String getPassword() {
        return password; // 返回密码
    }

    public void setPassword(String password) {
        this.password = password; // 设置密码
    }

    public int getSex() {
        return sex; // 返回性别
    }

    public void setSex(int sex) {
        this.sex = sex; // 设置性别
    }

    public String getPhoneNumber() {
        return phoneNumber; // 返回电话
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber; // 设置电话
    }

    // Alias method for backward compatibility
    public void setPhone(String phoneNumber) {
        this.phoneNumber = phoneNumber; // 设置电话
    }

    public String getUserEmail() {
        return userEmail; // 返回邮箱地址
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail; // 设置邮箱地址
    }

    // Alias method for backward compatibility
    public void setEmail(String userEmail) {
        this.userEmail = userEmail; // 设置邮箱地址
    }

    // Alias method for backward compatibility
    public String getEmail() {
        return this.userEmail; // 返回邮箱地址
    }

    // Alias method for backward compatibility
    public String getPhone() {
        return this.phoneNumber; // 返回电话号码
    }

    public Integer getUserLevel() {
        return userLevel; // 返回用户等级
    }

    public void setUserLevel(Integer userLevel) {
        this.userLevel = userLevel; // 设置用户等级
    }



    public String getUniqueArticleLink() {
        return uniqueArticleLink; // 返回唯一标识文章链接
    }

    public void setUniqueArticleLink(String uniqueArticleLink) {
        this.uniqueArticleLink = uniqueArticleLink; // 设置唯一标识文章链接
    }

    public String getUniqueVideoLink() {
        return uniqueVideoLink; // 返回唯一标识视频链接
    }

    public void setUniqueVideoLink(String uniqueVideoLink) {
        this.uniqueVideoLink = uniqueVideoLink; // 设置唯一标识视频链接
    }



    public Date getRegistrationDate() {
        return registrationDate; // 返回注册日期
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate; // 设置注册日期
    }

    public String getIpOwnership() {
        return ipOwnership; // 返回IP归属地
    }

    public void setIpOwnership(String ipOwnership) {
        this.ipOwnership = ipOwnership; // 设置IP归属地
    }

    // 新增 Getter 和 Setter 方法

    public String getType() {
        return type; // 返回用户类型
    }

    public void setType(String type) {
        this.type = type; // 设置用户类型
    }

    public String getAvatar() {
        return avatar; // 返回用户头像
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar; // 设置用户头像
    }

    public String getNickname() {
        return nickname; // 返回用户昵称
    }

    public void setNickname(String nickname) {
        this.nickname = nickname; // 设置用户昵称
    }

    public String getBadge() {
        return badge; // 返回用户徽章信息
    }

    public void setBadge(String badge) {
        this.badge = badge; // 设置用户徽章信息
    }

    public Date getLoginTime() {
        return loginTime; // 返回最后一次登录时间
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime; // 设置最后一次登录时间
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(Integer onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreateTime(Date createTime) {
        this.createdAt = createTime;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserStats getUserStats() {
        return userStats;
    }

    public void setUserStats(UserStats userStats) {
        this.userStats = userStats;
    }

    // 重写 toString 方法
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", sex=" + sex +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", uniqueArticleLink='" + uniqueArticleLink + '\'' +
                ", uniqueVideoLink='" + uniqueVideoLink + '\'' +
                ", registrationDate=" + registrationDate +
                ", ipOwnership='" + ipOwnership + '\'' +
                ", type='" + type + '\'' +
                ", avatar='" + avatar + '\'' +
                ", nickname='" + nickname + '\'' +
                ", badge='" + badge + '\'' +
                ", loginTime=" + loginTime +
                ", bio='" + bio + '\'' +
                ", onlineStatus=" + onlineStatus +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // 重写 equals 方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // 对象相同时返回true
        if (o == null || getClass() != o.getClass()) return false; // 类型不同时返回false
        User user = (User) o;
        return sex == user.sex &&
                Objects.equals(id, user.id) &&
                Objects.equals(username, user.username) &&
                Objects.equals(password, user.password) &&
                Objects.equals(phoneNumber, user.phoneNumber) &&
                Objects.equals(userEmail, user.userEmail) &&
                Objects.equals(uniqueArticleLink, user.uniqueArticleLink) &&
                Objects.equals(uniqueVideoLink, user.uniqueVideoLink) &&
                Objects.equals(registrationDate, user.registrationDate) &&
                Objects.equals(ipOwnership, user.ipOwnership) &&
                Objects.equals(type, user.type) &&
                Objects.equals(avatar, user.avatar) &&
                Objects.equals(nickname, user.nickname) &&
                Objects.equals(badge, user.badge) &&
                Objects.equals(loginTime, user.loginTime) &&
                Objects.equals(bio, user.bio) &&
                Objects.equals(onlineStatus, user.onlineStatus) &&
                Objects.equals(status, user.status) &&
                Objects.equals(createdAt, user.createdAt) &&
                Objects.equals(updatedAt, user.updatedAt);
    }

    // 重写 hashCode 方法
    @Override
    public int hashCode() {
        return Objects.hash(id, username, password, sex, phoneNumber, userEmail,
                uniqueArticleLink, uniqueVideoLink, registrationDate, ipOwnership,
                type, avatar, nickname, badge, loginTime, bio, onlineStatus, status,
                createdAt, updatedAt);
    }
}
