package com.web.vo.login;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class VerifyVo {
    @NotBlank(message = "密码不能为空")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}