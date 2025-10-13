package com.web.mapper;

import com.web.model.ArticleCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 文章分类相关的数据库访问层接口
 */
@Mapper
public interface ArticleCategoryMapper {

    /**
     * 获取所有分类
     * @return 分类列表
     */
    List<ArticleCategory> getAllCategories();

    /**
     * 根据ID获取分类
     * @param id 分类ID
     * @return 分类对象
     */
    ArticleCategory getCategoryById(@Param("id") Long id);

    /**
     * 添加分类
     * @param category 分类对象
     * @return 影响的行数
     */
    int insertCategory(ArticleCategory category);

    /**
     * 更新分类
     * @param category 分类对象
     * @return 影响的行数
     */
    int updateCategory(ArticleCategory category);

    /**
     * 删除分类
     * @param id 分类ID
     * @return 影响的行数
     */
    int deleteCategory(@Param("id") Long id);
}
