package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.ArticleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 文章评论Mapper接口
 */
@Mapper
public interface ArticleCommentMapper extends BaseMapper<ArticleComment> {
    
    /**
     * 获取文章评论列表（包含用户信息）
     * @param articleId 文章ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 评论列表
     */
    List<Map<String, Object>> getCommentsWithUserInfo(@Param("articleId") Long articleId, 
                                                      @Param("offset") int offset, 
                                                      @Param("limit") int limit);
    
    /**
     * 获取文章评论总数
     * @param articleId 文章ID
     * @return 评论总数
     */
    int getCommentCount(@Param("articleId") Long articleId);
}