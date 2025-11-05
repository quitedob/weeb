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
    
    private String clientMessageId; // 客户端消息ID（用于幂等性）

    private Long senderId; // 发送者ID
    private Long receiverId; // 接收者ID（私聊时使用）
    private Long groupId; // 群组ID（群聊时使用）
    private Long chatId;   // 会话列表ID

    @TableField(typeHandler = JacksonTypeHandler.class) // MyBatis Plus自动处理JSON转换
    private TextMessageContent content; // 消息内容，结构化对象

    private Integer messageType; // 消息类型
    
    /**
     * 统一消息状态字段
     * 0=sending(发送中), 1=sent(已发送), 2=delivered(已送达), 3=read(已读), 4=failed(失败)
     */
    private Integer status;
    
    /**
     * @deprecated 使用status字段代替，保留用于向后兼容
     */
    @Deprecated
    private Integer readStatus; // 已读状态
    
    /**
     * @deprecated 使用status字段代替，保留用于向后兼容
     */
    @Deprecated
    private Integer isRead; // 是否已读：0未读，1已读
    
    /**
     * @deprecated 使用独立的isRecalled字段
     */
    private Integer isRecalled; // 是否撤回：0未撤回，1已撤回
    private String userIp; // 用户IP
    private String source; // 消息来源
    private Integer isShowTime; // 是否显示时间
    private Timestamp createdAt; // 创建时间
    private Timestamp updatedAt; // 更新时间

    private Long replyToMessageId; // 回复的消息ID

    private Long threadId; // 话题ID，用于消息线程
    
    // 消息类型常量
    public static final String TYPE_PRIVATE = "PRIVATE";
    public static final String TYPE_GROUP = "GROUP";
    
    // 消息状态常量
    public static final int STATUS_SENDING = 0;    // 发送中
    public static final int STATUS_SENT = 1;       // 已发送
    public static final int STATUS_DELIVERED = 2;  // 已送达
    public static final int STATUS_READ = 3;       // 已读
    public static final int STATUS_FAILED = 4;     // 失败

    /**
     * 获取用户ID (别名方法，兼容性考虑)
     * @return 用户ID
     */
    public Long getUserId() {
        return this.senderId;
    }

    /**
     * 设置用户ID (别名方法，兼容性考虑)
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.senderId = userId;
    }

    /**
     * 获取回复消息ID (别名方法，兼容性考虑)
     * @return 回复消息ID
     */
    public Long getReplyToId() {
        return this.replyToMessageId;
    }

    /**
     * 设置回复消息ID (别名方法，兼容性考虑)
     * @param replyToId 回复消息ID
     */
    public void setReplyToId(Long replyToId) {
        this.replyToMessageId = replyToId;
    }

    /**
     * 获取聊天列表ID (别名方法，兼容性考虑)
     * @return 聊天列表ID
     */
    public Long getChatListId() {
        return this.chatId;
    }

    /**
     * 设置聊天列表ID (别名方法，兼容性考虑)
     * @param chatListId 聊天列表ID
     */
    public void setChatListId(Long chatListId) {
        this.chatId = chatListId;
    }
    
    /**
     * 获取消息类型（字符串形式）
     * @return 消息类型
     */
    public String getType() {
        if (this.groupId != null && this.groupId > 0) {
            return TYPE_GROUP;
        }
        return TYPE_PRIVATE;
    }
    
    /**
     * 设置消息类型（字符串形式）
     * @param type 消息类型
     */
    public void setType(String type) {
        // 根据类型设置groupId或receiverId
        if (TYPE_GROUP.equals(type)) {
            // 群聊消息，groupId会单独设置
        } else {
            // 私聊消息，receiverId会单独设置
        }
    }
}
