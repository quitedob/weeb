package com.web.vo.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 消息串回复请求VO
 */
@Data
public class ThreadReplyRequestVo {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 2000, message = "回复内容长度不能超过2000个字符")
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

    /**
     * 是否静默回复（不通知其他用户）
     */
    private Boolean silent = false;
}