package com.web.service.Impl;

import com.web.mapper.ArticleMapper;
import com.web.mapper.ArticleVersionMapper;
import com.web.model.Article;
import com.web.model.ArticleVersion;
import com.web.service.ArticleVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 文章版本服务实现类（简化版）
 * 只保留基本的版本记录功能
 */
@Slf4j
@Service
@Transactional
public class ArticleVersionServiceImpl implements ArticleVersionService {

    @Autowired
    private ArticleVersionMapper articleVersionMapper;

    @Autowired
    private ArticleMapper articleMapper;

    // 最大保留版本数
    private static final int MAX_VERSIONS = 10;

    @Override
    @Transactional
    public boolean createVersion(Long articleId, String title, String content, Long userId) {
        try {
            // 获取下一个版本号
            Integer latestVersion = articleVersionMapper.selectLatestVersionNumber(articleId);
            int versionNumber = (latestVersion == null) ? 1 : latestVersion + 1;

            // 创建版本记录
            ArticleVersion version = new ArticleVersion();
            version.setArticleId(articleId);
            version.setVersionNumber(versionNumber);
            version.setTitle(title);
            version.setContent(content);
            version.setCreatedBy(userId);
            version.setCreatedAt(LocalDateTime.now());
            version.setContentHash(calculateContentHash(content));

            int result = articleVersionMapper.insert(version);
            
            if (result > 0) {
                log.info("创建文章版本成功: articleId={}, versionNumber={}", articleId, versionNumber);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("创建文章版本失败: articleId={}", articleId, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getArticleVersions(Long articleId) {
        try {
            List<ArticleVersion> versions = articleVersionMapper.selectByArticleId(articleId);
            List<Map<String, Object>> result = new ArrayList<>();

            for (ArticleVersion version : versions) {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("id", version.getId());
                versionInfo.put("versionNumber", version.getVersionNumber());
                versionInfo.put("title", version.getTitle());
                versionInfo.put("createdBy", version.getCreatedBy());
                versionInfo.put("createdAt", version.getCreatedAt());
                result.add(versionInfo);
            }

            return result;
        } catch (Exception e) {
            log.error("获取文章版本列表失败: articleId={}", articleId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getLatestVersion(Long articleId) {
        try {
            ArticleVersion latestVersion = articleVersionMapper.selectLatestVersion(articleId);
            
            if (latestVersion == null) {
                return null;
            }

            Map<String, Object> versionInfo = new HashMap<>();
            versionInfo.put("id", latestVersion.getId());
            versionInfo.put("versionNumber", latestVersion.getVersionNumber());
            versionInfo.put("title", latestVersion.getTitle());
            versionInfo.put("content", latestVersion.getContent());
            versionInfo.put("createdBy", latestVersion.getCreatedBy());
            versionInfo.put("createdAt", latestVersion.getCreatedAt());

            return versionInfo;
        } catch (Exception e) {
            log.error("获取最新版本失败: articleId={}", articleId, e);
            return null;
        }
    }

    @Override
    @Transactional
    public boolean autoSaveVersion(Long articleId, String title, String content, Long userId) {
        try {
            // 获取最新版本
            ArticleVersion latestVersion = articleVersionMapper.selectLatestVersion(articleId);
            
            // 如果没有版本或内容有显著变化，则保存新版本
            if (latestVersion == null || hasSignificantChange(latestVersion.getContent(), content)) {
                return createVersion(articleId, title, content, userId);
            }

            log.debug("内容变化不显著，跳过自动保存: articleId={}", articleId);
            return false;
        } catch (Exception e) {
            log.error("自动保存版本失败: articleId={}", articleId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getVersionStatistics(Long articleId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            int totalVersions = articleVersionMapper.countByArticleId(articleId);
            stats.put("totalVersions", totalVersions);

            ArticleVersion latestVersion = articleVersionMapper.selectLatestVersion(articleId);
            if (latestVersion != null) {
                stats.put("latestVersionNumber", latestVersion.getVersionNumber());
                stats.put("lastModifiedAt", latestVersion.getCreatedAt());
            }

            return stats;
        } catch (Exception e) {
            log.error("获取版本统计失败: articleId={}", articleId, e);
            return new HashMap<>();
        }
    }

    @Override
    @Transactional
    public int cleanupOldVersions(Long articleId) {
        try {
            int totalVersions = articleVersionMapper.countByArticleId(articleId);
            
            if (totalVersions <= MAX_VERSIONS) {
                log.debug("版本数量未超过限制，无需清理: articleId={}, count={}", articleId, totalVersions);
                return 0;
            }

            // 保留最新的MAX_VERSIONS个版本，删除其他版本
            int versionsToDelete = totalVersions - MAX_VERSIONS;
            int deletedCount = articleVersionMapper.deleteOldVersions(articleId, MAX_VERSIONS);

            log.info("清理旧版本完成: articleId={}, deleted={}", articleId, deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("清理旧版本失败: articleId={}", articleId, e);
            return 0;
        }
    }

    @Override
    @Transactional
    public int deleteAllVersions(Long articleId) {
        try {
            int deletedCount = articleVersionMapper.deleteByArticleId(articleId);
            log.info("删除文章所有版本: articleId={}, count={}", articleId, deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("删除文章所有版本失败: articleId={}", articleId, e);
            return 0;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 计算内容哈希值
     */
    private String calculateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("计算内容哈希失败", e);
            return "";
        }
    }

    /**
     * 判断内容是否有显著变化
     * 简单实现：如果内容长度变化超过10%或哈希值不同，则认为有显著变化
     */
    private boolean hasSignificantChange(String oldContent, String newContent) {
        if (oldContent == null || newContent == null) {
            return true;
        }

        // 长度变化超过10%
        int oldLength = oldContent.length();
        int newLength = newContent.length();
        double lengthChangeRatio = Math.abs((double)(newLength - oldLength) / oldLength);
        
        if (lengthChangeRatio > 0.1) {
            return true;
        }

        // 哈希值不同
        String oldHash = calculateContentHash(oldContent);
        String newHash = calculateContentHash(newContent);
        
        return !oldHash.equals(newHash);
    }
}
