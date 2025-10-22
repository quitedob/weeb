package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章总结请求VO
 */
@Data
public class ArticleSummaryRequestVo {

    @NotBlank(message = "文章内容不能为空")
    @Size(max = 10000, message = "文章内容不能超过10000个字符")
    private String content;

    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    /**
     * 总结长度限制
     */
    private Integer maxLength = 200;

    /**
     * 总结风格：formal(正式), casual(休闲), technical(技术)
     */
    private String style = "casual";
}