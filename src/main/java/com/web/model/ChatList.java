package com.web.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class ChatList implements Serializable {
    private static final long serialVersionUID = 1L;

    // 主键ID（对应数据库 id 字段，varchar(255)）- 用户维度的唯一标识
    private String id;
    // 用户ID（对应数据库 user_id 字段，BIGINT）
    private Long userId;
    // 共享聊天ID（对应数据库 shared_chat_id 字段，BIGINT）- 私聊双方共用，群聊所有成员共用
    private Long sharedChatId;
    // 目标用户ID（对应数据库 target_id 字段，BIGINT）
    private Long targetId;
    // 会话标题或目标信息（对应数据库 target_info 字段，text，不允许为 null）
    private String targetInfo;
    // 未读消息数（对应数据库 unread_count 字段，int，默认值 0）
    private Integer unreadCount = 0;
    // 最后一条消息（对应数据库 last_message 字段，text）
    private String lastMessage;
    // 会话类型（对应数据库 type 字段，varchar(255)）
    private String type;
    // 创建时间（对应数据库 create_time 字段，timestamp(3)，不允许为 null）
    private LocalDateTime createTime;
    // 最后更新时间（对应数据库 update_time 字段，timestamp(3)，不允许为 null）
    private LocalDateTime updateTime;
    // 群组ID, for group chats
    private Long groupId;

    // 无参构造函数
    public ChatList() {
    }

    // 全参构造函数
    public ChatList(String id, Long userId, Long sharedChatId, Long targetId, String targetInfo, Integer unreadCount, String lastMessage,
                    String type, LocalDateTime createTime, LocalDateTime updateTime, Long groupId) {
        this.id = id;
        this.userId = userId;
        this.sharedChatId = sharedChatId;
        this.targetId = targetId;
        this.targetInfo = targetInfo;
        this.unreadCount = unreadCount;
        this.lastMessage = lastMessage;
        this.type = type;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.groupId = groupId;
    }

    // Getter 和 Setter 方法

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSharedChatId() {
        return sharedChatId;
    }

    public void setSharedChatId(Long sharedChatId) {
        this.sharedChatId = sharedChatId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getTargetInfo() {
        return targetInfo;
    }

    public void setTargetInfo(String targetInfo) {
        this.targetInfo = targetInfo;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    // 重写 toString 方法
    @Override
    public String toString() {
        return "ChatList{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", sharedChatId=" + sharedChatId +
                ", targetId=" + targetId +
                ", targetInfo='" + targetInfo + '\'' +
                ", unreadCount=" + unreadCount +
                ", lastMessage='" + lastMessage + '\'' +
                ", type='" + type + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", groupId=" + groupId +
                '}';
    }

    // 重写 equals 方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatList chatList = (ChatList) o;
        return Objects.equals(id, chatList.id) &&
                Objects.equals(userId, chatList.userId) &&
                Objects.equals(sharedChatId, chatList.sharedChatId) &&
                Objects.equals(targetId, chatList.targetId) &&
                Objects.equals(targetInfo, chatList.targetInfo) &&
                Objects.equals(unreadCount, chatList.unreadCount) &&
                Objects.equals(lastMessage, chatList.lastMessage) &&
                Objects.equals(type, chatList.type) &&
                Objects.equals(createTime, chatList.createTime) &&
                Objects.equals(updateTime, chatList.updateTime) &&
                Objects.equals(groupId, chatList.groupId);
    }

    // 重写 hashCode 方法
    @Override
    public int hashCode() {
        return Objects.hash(id, userId, sharedChatId, targetId, targetInfo, unreadCount, lastMessage, type, createTime, updateTime, groupId);
    }
}