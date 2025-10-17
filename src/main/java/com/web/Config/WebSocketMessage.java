package com.web.Config;

import lombok.Data;

import java.util.Date;

/**
 * WebSocket消息实体类
 */
@Data
public class WebSocketMessage {

    /**
     * 消息类型
     * auth: 认证消息
     * chat: 聊天消息
     * heartbeat: 心跳消息
     * error: 错误消息
     * typing: 打字指示器消息
     * message_status: 消息状态更新消息
     * reaction: 消息反应消息
     * recall: 消息撤回消息
     */
    private String type;

    /**
     * 消息数据
     */
    private Object data;

    /**
     * 消息时间戳
     */
    private Date timestamp;

    /**
     * 消息ID（可选）
     */
    private String messageId;

    /**
     * 发送者ID（可选）
     */
    private Long fromUserId;

    /**
     * 接收者ID（可选）
     */
    private Long toUserId;

    /**
     * 群组ID（可选，用于群聊消息）
     */
    private Long groupId;

    /**
     * 会话ID（可选，用于标识会话）
     */
    private Long conversationId;

    /**
     * 消息状态（可选）
     * 1: 发送中, 2: 已发送, 3: 已送达, 4: 已读, 5: 已撤回
     */
    private Integer messageStatus;

    /**
     * 消息类型（可选）
     * 1: 文本消息, 2: 图片消息, 3: 文件消息, 4: 系统消息
     */
    private Integer messageType;

    public WebSocketMessage() {
        this.timestamp = new Date();
    }

    public WebSocketMessage(String type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = new Date();
    }

    public WebSocketMessage(String type, Object data, String messageId) {
        this.type = type;
        this.data = data;
        this.messageId = messageId;
        this.timestamp = new Date();
    }
}