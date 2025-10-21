package com.web.service;

import com.web.model.ArticleVersion;

import java.util.List;
import java.util.Map;

/**
 * 文章版本服务接口
 */
public interface ArticleVersionService {

    /**
     * 创建文章版本
     * @param articleVersion 版本对象
     * @return 创建的版本对象
     */
    ArticleVersion createVersion(ArticleVersion articleVersion);

    /**
     * 获取文章的所有版本
     * @param articleId 文章ID
     * @return 版本列表
     */
    List<Map<String, Object>> getArticleVersions(Long articleId);

    /**
     * 获取文章的指定版本
     * @param articleId 文章ID
     * @param versionNumber 版本号
     * @return 版本信息
     */
    Map<String, Object> getArticleVersion(Long articleId, Integer versionNumber);

    /**
     * 获取文章的最新版本
     * @param articleId 文章ID
     * @return 最新版本信息
     */
    Map<String, Object> getLatestVersion(Long articleId);

    /**
     * 获取文章的最新发布版本
     * @param articleId 文章ID
     * @return 最新发布版本信息
     */
    Map<String, Object> getLatestPublishedVersion(Long articleId);

    /**
     * 获取文章的主要版本列表
     * @param articleId 文章ID
     * @return 主要版本列表
     */
    List<Map<String, Object>> getMajorVersions(Long articleId);

    /**
     * 回滚文章到指定版本
     * @param articleId 文章ID
     * @param versionNumber 目标版本号
     * @param userId 操作用户ID
     * @param rollbackNote 回滚说明
     * @return 是否回滚成功
     */
    boolean rollbackToVersion(Long articleId, Integer versionNumber, Long userId, String rollbackNote);

    /**
     * 自动保存文章版本
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @param userId 用户ID
     * @return 是否保存成功
     */
    boolean autoSaveVersion(Long articleId, String title, String content, Long userId);

    /**
     * 创建主要版本
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @param userId 用户ID
     * @param changeNote 变更说明
     * @return 创建的版本号
     */
    Integer createMajorVersion(Long articleId, String title, String content, Long userId, String changeNote);

    /**
     * 比较两个版本
     * @param articleId 文章ID
     * @param fromVersion 起始版本号
     * @param toVersion 结束版本号
     * @return 版本差异信息
     */
    Map<String, Object> compareVersions(Long articleId, Integer fromVersion, Integer toVersion);

    /**
     * 获取用户最近编辑的文章版本
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 版本列表
     */
    List<Map<String, Object>> getUserRecentVersions(Long userId, int limit);

    /**
     * 获取版本统计信息
     * @param articleId 文章ID
     * @return 统计信息
     */
    Map<String, Object> getVersionStatistics(Long articleId);

    /**
     * 清理旧版本
     * @param articleId 文章ID
     * @param keepCount 保留数量
     * @return 清理的版本数量
     */
    int cleanupOldVersions(Long articleId, int keepCount);

    /**
     * 删除文章的所有版本
     * @param articleId 文章ID
     * @return 删除的版本数量
     */
    int deleteAllVersions(Long articleId);

    /**
     * 搜索文章版本
     * @param articleId 文章ID
     * @param keyword 关键词
     * @return 匹配的版本列表
     */
    List<Map<String, Object>> searchVersions(Long articleId, String keyword);

    /**
     * 获取自动保存的版本
     * @param articleId 文章ID
     * @param hours 小时数
     * @return 自动保存版本列表
     */
    List<Map<String, Object>> getAutoSaveVersions(Long articleId, int hours);

    /**
     * 检查内容是否有变化
     * @param articleId 文章ID
     * @param contentHash 内容哈希
     * @return 是否有变化
     */
    boolean hasContentChanged(Long articleId, String contentHash);

    /**
     * 获取版本变更统计
     * @param articleId 文章ID
     * @return 变更统计
     */
    List<Map<String, Object>> getChangeStatistics(Long articleId);

    /**
     * 导出版本历史
     * @param articleId 文章ID
     * @param format 导出格式（json, xml, txt）
     * @return 导出的数据
     */
    String exportVersionHistory(Long articleId, String format);

    /**
     * 恢复已删除的版本
     * @param versionId 版本ID
     * @return 是否恢复成功
     */
    boolean restoreVersion(Long versionId);

    /**
     * 获取版本时间线
     * @param articleId 文章ID
     * @return 版本时间线
     */
    List<Map<String, Object>> getVersionTimeline(Long articleId);
}