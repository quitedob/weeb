package com.web.model;

import lombok.Data;
import jakarta.persistence.*; // Using jakarta
import java.io.Serializable;
import java.util.Date;

/**
 * 群组成员实体类
 * 简化注释：群成员实体
 */
@Data
@Entity
@Table(name = "group_member")
public class GroupMember implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 唯一ID

    private Long groupId; // 群组ID (Changed to Long)

    private Long userId; // 用户ID (Changed to Long)

    /**
     * 成员角色
     * @see com.web.constant.GroupRole
     * 简化注释：成员角色
     */
    private Integer role;

    private Date createTime; // 加入时间
}
