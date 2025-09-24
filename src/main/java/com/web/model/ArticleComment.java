package com.web.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文章评论实体类
 */
@Data
@TableName("article_comment")
public class ArticleComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long articleId;     // 文章ID
    private Long userId;        // 评论者ID
    private String content;     // 评论内容
    private Long parentId;      // 父评论ID（用于回复）
    private Date createdAt;     // 创建时间
    private Date updatedAt;     // 更新时间
}