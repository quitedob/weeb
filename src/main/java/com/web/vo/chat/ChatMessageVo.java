package com.web.vo.chat;

import jakarta.validation.constraints.NotNull;

/**
 * 聊天消息视图对象
 * 用于接收发送消息的请求数据
 */
public class ChatMessageVo {

    @NotNull(message = "消息内容不能为空")
    private Object content; // 消息内容（JSON格式）

    private Integer messageType; // 消息类型：1文本，2图片，3文件等

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }
}

