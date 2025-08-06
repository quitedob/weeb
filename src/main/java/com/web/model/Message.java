package com.web.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.web.vo.message.TextMessageContent; // 导入新VO
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 消息实体类
 * 简化注释：消息实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "message", autoResultMap = true) // MyBatis Plus注解，并启用类型处理器
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId // MyBatis Plus主键注解
    private Long id; // 消息ID

    private Long senderId; // 发送者ID
    private Long chatId;   // 会话列表ID

    @TableField(typeHandler = JacksonTypeHandler.class) // MyBatis Plus自动处理JSON转换
    private TextMessageContent content; // 消息内容，结构化对象

    private Integer messageType; // 消息类型

    private Integer readStatus; // 已读状态
    private Integer isRecalled; // 是否撤回
    private String userIp; // 用户IP
    private String source; // 消息来源
    private Integer isShowTime; // 是否显示时间
    private Timestamp createdAt; // 创建时间
    private Timestamp updatedAt; // 更新时间

    private Long replyToMessageId; // 回复的消息ID
}
