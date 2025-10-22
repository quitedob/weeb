package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * AI 统一聊天请求的视图对象 (VO)
 */
@Data
public class ChatRequestVo {

    @NotBlank(message = "模型名称不能为空")
    private String model;

    @NotEmpty(message = "消息列表不能为空")
    private List<MessageVo> messages;

    private boolean stream = false;

    /**
     * 内部类，用于表示单条消息
     */
    @Data
    public static class MessageVo {
        @NotBlank(message = "角色不能为空")
        private String role;

        @NotBlank(message = "内容不能为空")
        private String content;

        // 可选字段，用于多模态
        private List<String> images;
    }
}
