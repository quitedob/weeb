package com.web.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 消息反应实体类
 */
@Data
@TableName("message_reaction")
public class MessageReaction {
    /**
     * 反应ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 反应类型（如👍、❤️等）
     */
    private String reactionType;

    /**
     * 创建时间
     */
    private Timestamp createdAt;
}
