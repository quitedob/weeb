package com.web.model;

import java.io.Serializable;
import java.util.Objects;
import java.time.LocalDateTime;

/**
 * 群组实体类，对应数据库中的群组记录
 */
public class Group implements Serializable {
    private static final long serialVersionUID = 1L; // 序列化 UID

    // 群组ID，主键
    private Long id;
    // 群组名称
    private String name;
    // 群组头像
    private String avatar;
    // 创建时间
    private LocalDateTime createTime;
    // 更新时间
    private LocalDateTime updateTime;

    // 无参构造函数
    public Group() {
    }

    // 全参构造函数
    public Group(Long id, String name, String avatar, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;                     // 设置群组ID
        this.name = name;                 // 设置群组名称
        this.avatar = avatar;             // 设置群组头像
        this.createTime = createTime;     // 设置创建时间
        this.updateTime = updateTime;     // 设置最后更新时间
    }

    // getter 和 setter 方法
    public Long getId() {
        return id; // 返回群组ID
    }

    public void setId(Long id) {
        this.id = id; // 设置群组ID
    }

    public String getName() {
        return name; // 返回群组名称
    }

    public void setName(String name) {
        this.name = name; // 设置群组名称
    }

    public String getAvatar() {
        return avatar; // 返回群组头像
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar; // 设置群组头像
    }

    public LocalDateTime getCreateTime() {
        return createTime; // 返回创建时间
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime; // 设置创建时间
    }

    public LocalDateTime getUpdateTime() {
        return updateTime; // 返回最后更新时间
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime; // 设置最后更新时间
    }

    // 重写 toString 方法
    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", avatar='" + avatar + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }

    // 重写 equals 方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // 如果对象相同，返回true
        if (o == null || getClass() != o.getClass()) return false; // 如果对象为空或类型不同，返回false
        Group group = (Group) o;
        return Objects.equals(id, group.id) &&
                Objects.equals(name, group.name) &&
                Objects.equals(avatar, group.avatar) &&
                Objects.equals(createTime, group.createTime) &&
                Objects.equals(updateTime, group.updateTime);
    }

    // 重写 hashCode 方法
    @Override
    public int hashCode() {
        return Objects.hash(id, name, avatar, createTime, updateTime);
    }
}
