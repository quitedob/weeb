package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息反应实体类
 * 简化注释：消息反应实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "message_reaction") // MyBatis Plus注解
public class MessageReaction implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO) // MyBatis Plus主键注解
    private Long id; // 反应ID

    private Long messageId; // 消息ID
    private Long userId; // 用户ID
    private String reactionType; // 反应类型 (如: 👍, ❤️, 😂)
    private Date createTime; // 创建时间

    // 默认构造函数
    public MessageReaction() {
        this.createTime = new Date();
    }

    // 创建时的构造函数
    public MessageReaction(Long messageId, Long userId, String reactionType) {
        this.messageId = messageId;
        this.userId = userId;
        this.reactionType = reactionType;
        this.createTime = new Date();
    }
}
