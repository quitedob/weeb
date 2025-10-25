package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * 回复建议请求VO
 */
@Data
public class ReplySuggestionsRequestVo {

    @NotBlank(message = "原始消息不能为空")
    @Size(max = 2000, message = "原始消息长度不能超过2000个字符")
    private String originalMessage;

    /**
     * 上下文信息
     */
    @Size(max = 1000, message = "上下文长度不能超过1000个字符")
    private String context;

    @Min(value = 1, message = "建议数量最少为1个")
    @Max(value = 10, message = "建议数量最多为10个")
    private Integer count = 3;

    /**
     * 回复风格：formal(正式), casual(休闲), friendly(友好), professional(专业)
     */
    private String style = "casual";

    /**
     * 情感倾向：positive(积极), negative(消极), neutral(中立), adaptive(自适应)
     */
    private String sentiment = "adaptive";

    /**
     * 语言：zh(中文), en(英文), auto(自动检测)
     */
    private String language = "auto";
}