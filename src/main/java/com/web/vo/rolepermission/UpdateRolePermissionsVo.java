package com.web.vo.rolepermission;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新角色权限VO
 */
@Data
public class UpdateRolePermissionsVo {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 权限ID列表
     */
    @NotEmpty(message = "权限列表不能为空")
    private List<Long> permissionIds;
}
