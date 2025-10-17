package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体类
 * 表示用户与角色的多对多关系
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserRole extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 关联状态（0: 无效, 1: 有效）
     */
    private Integer status;

    /**
     * 分配人ID（谁给这个用户分配了这个角色）
     */
    private Long assignedBy;

    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;

    /**
     * 到期时间（可选，用于临时角色分配）
     */
    private LocalDateTime expiresAt;

    /**
     * 关联的角色信息（不持久化到数据库，只在查询时填充）
     */
    private Role role;

    public UserRole() {
        this.status = 1;
        this.assignedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserRole(Long userId, Long roleId) {
        this();
        this.userId = userId;
        this.roleId = roleId;
    }

    public UserRole(Long userId, Long roleId, Long assignedBy) {
        this();
        this.userId = userId;
        this.roleId = roleId;
        this.assignedBy = assignedBy;
    }
}
