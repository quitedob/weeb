package com.web.vo.user;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CreateUserVo {
    @NotBlank(message = "用户名不能为空~")
    private String name;
    @NotBlank(message = "邮箱不能为空~")
    private String email;
}
