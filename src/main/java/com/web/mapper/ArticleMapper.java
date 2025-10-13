package com.web.mapper;

import com.web.model.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 文章相关的数据库访问层接口
 */
@Mapper
public interface ArticleMapper {

    /**
     * 根据文章 ID 查询文章信息
     * @param id 文章 ID
     * @return 对应的文章对象
     */
    Article selectArticleById(@Param("id") Long id);

    /**
     * 插入新文章
     * @param article 文章实体
     * @return 影响的行数
     */
    int insertArticle(Article article);

    /**
     * 更新文章信息（示例，项目中未完全使用）
     * @param article 文章实体
     * @return 影响的行数
     */
    int updateArticle(Article article);

    /**
     * 获取所有文章信息
     * @return 文章列表
     */
    List<Article> getAllArticles();

    /**
     * 根据文章 ID 增加点赞数
     * @param id 文章 ID
     * @return 影响的行数
     */
    int increaseLikeCount(@Param("id") Long id);

    /**
     * 订阅用户（如：关注博主）
     * @param userId       订阅者用户 ID
     * @param targetUserId 被订阅者用户 ID
     * @return 影响的行数
     */
    int subscribeUser(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    /**
     * 更新文章内容（包括标题等）
     * @param id 文章 ID
     * @param article 包含更新内容的文章实体
     * @return 影响的行数
     */
    int updateArticleContent(@Param("id") Long id, @Param("article") Article article);

    /**
     * 为指定文章增加金币数
     * @param id 文章 ID
     * @param amount 增加的金币数
     * @return 影响的行数
     */
    int addCoin(@Param("id") Long id, @Param("amount") Double amount);

    /**
     * 为指定文章增加阅读量
     * @param id 文章 ID
     * @return 影响的行数
     */
    int increaseReadCount(@Param("id") Long id);

    /**
     * 获取用户统计信息（例如粉丝数、点赞总数等）
     * @param userId 用户 ID
     * @return 包含用户统计信息的 Map
     */
    Map<String, Object> selectUserStatsInformation(@Param("userId") Long userId);

    /**
     * 根据用户名获取用户的完整信息，包括基本信息和统计数据（JOIN查询，避免N+1问题）
     * @param username 用户名
     * @return 用户完整信息Map
     */
    Map<String, Object> selectUserCompleteInformationByUsername(@Param("username") String username);

    /**
     * 获取指定用户 ID 文章聚合统计数据（如：总点赞、总收藏、总曝光等）
     * @param userId 用户 ID
     * @return 包含统计数据的 Map
     */
    Map<String, Object> selectAggregatedStatsByUserId(@Param("userId") Long userId);

    /**
     * 更新 user_stats 表中的文章统计数据
     * @param stats 统计信息 Map
     * @return 影响的行数
     */
    int updateUserStatsArticleStats(Map<String, Object> stats);

    /**
     * 根据用户 ID 获取该用户的所有文章
     * @param userId 用户 ID
     * @return 文章列表
     */
    List<Article> selectArticlesByUserId(@Param("userId") Long userId);

    /**
     * 根据文章 ID 删除文章
     * @param id 文章 ID
     * @return 影响的行数
     */
    int deleteArticleById(@Param("id") Long id);

    /**
     * 更新指定用户的 user_stats 表的文章统计数据
     * 根据该用户在 articles 表中的所有文章累加的 likes_count, favorites_count, sponsors_count, exposure_count 来更新 user_stats 表
     * @param userId 用户 ID
     * @return 影响的行数
     */
    int updateUserStatsTotals(@Param("userId") Long userId);

    /**
     * 分页获取所有文章
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 文章列表
     */
    List<Article> getAllArticles(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计所有文章总数
     * @return 文章总数
     */
    int countAllArticles();
}
