package com.web.vo.login;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class LoginVo {
    @NotBlank(message = "用户名不能为空~")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9]{2,15}$",
            message = "用户名只能包含英文字母和数字，且必须以英文字母开头，长度为[3-16]位~"
    )
    private String name;
    @NotBlank(message = "邮箱不能为空~")
    @Email(message = "邮箱格式不正确~")
    private String email;
}
