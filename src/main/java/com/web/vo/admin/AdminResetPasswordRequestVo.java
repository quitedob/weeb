package com.web.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置密码请求VO
 */
@Data
public class AdminResetPasswordRequestVo {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "新密码长度必须在6-50个字符之间")
    private String newPassword;

    @NotBlank(message = "确认新密码不能为空")
    private String confirmPassword;

    /**
     * 重置原因（可选）
     */
    @Size(max = 200, message = "重置原因长度不能超过200个字符")
    private String reason;

    /**
     * 是否强制用户下次登录时修改密码
     */
    private Boolean forceChangeOnNextLogin = false;
}