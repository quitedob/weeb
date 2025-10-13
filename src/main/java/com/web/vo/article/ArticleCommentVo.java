package com.web.vo.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章评论请求VO
 */
@Data
public class ArticleCommentVo {
    
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000个字符")
    private String content;
    
    /**
     * 父评论ID（用于回复评论，可为空）
     */
    private Long parentId;
}
