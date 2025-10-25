package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * 内容建议请求VO
 */
@Data
public class ContentSuggestionsRequestVo {

    @NotBlank(message = "主题不能为空")
    @Size(max = 200, message = "主题长度不能超过200个字符")
    private String topic;

    /**
     * 内容类型：article(文章), blog(博客), social(社交媒体), email(邮件), report(报告)
     */
    private String contentType = "article";

    @Min(value = 1, message = "建议数量最少为1个")
    @Max(value = 10, message = "建议数量最多为10个")
    private Integer count = 3;

    /**
     * 目标受众：general(一般), professional(专业), academic(学术), casual(休闲)
     */
    private String audience = "general";

    /**
     * 内容长度：short(简短), medium(中等), long(详细)
     */
    private String length = "medium";

    /**
     * 语言：zh(中文), en(英文), auto(自动检测)
     */
    private String language = "auto";

    /**
     * 额外要求或约束
     */
    @Size(max = 500, message = "额外要求长度不能超过500个字符")
    private String requirements;
}