package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文本翻译请求VO
 */
@Data
public class TextTranslationRequestVo {

    @NotBlank(message = "文本内容不能为空")
    @Size(max = 5000, message = "文本内容不能超过5000个字符")
    private String content;

    @NotBlank(message = "目标语言不能为空")
    @Size(max = 10, message = "目标语言代码不能超过10个字符")
    private String targetLanguage;

    /**
     * 源语言：auto(自动检测) 或具体语言代码
     */
    private String sourceLanguage = "auto";

    /**
     * 翻译风格：formal(正式), casual(休闲), literary(文学)
     */
    private String style = "casual";

    /**
     * 是否返回多种翻译选项
     */
    private Boolean multipleOptions = false;
}