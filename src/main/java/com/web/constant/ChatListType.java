package com.web.constant;

/**
 * 会话列表类型枚举
 * 简化注释：会话类型
 */
public enum ChatListType {
    USER("USER", "用户私聊"), // Assuming "USER" or "PRIVATE" for private chats
    GROUP("GROUP", "群组聊天");

    private final String code; // String code as used in user's example (chatList.getChatType() which is String)
    private final String description;

    ChatListType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
