package com.web.vo.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class UpdateUserVo {
    @NotBlank(message = "用户名不能为空~")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9]{2,15}$",
            message = "用户名只能包含英文字母和数字，且必须以英文字母开头，长度为[3-16]位~"
    )
    private String name;
    private String avatar;
}
