package com.web.vo.comment;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建评论请求VO
 */
@Data
public class CommentCreateVo {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
    
    @NotEmpty(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字符")
    private String content;
    
    private Long parentId; // 父评论ID，可选
}