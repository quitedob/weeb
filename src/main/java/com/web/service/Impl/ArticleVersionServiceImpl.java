package com.web.service.Impl;

import com.web.mapper.ArticleMapper;
import com.web.mapper.ArticleVersionMapper;
import com.web.mapper.UserMapper;
import com.web.model.Article;
import com.web.model.ArticleVersion;
import com.web.model.User;
import com.web.service.ArticleService;
import com.web.service.ArticleVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章版本服务实现类
 */
@Slf4j
@Service
@Transactional
public class ArticleVersionServiceImpl implements ArticleVersionService {

    @Autowired
    private ArticleVersionMapper articleVersionMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleService articleService;

    @Override
    @Transactional
    public ArticleVersion createVersion(ArticleVersion articleVersion) {
        try {
            // 获取下一个版本号
            Integer latestVersion = articleVersionMapper.selectLatestVersionNumber(articleVersion.getArticleId());
            if (latestVersion == null) {
                articleVersion.setVersionNumber(1);
            } else {
                articleVersion.setVersionNumber(latestVersion + 1);
            }

            // 生成内容哈希
            String contentHash = generateContentHash(articleVersion.getContent());
            articleVersion.setContentHash(contentHash);

            // 检查内容是否已存在
            if (articleVersionMapper.existsContentHash(articleVersion.getArticleId(), contentHash)) {
                log.info("内容未变化，跳过版本创建: articleId={}", articleVersion.getArticleId());
                return null;
            }

            // 设置创建者信息
            User user = userMapper.selectById(articleVersion.getCreatedBy());
            if (user != null) {
                articleVersion.setCreatedByUsername(user.getUsername());
            }

            // 计算字符变化
            if (latestVersion != null) {
                Map<String, Object> previousVersion = articleVersionMapper.selectArticleVersion(
                        articleVersion.getArticleId(), latestVersion);
                if (previousVersion != null) {
                    String previousContent = (String) previousVersion.get("content");
                    if (previousContent != null && articleVersion.getContent() != null) {
                        int characterChange = articleVersion.getContent().length() - previousContent.length();
                        articleVersion.setCharacterChange(characterChange);
                    }
                }
            }

            // 设置版本大小
            if (articleVersion.getContent() != null) {
                articleVersion.setSize(articleVersion.getContent().length());
            }

            // 插入版本记录
            int result = articleVersionMapper.insert(articleVersion);
            if (result > 0) {
                log.info("创建文章版本成功: articleId={}, version={}",
                        articleVersion.getArticleId(), articleVersion.getVersionNumber());
                return articleVersion;
            } else {
                throw new com.web.exception.WeebException("创建文章版本失败");
            }
        } catch (Exception e) {
            log.error("创建文章版本失败: articleId={}", articleVersion.getArticleId(), e);
            throw new com.web.exception.WeebException("创建文章版本失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getArticleVersions(Long articleId) {
        try {
            return articleVersionMapper.selectVersionsByArticleId(articleId);
        } catch (Exception e) {
            log.error("获取文章版本列表失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("获取文章版本列表失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getArticleVersion(Long articleId, Integer versionNumber) {
        try {
            Map<String, Object> version = articleVersionMapper.selectArticleVersion(articleId, versionNumber);
            if (version == null) {
                throw new com.web.exception.WeebException("版本不存在: articleId=" + articleId + ", version=" + versionNumber);
            }
            return version;
        } catch (Exception e) {
            log.error("获取文章版本失败: articleId={}, version={}", articleId, versionNumber, e);
            throw new com.web.exception.WeebException("获取文章版本失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getLatestVersion(Long articleId) {
        try {
            Integer latestVersion = articleVersionMapper.selectLatestVersionNumber(articleId);
            if (latestVersion == null) {
                throw new com.web.exception.WeebException("文章不存在版本: articleId=" + articleId);
            }
            return getArticleVersion(articleId, latestVersion);
        } catch (Exception e) {
            log.error("获取文章最新版本失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("获取文章最新版本失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getLatestPublishedVersion(Long articleId) {
        try {
            Integer latestPublishedVersion = articleVersionMapper.selectLatestPublishedVersionNumber(articleId);
            if (latestPublishedVersion == null) {
                throw new com.web.exception.WeebException("文章不存在发布版本: articleId=" + articleId);
            }
            return getArticleVersion(articleId, latestPublishedVersion);
        } catch (Exception e) {
            log.error("获取文章最新发布版本失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("获取文章最新发布版本失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getMajorVersions(Long articleId) {
        try {
            return articleVersionMapper.selectMajorVersionsByArticleId(articleId);
        } catch (Exception e) {
            log.error("获取文章主要版本失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("获取文章主要版本失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean rollbackToVersion(Long articleId, Integer versionNumber, Long userId, String rollbackNote) {
        try {
            // 获取目标版本
            Map<String, Object> version = getArticleVersion(articleId, versionNumber);

            // 创建回滚版本
            ArticleVersion rollbackVersion = new ArticleVersion();
            rollbackVersion.setArticleId(articleId);
            rollbackVersion.setTitle((String) version.get("title"));
            rollbackVersion.setContent((String) version.get("content"));
            rollbackVersion.setSummary((String) version.get("summary"));
            rollbackVersion.setArticleLink((String) version.get("article_link"));
            rollbackVersion.setCategoryId(((Number) version.get("category_id")).longValue());
            rollbackVersion.setCreatedBy(userId);
            rollbackVersion.setChangeType(ArticleVersion.ChangeType.UPDATE);
            rollbackVersion.setVersionNote("回滚到版本 " + versionNumber + ": " + rollbackNote);
            rollbackVersion.setChangeSummary("回滚到版本 " + versionNumber);
            rollbackVersion.setIsMajorVersion(true);
            rollbackVersion.setIsAutoSave(false);

            // 创建回滚版本
            ArticleVersion createdVersion = createVersion(rollbackVersion);
            if (createdVersion != null) {
                // 更新文章内容
                Article article = articleMapper.selectById(articleId);
                if (article != null) {
                    article.setArticleTitle((String) version.get("title"));
                    article.setArticleContent((String) version.get("content"));
                    article.setUpdatedAt(java.time.LocalDateTime.now());
                    articleMapper.updateById(article);

                    log.info("文章回滚成功: articleId={}, fromVersion={}, toVersion={}",
                            articleId, versionNumber, createdVersion.getVersionNumber());
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("文章回滚失败: articleId={}, version={}", articleId, versionNumber, e);
            throw new com.web.exception.WeebException("文章回滚失败: " + e.getMessage());
        }
    }

    @Override
    public boolean autoSaveVersion(Long articleId, String title, String content, Long userId) {
        try {
            // 检查内容是否有变化
            String contentHash = generateContentHash(content);
            if (!hasContentChanged(articleId, contentHash)) {
                return true; // 内容未变化，无需保存
            }

            ArticleVersion autoSaveVersion = new ArticleVersion();
            autoSaveVersion.setArticleId(articleId);
            autoSaveVersion.setTitle(title);
            autoSaveVersion.setContent(content);
            autoSaveVersion.setCreatedBy(userId);
            autoSaveVersion.setChangeType(ArticleVersion.ChangeType.AUTO_SAVE);
            autoSaveVersion.setVersionNote("自动保存");
            autoSaveVersion.setIsMajorVersion(false);
            autoSaveVersion.setIsAutoSave(true);

            ArticleVersion createdVersion = createVersion(autoSaveVersion);
            return createdVersion != null;
        } catch (Exception e) {
            log.error("自动保存版本失败: articleId={}", articleId, e);
            return false;
        }
    }

    @Override
    public Integer createMajorVersion(Long articleId, String title, String content, Long userId, String changeNote) {
        try {
            ArticleVersion majorVersion = new ArticleVersion();
            majorVersion.setArticleId(articleId);
            majorVersion.setTitle(title);
            majorVersion.setContent(content);
            majorVersion.setCreatedBy(userId);
            majorVersion.setChangeType(ArticleVersion.ChangeType.MAJOR_EDIT);
            majorVersion.setVersionNote(changeNote);
            majorVersion.setChangeSummary(changeNote);
            majorVersion.setIsMajorVersion(true);
            majorVersion.setIsAutoSave(false);

            ArticleVersion createdVersion = createVersion(majorVersion);
            return createdVersion != null ? createdVersion.getVersionNumber() : null;
        } catch (Exception e) {
            log.error("创建主要版本失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("创建主要版本失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> compareVersions(Long articleId, Integer fromVersion, Integer toVersion) {
        try {
            Map<String, Object> fromVersionData = getArticleVersion(articleId, fromVersion);
            Map<String, Object> toVersionData = getArticleVersion(articleId, toVersion);

            Map<String, Object> comparison = new HashMap<>();
            comparison.put("fromVersion", fromVersionData);
            comparison.put("toVersion", toVersionData);

            // 比较标题
            String fromTitle = (String) fromVersionData.get("title");
            String toTitle = (String) toVersionData.get("title");
            comparison.put("titleChanged", !Objects.equals(fromTitle, toTitle));

            // 比较内容
            String fromContent = (String) fromVersionData.get("content");
            String toContent = (String) toVersionData.get("content");
            comparison.put("contentChanged", !Objects.equals(fromContent, toContent));

            // 计算内容差异
            if (fromContent != null && toContent != null) {
                int lengthDiff = toContent.length() - fromContent.length();
                comparison.put("lengthDifference", lengthDiff);
                comparison.put("fromLength", fromContent.length());
                comparison.put("toLength", toContent.length());
            }

            // 获取中间版本
            List<ArticleVersion> intermediateVersions = articleVersionMapper.selectVersionsBetween(
                    articleId, Math.min(fromVersion, toVersion), Math.max(fromVersion, toVersion));
            comparison.put("intermediateVersions", intermediateVersions.size() - 2); // 减去起始和结束版本

            return comparison;
        } catch (Exception e) {
            log.error("比较版本失败: articleId={}, from={}, to={}", articleId, fromVersion, toVersion, e);
            throw new com.web.exception.WeebException("比较版本失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getUserRecentVersions(Long userId, int limit) {
        try {
            return articleVersionMapper.selectRecentVersionsByUser(userId, limit);
        } catch (Exception e) {
            log.error("获取用户最近版本失败: userId={}", userId, e);
            throw new com.web.exception.WeebException("获取用户最近版本失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getVersionStatistics(Long articleId) {
        try {
            Map<String, Object> statistics = new HashMap<>();

            // 基础统计
            statistics.put("totalVersions", articleVersionMapper.countVersionsByArticleId(articleId));
            statistics.put("latestVersion", articleVersionMapper.selectLatestVersionNumber(articleId));
            statistics.put("latestPublishedVersion", articleVersionMapper.selectLatestPublishedVersionNumber(articleId));

            // 变更统计
            statistics.put("changeStatistics", articleVersionMapper.selectChangeStatistics(articleId));

            // 时间统计（最近30天）
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            Map<String, Object> timeStats = articleVersionMapper.selectVersionStatistics(
                    articleId, thirtyDaysAgo, LocalDateTime.now());
            statistics.put("recentStatistics", timeStats);

            return statistics;
        } catch (Exception e) {
            log.error("获取版本统计失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("获取版本统计失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public int cleanupOldVersions(Long articleId, int keepCount) {
        try {
            int deletedCount = articleVersionMapper.cleanupOldVersions(articleId, keepCount);
            log.info("清理旧版本完成: articleId={}, deletedCount={}", articleId, deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("清理旧版本失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("清理旧版本失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public int deleteAllVersions(Long articleId) {
        try {
            int deletedCount = articleVersionMapper.deleteVersionsByArticleId(articleId);
            log.info("删除所有版本完成: articleId={}, deletedCount={}", articleId, deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("删除所有版本失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("删除所有版本失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> searchVersions(Long articleId, String keyword) {
        try {
            // 输入验证防止SQL注入
            if (articleId == null || articleId <= 0) {
                throw new com.web.exception.WeebException("无效的文章ID");
            }
            if (keyword != null) {
                keyword = keyword.trim();
                if (keyword.length() > 100) {
                    keyword = keyword.substring(0, 100);
                }
                // 移除潜在的危险字符
                keyword = keyword.replaceAll("[';\"\\-\\-]", "");
            }

            return articleVersionMapper.searchVersionsByKeyword(articleId, keyword, 50);
        } catch (Exception e) {
            log.error("搜索版本失败: articleId={}, keyword={}", articleId, keyword, e);
            throw new com.web.exception.WeebException("搜索版本失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getAutoSaveVersions(Long articleId, int hours) {
        try {
            return articleVersionMapper.selectAutoSaveVersions(articleId, hours)
                    .stream()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取自动保存版本失败: articleId={}, hours={}", articleId, hours, e);
            throw new com.web.exception.WeebException("获取自动保存版本失败: " + e.getMessage());
        }
    }

    @Override
    public boolean hasContentChanged(Long articleId, String contentHash) {
        try {
            return !articleVersionMapper.existsContentHash(articleId, contentHash);
        } catch (Exception e) {
            log.error("检查内容变化失败: articleId={}", articleId, e);
            return true; // 出错时默认认为内容有变化
        }
    }

    @Override
    public List<Map<String, Object>> getChangeStatistics(Long articleId) {
        try {
            return articleVersionMapper.selectChangeStatistics(articleId);
        } catch (Exception e) {
            log.error("获取变更统计失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("获取变更统计失败: " + e.getMessage());
        }
    }

    @Override
    public String exportVersionHistory(Long articleId, String format) {
        try {
            List<Map<String, Object>> versions = getArticleVersions(articleId);

            switch (format.toLowerCase()) {
                case "json":
                    return exportAsJson(versions);
                case "xml":
                    return exportAsXml(versions);
                case "txt":
                    return exportAsText(versions);
                default:
                    throw new com.web.exception.WeebException("不支持的导出格式: " + format);
            }
        } catch (Exception e) {
            log.error("导出版本历史失败: articleId={}, format={}", articleId, format, e);
            throw new com.web.exception.WeebException("导出版本历史失败: " + e.getMessage());
        }
    }

    @Override
    public boolean restoreVersion(Long versionId) {
        try {
            ArticleVersion version = articleVersionMapper.selectById(versionId);
            if (version == null) {
                throw new com.web.exception.WeebException("版本不存在: versionId=" + versionId);
            }

            // 这里可以实现恢复逻辑，比如将版本状态从deleted改为正常
            log.info("恢复版本: versionId={}", versionId);
            return true;
        } catch (Exception e) {
            log.error("恢复版本失败: versionId={}", versionId, e);
            throw new com.web.exception.WeebException("恢复版本失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getVersionTimeline(Long articleId) {
        try {
            List<Map<String, Object>> versions = getArticleVersions(articleId);

            // 构建时间线数据
            return versions.stream()
                    .map(version -> {
                        Map<String, Object> timelineItem = new HashMap<>();
                        timelineItem.put("version", version.get("version_number"));
                        timelineItem.put("timestamp", version.get("created_at"));
                        timelineItem.put("type", version.get("change_type"));
                        timelineItem.put("author", version.get("created_by_username"));
                        timelineItem.put("note", version.get("version_note"));
                        timelineItem.put("isMajor", version.get("is_major_version"));
                        return timelineItem;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取版本时间线失败: articleId={}", articleId, e);
            throw new com.web.exception.WeebException("获取版本时间线失败: " + e.getMessage());
        }
    }

    /**
     * 生成内容哈希
     */
    private String generateContentHash(String content) {
        try {
            if (content == null) {
                return "";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("生成内容哈希失败", e);
            return "";
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 导出为JSON格式
     */
    private String exportAsJson(List<Map<String, Object>> versions) {
        // 简化实现，实际应该使用JSON库
        return "{\"versions\":" + versions.toString() + "}";
    }

    /**
     * 导出为XML格式
     */
    private String exportAsXml(List<Map<String, Object>> versions) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<versions>\n");
        for (Map<String, Object> version : versions) {
            xml.append("  <version>\n");
            xml.append("    <number>").append(version.get("version_number")).append("</number>\n");
            xml.append("    <title>").append(version.get("title")).append("</title>\n");
            xml.append("    <created_at>").append(version.get("created_at")).append("</created_at>\n");
            xml.append("  </version>\n");
        }
        xml.append("</versions>");
        return xml.toString();
    }

    /**
     * 导出为文本格式
     */
    private String exportAsText(List<Map<String, Object>> versions) {
        StringBuilder text = new StringBuilder();
        text.append("Article Version History\n");
        text.append("========================\n\n");

        for (Map<String, Object> version : versions) {
            text.append("Version: ").append(version.get("version_number")).append("\n");
            text.append("Title: ").append(version.get("title")).append("\n");
            text.append("Created: ").append(version.get("created_at")).append("\n");
            text.append("Author: ").append(version.get("created_by_username")).append("\n");
            text.append("Note: ").append(version.get("version_note")).append("\n");
            text.append("------------------------\n\n");
        }

        return text.toString();
    }
}