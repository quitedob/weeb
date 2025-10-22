package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文本润色请求VO
 */
@Data
public class TextRefineRequestVo {

    @NotBlank(message = "文本内容不能为空")
    @Size(max = 5000, message = "文本内容不能超过5000个字符")
    private String content;

    /**
     * 润色类型：grammar(语法), style(风格), clarity(清晰度), all(全部)
     */
    private String refineType = "all";

    /**
     * 期望风格：formal(正式), casual(休闲), academic(学术), business(商务)
     */
    private String targetStyle = "casual";

    /**
     * 保留原文核心意思的程度：high(高度保留), medium(中度保留), low(低度保留)
     */
    private String preserveMeaning = "high";
}