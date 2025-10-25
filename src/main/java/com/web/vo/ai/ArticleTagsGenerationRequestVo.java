package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * 文章标签生成请求VO
 */
@Data
public class ArticleTagsGenerationRequestVo {

    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000个字符")
    private String content;

    @Min(value = 1, message = "标签数量最少为1个")
    @Max(value = 15, message = "标签数量最多为15个")
    private Integer count = 5;

    /**
     * 标签类型：topic(主题), category(分类), emotion(情感), all(全部)
     */
    private String type = "all";

    /**
     * 是否包含已有标签
     */
    private String existingTags;

    /**
     * 语言：zh(中文), en(英文), auto(自动检测)
     */
    private String language = "auto";

    /**
     * 是否返回标签权重
     */
    private Boolean includeWeight = false;
}