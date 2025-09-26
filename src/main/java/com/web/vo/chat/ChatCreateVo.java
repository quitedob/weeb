package com.web.vo.chat;

import jakarta.validation.constraints.NotNull;

/**
 * 聊天创建视图对象
 * 用于接收创建聊天会话的请求数据
 */
public class ChatCreateVo {

    @NotNull(message = "目标用户ID不能为空")
    private Long targetId; // 目标用户ID

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}

