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