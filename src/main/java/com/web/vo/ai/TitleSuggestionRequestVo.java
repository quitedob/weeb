package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标题建议请求VO
 */
@Data
public class TitleSuggestionRequestVo {

    @NotBlank(message = "内容不能为空")
    @Size(max = 8000, message = "内容长度不能超过8000个字符")
    private String content;

    @Size(max = 100, message = "当前标题长度不能超过100个字符")
    private String currentTitle;

    /**
     * 建议数量：1-10个
     */
    private Integer count = 5;

    /**
     * 标题风格：formal(正式), casual(休闲), creative(创意), technical(技术)
     */
    private String style = "casual";

    /**
     * 关键词
     */
    @Size(max = 200, message = "关键词长度不能超过200个字符")
    private String keywords;
}