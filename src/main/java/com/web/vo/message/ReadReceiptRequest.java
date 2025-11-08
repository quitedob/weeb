package com.web.vo.message;

import lombok.Data;

/**
 * 已读回执请求对象
 */
@Data
public class ReadReceiptRequest {
    /**
     * 聊天ID（String类型，因为chat_list.id是VARCHAR）
     */
    private String chatId;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 时间戳
     */
    private String timestamp;
}
