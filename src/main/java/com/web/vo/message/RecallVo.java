package com.web.vo.message;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RecallVo {
    @NotBlank(message = "消息不能为空~")
    private String msgId;
}
