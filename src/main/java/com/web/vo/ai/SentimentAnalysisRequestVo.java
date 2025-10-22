package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 情感分析请求VO
 */
@Data
public class SentimentAnalysisRequestVo {

    @NotBlank(message = "文本内容不能为空")
    @Size(max = 1000, message = "文本内容不能超过1000个字符")
    private String content;

    /**
     * 分析语言：zh(中文), en(英文), auto(自动检测)
     */
    private String language = "auto";

    /**
     * 分析粒度：sentence(句子), paragraph(段落), overall(整体)
     */
    private String granularity = "overall";

    /**
     * 返回详细分析：true/false
     */
    private Boolean detailed = false;
}