package com.web.service;

import com.web.constant.ArticleStatus;
import com.web.exception.WeebException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章审核服务
 * 实现文章审核流程和敏感词检测
 * 
 * ✅ 已适配ArticleStatus枚举 - 2025-11-07
 */
@Slf4j
@Service
public class ArticleModerationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 审核优先级
    public static final int PRIORITY_LOW = 1;      // 低优先级
    public static final int PRIORITY_NORMAL = 2;   // 普通优先级
    public static final int PRIORITY_HIGH = 3;     // 高优先级
    public static final int PRIORITY_URGENT = 4;   // 紧急优先级

    // 审核超期时间（小时）
    public static final int OVERDUE_HOURS = 24;

    // 敏感词列表（实际应从数据库或配置文件加载）
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
        "政治", "暴力", "色情", "赌博", "毒品", "诈骗", "恐怖", "反动"
    ));

    /**
     * 提交文章审核
     * 实现审核状态机：DRAFT → PENDING_REVIEW → PUBLISHED/REJECTED
     */
    public Map<String, Object> submitForReview(Long articleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查文章是否存在
            String checkSql = "SELECT COUNT(*) FROM article WHERE id = ? AND author_id = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, articleId, userId);
            
            if (count == null || count == 0) {
                throw new WeebException("文章不存在或无权操作");
            }

            // 获取文章内容
            String contentSql = "SELECT title, content FROM article WHERE id = ?";
            Map<String, Object> article = jdbcTemplate.queryForMap(contentSql, articleId);
            String title = (String) article.get("title");
            String content = (String) article.get("content");

            // 检测敏感词及其位置
            String fullText = title + " " + content;
            List<String> foundSensitiveWords = detectSensitiveWords(fullText);
            List<Map<String, Object>> wordPositions = getSensitiveWordPositions(fullText);
            
            ArticleStatus newStatus;
            int priority = PRIORITY_NORMAL;
            
            if (foundSensitiveWords.isEmpty()) {
                // 无敏感词，直接发布
                newStatus = ArticleStatus.PUBLISHED;
                result.put("autoPublished", true);
            } else {
                // 有敏感词，提交审核
                newStatus = ArticleStatus.PENDING_REVIEW;
                
                // 根据敏感词数量设置优先级
                if (foundSensitiveWords.size() >= 5) {
                    priority = PRIORITY_URGENT;
                } else if (foundSensitiveWords.size() >= 3) {
                    priority = PRIORITY_HIGH;
                }
                
                result.put("autoPublished", false);
                result.put("sensitiveWords", foundSensitiveWords);
                result.put("wordPositions", wordPositions);
                result.put("priority", priority);
                
                log.warn("⚠️ 文章包含敏感词: articleId={}, words={}, priority={}", 
                        articleId, foundSensitiveWords, priority);
            }

            // 更新文章状态和优先级（使用枚举的code值）
            String updateSql = "UPDATE article SET status = ?, moderation_priority = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newStatus.getCode(), priority, new Timestamp(System.currentTimeMillis()), articleId);

            result.put("success", true);
            result.put("status", newStatus.getCode());
            result.put("statusName", newStatus.getDescription());
            result.put("articleId", articleId);
            
            log.info("✅ 文章已提交审核: articleId={}, status={}, priority={}", articleId, newStatus, priority);
            return result;

        } catch (Exception e) {
            log.error("❌ 提交文章审核失败: articleId={}", articleId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 检测敏感词
     */
    public List<String> detectSensitiveWords(String text) {
        List<String> found = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return found;
        }

        for (String word : SENSITIVE_WORDS) {
            if (text.contains(word)) {
                found.add(word);
            }
        }

        return found;
    }

    /**
     * 获取敏感词位置
     */
    public List<Map<String, Object>> getSensitiveWordPositions(String text) {
        List<Map<String, Object>> positions = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return positions;
        }

        for (String word : SENSITIVE_WORDS) {
            Pattern pattern = Pattern.compile(Pattern.quote(word));
            Matcher matcher = pattern.matcher(text);
            
            while (matcher.find()) {
                Map<String, Object> position = new HashMap<>();
                position.put("word", word);
                position.put("start", matcher.start());
                position.put("end", matcher.end());
                positions.add(position);
            }
        }

        return positions;
    }

    /**
     * 审核通过
     */
    public boolean approveArticle(Long articleId, Long reviewerId) {
        try {
            String sql = "UPDATE article SET status = ?, reviewer_id = ?, reviewed_at = ?, updated_at = ? WHERE id = ?";
            Timestamp now = new Timestamp(System.currentTimeMillis());
            int rows = jdbcTemplate.update(sql, ArticleStatus.PUBLISHED.getCode(), reviewerId, now, now, articleId);
            
            if (rows > 0) {
                log.info("✅ 文章审核通过: articleId={}, reviewerId={}", articleId, reviewerId);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("❌ 审核文章失败: articleId={}", articleId, e);
            return false;
        }
    }

    /**
     * 审核拒绝
     */
    public boolean rejectArticle(Long articleId, Long reviewerId, String reason) {
        try {
            String sql = "UPDATE article SET status = ?, reviewer_id = ?, reject_reason = ?, reviewed_at = ?, updated_at = ? WHERE id = ?";
            Timestamp now = new Timestamp(System.currentTimeMillis());
            int rows = jdbcTemplate.update(sql, ArticleStatus.REJECTED.getCode(), reviewerId, reason, now, now, articleId);
            
            if (rows > 0) {
                log.info("✅ 文章审核拒绝: articleId={}, reviewerId={}, reason={}", articleId, reviewerId, reason);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("❌ 拒绝文章失败: articleId={}", articleId, e);
            return false;
        }
    }

    /**
     * 获取待审核文章列表（按优先级排序）
     * 优先级高的文章排在前面，同优先级按提交时间排序
     */
    public List<Map<String, Object>> getPendingArticles(int page, int size) {
        try {
            int offset = (page - 1) * size;
            String sql = "SELECT id, title, author_id, status, created_at, " +
                        "COALESCE(moderation_priority, ?) as priority, " +
                        "TIMESTAMPDIFF(HOUR, created_at, NOW()) as waiting_hours " +
                        "FROM article " +
                        "WHERE status = ? " +
                        "ORDER BY priority DESC, created_at ASC " +
                        "LIMIT ? OFFSET ?";
            
            return jdbcTemplate.queryForList(sql, PRIORITY_NORMAL, ArticleStatus.PENDING_REVIEW.getCode(), size, offset);

        } catch (Exception e) {
            log.error("❌ 获取待审核文章失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取待审核文章数量
     */
    public int getPendingArticleCount() {
        try {
            String sql = "SELECT COUNT(*) FROM article WHERE status = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, ArticleStatus.PENDING_REVIEW.getCode());
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("❌ 获取待审核文章数量失败", e);
            return 0;
        }
    }

    /**
     * 检查审核超期（默认24小时）
     */
    public List<Map<String, Object>> getOverdueArticles() {
        return getOverdueArticles(OVERDUE_HOURS);
    }

    /**
     * 检查审核超期（自定义小时数）
     */
    public List<Map<String, Object>> getOverdueArticles(int hours) {
        try {
            String sql = "SELECT id, title, author_id, created_at, " +
                        "TIMESTAMPDIFF(HOUR, created_at, NOW()) as overdue_hours " +
                        "FROM article " +
                        "WHERE status = ? AND created_at < DATE_SUB(NOW(), INTERVAL ? HOUR) " +
                        "ORDER BY created_at ASC";
            
            List<Map<String, Object>> overdueArticles = jdbcTemplate.queryForList(sql, ArticleStatus.PENDING_REVIEW.getCode(), hours);
            
            if (!overdueArticles.isEmpty()) {
                log.warn("⚠️ 发现 {} 篇超期未审核文章（超过{}小时）", overdueArticles.size(), hours);
            }
            
            return overdueArticles;

        } catch (Exception e) {
            log.error("❌ 获取超期文章失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取审核统计信息
     */
    public Map<String, Object> getModerationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 待审核数量
            stats.put("pendingCount", getPendingArticleCount());
            
            // 超期数量
            stats.put("overdueCount", getOverdueArticles().size());
            
            // 今日审核数量
            String todaySql = "SELECT COUNT(*) FROM article WHERE DATE(reviewed_at) = CURDATE()";
            Integer todayCount = jdbcTemplate.queryForObject(todaySql, Integer.class);
            stats.put("todayReviewedCount", todayCount != null ? todayCount : 0);
            
            // 按优先级统计
            String prioritySql = "SELECT COALESCE(moderation_priority, ?) as priority, COUNT(*) as count " +
                               "FROM article WHERE status = ? GROUP BY priority";
            List<Map<String, Object>> priorityStats = jdbcTemplate.queryForList(
                prioritySql, PRIORITY_NORMAL, ArticleStatus.PENDING_REVIEW.getCode());
            stats.put("priorityDistribution", priorityStats);
            
            return stats;

        } catch (Exception e) {
            log.error("❌ 获取审核统计失败", e);
            return stats;
        }
    }

    /**
     * 设置文章审核优先级
     */
    public boolean setArticlePriority(Long articleId, int priority) {
        try {
            if (priority < PRIORITY_LOW || priority > PRIORITY_URGENT) {
                log.warn("⚠️ 无效的优先级: {}", priority);
                return false;
            }

            String sql = "UPDATE article SET moderation_priority = ?, updated_at = ? WHERE id = ? AND status = ?";
            int rows = jdbcTemplate.update(sql, priority, new Timestamp(System.currentTimeMillis()), 
                                         articleId, ArticleStatus.PENDING_REVIEW.getCode());
            
            if (rows > 0) {
                log.info("✅ 文章优先级已更新: articleId={}, priority={}", articleId, priority);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("❌ 设置文章优先级失败: articleId={}", articleId, e);
            return false;
        }
    }
}
