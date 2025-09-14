package com.web.vo.message;

import com.web.constant.MessageType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class SendMessageVo {
    @NotBlank(message = "目标用户不能为空~")
    private String targetId;
    private String source;
    private String type = MessageType.Text;
    @NotBlank(message = "消息内容不能为空~")
    private String msgContent;
    private String referenceMsgId;
    private String userIp;
}
