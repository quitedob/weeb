package com.web.model;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 聊天未读计数实体类
 * 用于优化未读消息计数的性能
 */
@Data
public class ChatUnreadCount {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 聊天ID（可以是私聊或群聊）
     */
    private Long chatId;
    
    /**
     * 未读消息数
     */
    private Integer unreadCount;
    
    /**
     * 最后已读消息ID
     */
    private Long lastReadMessageId;
    
    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
