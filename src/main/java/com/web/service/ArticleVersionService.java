package com.web.service;

import com.web.model.ArticleVersion;

import java.util.List;
import java.util.Map;

/**
 * 文章版本服务接口（简化版）
 * 只保留基本的版本记录功能，移除复杂的版本比较和回滚功能
 */
public interface ArticleVersionService {

    /**
     * 创建文章版本（简化版）
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @param userId 用户ID
     * @return 是否创建成功
     */
    boolean createVersion(Long articleId, String title, String content, Long userId);

    /**
     * 获取文章的所有版本（简化版）
     * @param articleId 文章ID
     * @return 版本列表
     */
    List<Map<String, Object>> getArticleVersions(Long articleId);

    /**
     * 获取文章的最新版本
     * @param articleId 文章ID
     * @return 最新版本信息
     */
    Map<String, Object> getLatestVersion(Long articleId);

    /**
     * 自动保存文章版本（简化版）
     * 只在内容有显著变化时保存
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @param userId 用户ID
     * @return 是否保存成功
     */
    boolean autoSaveVersion(Long articleId, String title, String content, Long userId);

    /**
     * 获取版本统计信息（简化版）
     * @param articleId 文章ID
     * @return 基本统计信息
     */
    Map<String, Object> getVersionStatistics(Long articleId);

    /**
     * 清理旧版本（简化版）
     * 只保留最近的10个版本
     * @param articleId 文章ID
     * @return 清理的版本数量
     */
    int cleanupOldVersions(Long articleId);

    /**
     * 删除文章的所有版本
     * @param articleId 文章ID
     * @return 删除的版本数量
     */
    int deleteAllVersions(Long articleId);

    // ==================== 已移除的复杂功能 ====================
    // 以下功能因使用率低且复杂度高而被移除：
    // - 版本比较 (compareVersions)
    // - 版本回滚 (rollbackToVersion)
    // - 主要版本管理 (createMajorVersion, getMajorVersions)
    // - 版本搜索 (searchVersions)
    // - 版本导出 (exportVersionHistory)
    // - 版本恢复 (restoreVersion)
    // - 版本时间线 (getVersionTimeline)
    // - 变更统计 (getChangeStatistics)
}