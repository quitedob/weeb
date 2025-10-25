package com.web.vo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * 关键词提取请求VO
 */
@Data
public class KeywordsExtractionRequestVo {

    @NotBlank(message = "文本内容不能为空")
    @Size(max = 8000, message = "文本内容不能超过8000个字符")
    private String content;

    @Min(value = 1, message = "提取数量最少为1个")
    @Max(value = 20, message = "提取数量最多为20个")
    private Integer count = 10;

    /**
     * 语言：zh(中文), en(英文), auto(自动检测)
     */
    private String language = "auto";

    /**
     * 关键词类型：noun(名词), verb(动词), adjective(形容词), all(全部)
     */
    private String type = "all";

    /**
     * 是否返回权重分数
     */
    private Boolean includeScore = false;
}