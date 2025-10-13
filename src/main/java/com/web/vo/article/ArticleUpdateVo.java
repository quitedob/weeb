package com.web.vo.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章更新请求VO
 */
@Data
public class ArticleUpdateVo {
    
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题不能超过200个字符")
    private String articleTitle;
    
    @NotBlank(message = "文章内容不能为空")
    @Size(max = 10000, message = "文章内容不能超过10000个字符")
    private String articleContent;
    
    @Size(max = 500, message = "文章链接不能超过500个字符")
    private String articleLink;
    
    /**
     * 文章分类ID
     */
    private Long categoryId;
    
    @Size(max = 100, message = "标签不能超过100个字符")
    private String tags;
    
    /**
     * 文章状态：0-草稿，1-已发布
     */
    private Integer status;
}