package com.web.vo.chatlist;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class ChatListCreateVo {
    @NotBlank(message = "目标不能为空~")
    private Long targetId;
}
