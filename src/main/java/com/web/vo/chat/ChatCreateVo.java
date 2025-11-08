package com.web.vo.chat;

import jakarta.validation.constraints.NotBlank;

/**
 * 聊天创建视图对象
 * 用于接收创建聊天会话的请求数据
 * 
 * ✅ HARDENED: Changed targetId from Long to String to handle malformed input gracefully
 */
public class ChatCreateVo {

    @NotBlank(message = "目标用户ID不能为空")
    private String targetId; // 目标用户ID (String for robust validation)

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
