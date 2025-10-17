package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 角色权限关联实体类
 * 表示角色与权限的多对多关系
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RolePermission extends BaseEntity {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 权限ID
     */
    private Long permissionId;

    /**
     * 关联状态（0: 无效, 1: 有效）
     */
    private Integer status;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 关联的权限信息（不持久化到数据库，只在查询时填充）
     */
    private Permission permission;

    public RolePermission() {
        this.status = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public RolePermission(Long roleId, Long permissionId) {
        this();
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public RolePermission(Long roleId, Long permissionId, Long createdBy) {
        this();
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.createdBy = createdBy;
    }
}
