package com.web.vo.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 消息串创建请求VO
 */
@Data
public class ThreadCreationRequestVo {

    @NotNull(message = "父消息ID不能为空")
    private Long parentMessageId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容长度不能超过5000个字符")
    private String content;

    /**
     * 消息类型：text, image, file, link等
     */
    private String contentType = "text";

    /**
     * 是否@相关人员
     */
    private String mentions;

    /**
     * 附件信息
     */
    private Object attachments;
}