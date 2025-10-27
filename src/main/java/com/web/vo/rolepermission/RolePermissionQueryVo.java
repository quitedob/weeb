package com.web.vo.rolepermission;

import lombok.Data;

/**
 * 角色权限查询VO
 */
@Data
public class RolePermissionQueryVo {

    /**
     * 角色ID（可选）
     */
    private Long roleId;

    /**
     * 权限ID（可选）
     */
    private Long permissionId;

    /**
     * 状态（可选）
     * 0: 无效, 1: 有效
     */
    private Integer status;

    /**
     * 页码（默认1）
     */
    private int page = 1;

    /**
     * 每页大小（默认10）
     */
    private int pageSize = 10;
}
