package com.web.vo.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 两因素认证请求VO
 */
@Data
public class TwoFactorRequestVo {

    @NotBlank(message = "验证码不能为空")
    private String verificationCode;
}