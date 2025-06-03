package com.web.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.web.vo.message.TextMessageContent; // Import new VO
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 消息实体类
 * 简化注释：消息实体
 */
@Data
@EqualsAndHashCode(callSuper = false) // Common Lombok practice
@Entity // JPA annotation
@Table(name = "message") // JPA annotation for table name
@TableName(value = "message", autoResultMap = true) // MyBatis Plus: 启用MyBatis Plus的类型处理器
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id // JPA
    @TableId // MyBatis Plus
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA
    private Long id; // 消息ID

    private Long senderId; // 发送者ID (Kept from existing: senderId)
    private Long chatId;   // 会话列表ID (Kept from existing: chatId)

    @Column(columnDefinition = "TEXT") // JPA: 明确指定为TEXT类型以存储JSON
    @TableField(typeHandler = JacksonTypeHandler.class) // MyBatis Plus自动处理JSON转换
    private TextMessageContent content; // 消息内容，现在是结构化对象 (Changed from msgContent)

    // Changed from msgType (String) to messageType (Integer) to align with TextContentType
    private Integer messageType;

    // Retaining fields from existing Message.java
    private Integer readStatus;
    private Integer isRecalled;
    private String userIp;
    private String source; // 消息来源
    private Integer isShowTime;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Renamed existing 'referenceMsgId' to 'replyToMessageId'
    private Long replyToMessageId; // 回复的消息ID
}
