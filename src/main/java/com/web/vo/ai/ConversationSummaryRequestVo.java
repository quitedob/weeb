package com.web.vo.ai;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 对话摘要请求VO
 */
@Data
public class ConversationSummaryRequestVo {

    @NotEmpty(message = "消息列表不能为空")
    private List<ConversationMessage> messages;

    @Min(value = 50, message = "摘要长度最少为50个字符")
    @Max(value = 500, message = "摘要长度最多为500个字符")
    private Integer maxLength = 200;

    /**
     * 摘要风格：bullet(要点列表), paragraph(段落), timeline(时间线)
     */
    private String style = "paragraph";

    /**
     * 摘要重点：decisions(决策), questions(问题), actions(行动), all(全部)
     */
    private String focus = "all";

    /**
     * 语言：zh(中文), en(英文), auto(自动检测)
     */
    private String language = "auto";

    /**
     * 对话消息内部类
     */
    @Data
    public static class ConversationMessage {
        private String role; // user, assistant
        private String content;
        private Long timestamp;
    }
}