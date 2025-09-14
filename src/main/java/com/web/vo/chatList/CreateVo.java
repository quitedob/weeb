package com.web.vo.chatList;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CreateVo {
    @NotBlank(message = "目标不能为空~")
    private Long targetId;
}
