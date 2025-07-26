package com.web.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话列表类型枚举
 * 简化注释：会话类型
 */
@Getter
@AllArgsConstructor
public enum ChatListType {
    USER("USER", "用户私聊"), // Assuming "USER" or "PRIVATE" for private chats
    GROUP("GROUP", "群组聊天");

    private final String code; // String code as used in user's example (chatList.getChatType() which is String)
    private final String description;
}
