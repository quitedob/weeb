package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 内容合规检查请求VO
 */
@Data
public class ContentComplianceCheckRequestVo {

    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000个字符")
    private String content;

    /**
     * 检查类型：spam(垃圾信息), sensitive(敏感词), violence(暴力), adult(成人), all(全部)
     */
    private String checkType = "all";

    /**
     * 内容类型：article(文章), comment(评论), message(消息), profile(个人资料)
     */
    private String contentType = "article";

    /**
     * 严格程度：strict(严格), normal(正常), relaxed(宽松)
     */
    private String strictness = "normal";

    /**
     * 语言：zh(中文), en(英文), auto(自动检测)
     */
    private String language = "auto";

    /**
     * 是否返回详细分析
     */
    private Boolean detailed = false;
}