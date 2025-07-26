package com.web.model;

import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date; // Using java.util.Date as per user spec for this new entity

/**
 * 消息反应实体类
 * 记录用户对消息的Emoji反应
 * 简化注释：消息反应实体
 */
@Data
@Entity
@Table(name = "message_reaction") // JPA table name
// Assuming MyBatis Plus might also be used with this entity,
// @TableName(value = "message_reaction") could be added if needed,
// but user spec for this new file didn't include it. Sticking to JPA for now.
public class MessageReaction implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 唯一ID

    private Long messageId; // 关联的消息ID

    private Integer userId; // 做出反应的用户ID

    private String emoji; // 反应的Emoji（例如，👍）

    private Date createTime; // 创建时间
}
