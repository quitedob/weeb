package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文本校对请求VO
 */
@Data
public class TextProofreadRequestVo {

    @NotBlank(message = "文本内容不能为空")
    @Size(max = 10000, message = "文本内容不能超过10000个字符")
    private String content;

    /**
     * 校对类型：grammar(语法), spelling(拼写), punctuation(标点), style(风格), all(全部)
     */
    private String checkType = "all";

    /**
     * 文本类型：article(文章), email(邮件), academic(学术), casual(休闲), business(商务)
     */
    private String textType = "article";

    /**
     * 语言：zh(中文), en(英文), auto(自动检测)
     */
    private String language = "auto";

    /**
     * 严格程度：strict(严格), normal(正常), relaxed(宽松)
     */
    private String strictness = "normal";

    /**
     * 是否返回修改建议和原因
     */
    private Boolean includeSuggestions = true;
}