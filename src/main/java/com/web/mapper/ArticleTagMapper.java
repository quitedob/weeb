package com.web.mapper;

import com.web.model.ArticleTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 文章标签相关的数据库访问层接口
 */
@Mapper
public interface ArticleTagMapper {

    /**
     * 获取所有标签
     * @return 标签列表
     */
    List<ArticleTag> getAllTags();

    /**
     * 根据ID获取标签
     * @param id 标签ID
     * @return 标签对象
     */
    ArticleTag getTagById(@Param("id") Long id);

    /**
     * 根据名称获取标签
     * @param tagName 标签名称
     * @return 标签对象
     */
    ArticleTag getTagByName(@Param("tagName") String tagName);

    /**
     * 添加标签
     * @param tag 标签对象
     * @return 影响的行数
     */
    int insertTag(ArticleTag tag);

    /**
     * 获取文章的标签列表
     * @param articleId 文章ID
     * @return 标签列表
     */
    List<ArticleTag> getTagsByArticleId(@Param("articleId") Long articleId);

    /**
     * 为文章添加标签关联
     * @param articleId 文章ID
     * @param tagId 标签ID
     * @return 影响的行数
     */
    int addTagToArticle(@Param("articleId") Long articleId, @Param("tagId") Long tagId);

    /**
     * 移除文章的标签关联
     * @param articleId 文章ID
     * @param tagId 标签ID
     * @return 影响的行数
     */
    int removeTagFromArticle(@Param("articleId") Long articleId, @Param("tagId") Long tagId);

    /**
     * 清空文章的所有标签关联
     * @param articleId 文章ID
     * @return 影响的行数
     */
    int clearArticleTags(@Param("articleId") Long articleId);
}
