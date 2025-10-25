package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 内容大纲请求VO
 */
@Data
public class ContentOutlineRequestVo {

    @NotBlank(message = "主题不能为空")
    @Size(max = 500, message = "主题长度不能超过500个字符")
    private String topic;

    /**
     * 大纲结构：introduction-body-conclusion(引言-正文-结论),
     * chronological(时间顺序), problem-solution(问题-解决方案),
     * comparison(对比分析), custom(自定义)
     */
    private String structure = "introduction-body-conclusion";

    /**
     * 内容类型：article(文章), essay(论文), report(报告), presentation(演讲), book(书籍)
     */
    private String contentType = "article";

    /**
     * 详细程度：brief(简要), detailed(详细), comprehensive(全面)
     */
    private String detailLevel = "detailed";

    /**
     * 章节数量
     */
    private Integer chapterCount;

    /**
     * 每章小节数量
     */
    private Integer sectionCount;

    /**
     * 语言：zh(中文), en(英文), auto(自动检测)
     */
    private String language = "auto";

    /**
     * 目标受众：general(一般), professional(专业), academic(学术), casual(休闲)
     */
    private String audience = "general";

    /**
     * 额外要求或约束
     */
    @Size(max = 500, message = "额外要求长度不能超过500个字符")
    private String requirements;
}