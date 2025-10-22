package com.web.vo.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 密码修改请求VO
 */
@Data
public class PasswordChangeVo {

    @NotBlank(message = "当前密码不能为空")
    @Size(min = 6, max = 100, message = "当前密码长度必须在6-100个字符之间")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "新密码长度必须在6-50个字符之间")
    private String newPassword;

    @NotBlank(message = "确认新密码不能为空")
    private String confirmPassword;
}