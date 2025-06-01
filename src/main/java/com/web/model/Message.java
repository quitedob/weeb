package com.web.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * 消息实体类，表示数据库中的消息记录
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L; // 序列化 UID

    private Long id; // 消息ID
    private Long chatId; // 聊天ID
    private Long senderId; // 发送者ID
    private String msgContent; // 消息内容
    private String msgType; // 消息类型，String 类型
    private Integer readStatus; // 已读状态
    private Integer isRecalled; // 是否撤回
    private String userIp; // 发送者IP地址
    private String source; // 消息来源
    private Long referenceMsgId; // 参考消息ID
    private Integer isShowTime; // 是否显示时间
    private Timestamp createdAt; // 创建时间
    private Timestamp updatedAt; // 更新时间

    // Getter 和 Setter 方法

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getMsgContent() {
        return msgContent;
    }

    public void setMsgContent(String msgContent) {
        this.msgContent = msgContent;
    }

    public String getMsgType() { // 确保返回类型为 String
        return msgType;
    }

    public void setMsgType(String msgType) { // 确保接收类型为 String
        this.msgType = msgType;
    }

    public Integer getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Integer readStatus) {
        this.readStatus = readStatus;
    }

    public Integer getIsRecalled() {
        return isRecalled;
    }

    public void setIsRecalled(Integer isRecalled) {
        this.isRecalled = isRecalled;
    }

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getReferenceMsgId() {
        return referenceMsgId;
    }

    public void setReferenceMsgId(Long referenceMsgId) {
        this.referenceMsgId = referenceMsgId;
    }

    public Integer getIsShowTime() {
        return isShowTime;
    }

    public void setIsShowTime(Integer isShowTime) {
        this.isShowTime = isShowTime;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 重写 toString 方法
    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", chatId=" + chatId +
                ", senderId=" + senderId +
                ", msgContent='" + msgContent + '\'' +
                ", msgType='" + msgType + '\'' +
                ", readStatus=" + readStatus +
                ", isRecalled=" + isRecalled +
                ", userIp='" + userIp + '\'' +
                ", source='" + source + '\'' +
                ", referenceMsgId=" + referenceMsgId +
                ", isShowTime=" + isShowTime +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // 重写 equals 方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // 对象相同时返回true
        if (o == null || getClass() != o.getClass()) return false; // 类型不同时返回false

        Message message = (Message) o;
        return Objects.equals(id, message.id) &&
                Objects.equals(chatId, message.chatId) &&
                Objects.equals(senderId, message.senderId) &&
                Objects.equals(msgContent, message.msgContent) &&
                Objects.equals(msgType, message.msgType) &&
                Objects.equals(readStatus, message.readStatus) &&
                Objects.equals(isRecalled, message.isRecalled) &&
                Objects.equals(userIp, message.userIp) &&
                Objects.equals(source, message.source) &&
                Objects.equals(referenceMsgId, message.referenceMsgId) &&
                Objects.equals(isShowTime, message.isShowTime) &&
                Objects.equals(createdAt, message.createdAt) &&
                Objects.equals(updatedAt, message.updatedAt);
    }

    // 重写 hashCode 方法
    @Override
    public int hashCode() {
        return Objects.hash(id, chatId, senderId, msgContent, msgType, readStatus, isRecalled, userIp, source,
                referenceMsgId, isShowTime, createdAt, updatedAt);
    }
}
