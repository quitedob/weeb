package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.LinkPreviewMapper;
import com.web.mapper.MessageMapper;
import com.web.mapper.UserMapper;
import com.web.model.LinkPreview;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.LinkPreviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 链接预览服务实现类
 */
@Slf4j
@Service
public class LinkPreviewServiceImpl implements LinkPreviewService {

    private final LinkPreviewMapper linkPreviewMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final HttpClient httpClient;

    // URL 正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://(?:www\\.|(?!www))[^\\s/$.?#].[^\\s]*)",
        Pattern.CASE_INSENSITIVE
    );

    @Autowired
    public LinkPreviewServiceImpl(LinkPreviewMapper linkPreviewMapper, MessageMapper messageMapper, UserMapper userMapper) {
        this.linkPreviewMapper = linkPreviewMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    @Override
    @Transactional
    public LinkPreview createPreview(String originalUrl, Long messageId, Long createdBy) {
        log.info("创建链接预览: url={}, messageId={}, createdBy={}", originalUrl, messageId, createdBy);

        try {
            // 输入验证
            if (originalUrl == null || originalUrl.trim().isEmpty()) {
                throw new WeebException("URL不能为空");
            }
            if (messageId == null || messageId <= 0) {
                throw new WeebException("无效的消息ID");
            }
            if (createdBy == null || createdBy <= 0) {
                throw new WeebException("无效的用户ID");
            }

            // 验证URL有效性
            if (!validateUrl(originalUrl)) {
                throw new WeebException("无效的URL: " + originalUrl);
            }

            // 验证消息是否存在
            Message message = messageMapper.selectById(messageId);
            if (message == null) {
                throw new WeebException("消息不存在: " + messageId);
            }

            // 验证用户是否存在
            User creator = userMapper.selectById(createdBy);
            if (creator == null) {
                throw new WeebException("用户不存在: " + createdBy);
            }

            // 检查是否已存在相同的预览
            LinkPreview existingPreview = linkPreviewMapper.findByUrlAndMessage(originalUrl, messageId);
            if (existingPreview != null) {
                log.info("预览已存在，返回现有预览: previewId={}", existingPreview.getId());
                return existingPreview;
            }

            // 创建预览
            LinkPreview preview = new LinkPreview(originalUrl);
            preview.setMessageId(messageId);
            preview.setCreatedBy(createdBy);

            linkPreviewMapper.insert(preview);

            // 异步生成预览内容
            generatePreviewAsync(preview.getId());

            log.info("链接预览创建成功: previewId={}", preview.getId());
            return preview;

        } catch (Exception e) {
            log.error("创建链接预览失败: {}", e.getMessage(), e);
            throw new com.web.exception.WeebException("创建链接预览失败: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void generatePreviewAsync(Long previewId) {
        log.info("异步生成预览: previewId={}", previewId);

        try {
            LinkPreview preview = linkPreviewMapper.findById(previewId);
            if (preview == null) {
                log.warn("预览不存在: previewId={}", previewId);
                return;
            }

            // 发送HTTP请求获取页面内容
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(preview.getOriginalUrl()))
                .header("User-Agent", "Mozilla/5.0 (compatible; LinkPreviewBot/1.0)")
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();

            // 记录生成时间
            preview.setGenerationTime(endTime - startTime);

            if (response.statusCode() == 200) {
                // 解析页面内容
                parseHtmlContent(preview, response.body());
                preview.markAsSuccess();
            } else {
                preview.markAsFailed("HTTP " + response.statusCode() + ": " + response.body());
            }

            linkPreviewMapper.update(preview);
            log.info("预览生成完成: previewId={}, status={}", previewId, preview.getStatus());

        } catch (Exception e) {
            log.error("生成预览失败: previewId={}, error={}", previewId, e.getMessage(), e);

            // 更新失败状态
            try {
                LinkPreview preview = linkPreviewMapper.findById(previewId);
                if (preview != null) {
                    preview.markAsFailed(e.getMessage());
                    linkPreviewMapper.update(preview);
                }
            } catch (Exception updateError) {
                log.error("更新预览失败状态失败: {}", updateError.getMessage(), updateError);
            }
        }
    }

    @Override
    public LinkPreview getPreviewById(Long previewId) {
        log.debug("获取预览详情: previewId={}", previewId);

        LinkPreview preview = linkPreviewMapper.findById(previewId);
        if (preview == null) {
            throw new WeebException("预览不存在: " + previewId);
        }

        return preview;
    }

    @Override
    public List<LinkPreview> getPreviewsByMessage(Long messageId) {
        log.debug("获取消息预览列表: messageId={}", messageId);
        return linkPreviewMapper.findByMessageId(messageId);
    }

    @Override
    public Map<String, Object> getPreviewsByUser(Long userId, int page, int pageSize) {
        log.debug("获取用户预览列表: userId={}, page={}, pageSize={}", userId, page, pageSize);

        int offset = (page - 1) * pageSize;

        List<LinkPreview> previews = linkPreviewMapper.findByUserId(userId, offset, pageSize);
        int total = linkPreviewMapper.countByUserId(userId);

        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("previews", previews);
        result.put("pagination", Map.of(
            "page", page,
            "pageSize", pageSize,
            "total", total,
            "totalPages", totalPages,
            "hasNext", page < totalPages,
            "hasPrev", page > 1
        ));

        return result;
    }

    @Override
    @Transactional
    public boolean refreshPreview(Long previewId, Long userId) {
        log.info("刷新预览: previewId={}, userId={}", previewId, userId);

        try {
            LinkPreview preview = getPreviewById(previewId);

            // 只有创建者可以刷新预览
            if (!preview.getCreatedBy().equals(userId)) {
                throw new WeebException("只有创建者可以刷新预览");
            }

            // 重置状态并重新生成
            preview.setStatus(LinkPreview.Status.PENDING);
            preview.setErrorMessage(null);
            linkPreviewMapper.update(preview);

            // 异步重新生成
            generatePreviewAsync(previewId);

            log.info("预览刷新成功: previewId={}", previewId);
            return true;

        } catch (Exception e) {
            log.error("刷新预览失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean disablePreview(Long previewId, Long userId) {
        log.info("禁用预览: previewId={}, userId={}", previewId, userId);

        try {
            LinkPreview preview = getPreviewById(previewId);

            // 只有创建者可以禁用预览
            if (!preview.getCreatedBy().equals(userId)) {
                throw new WeebException("只有创建者可以禁用预览");
            }

            preview.disable();
            linkPreviewMapper.update(preview);

            log.info("预览禁用成功: previewId={}", previewId);
            return true;

        } catch (Exception e) {
            log.error("禁用预览失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean enablePreview(Long previewId, Long userId) {
        log.info("启用预览: previewId={}, userId={}", previewId, userId);

        try {
            LinkPreview preview = getPreviewById(previewId);

            // 只有创建者可以启用预览
            if (!preview.getCreatedBy().equals(userId)) {
                throw new WeebException("只有创建者可以启用预览");
            }

            preview.enable();
            linkPreviewMapper.update(preview);

            log.info("预览启用成功: previewId={}", previewId);
            return true;

        } catch (Exception e) {
            log.error("启用预览失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deletePreview(Long previewId, Long userId) {
        log.info("删除预览: previewId={}, userId={}", previewId, userId);

        try {
            LinkPreview preview = getPreviewById(previewId);

            // 只有创建者可以删除预览
            if (!preview.getCreatedBy().equals(userId)) {
                throw new WeebException("只有创建者可以删除预览");
            }

            linkPreviewMapper.deleteById(previewId);

            log.info("预览删除成功: previewId={}", previewId);
            return true;

        } catch (Exception e) {
            log.error("删除预览失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public int extractAndCreatePreviews(String content, Long messageId, Long userId) {
        log.debug("从消息内容提取链接: messageId={}, userId", messageId, userId);

        if (content == null || content.trim().isEmpty()) {
            return 0;
        }

        try {
            Set<String> uniqueUrls = new HashSet<>();
            Matcher matcher = URL_PATTERN.matcher(content);

            while (matcher.find()) {
                String url = matcher.group(1);
                uniqueUrls.add(url);
            }

            int createdCount = 0;
            for (String url : uniqueUrls) {
                try {
                    createPreview(url, messageId, userId);
                    createdCount++;
                } catch (Exception e) {
                    log.warn("创建链接预览失败: url={}, error={}", url, e.getMessage());
                }
            }

            log.info("链接提取完成: found={}, created={}", uniqueUrls.size(), createdCount);
            return createdCount;

        } catch (Exception e) {
            log.error("提取链接失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public int batchGeneratePreviews(List<Long> previewIds) {
        log.info("批量生成预览: count={}", previewIds.size());

        int successCount = 0;
        for (Long previewId : previewIds) {
            try {
                LinkPreview preview = linkPreviewMapper.findById(previewId);
                if (preview != null && LinkPreview.Status.PENDING.equals(preview.getStatus())) {
                    generatePreviewAsync(previewId);
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("批量生成预览失败: previewId={}, error={}", previewId, e.getMessage());
            }
        }

        log.info("批量生成预览完成: successCount={}", successCount);
        return successCount;
    }

    @Override
    public Map<String, Object> getPreviewStatistics(Long userId) {
        log.debug("获取用户预览统计: userId={}", userId);

        Map<String, Object> statistics = new HashMap<>();

        int totalPreviews = linkPreviewMapper.countByUserId(userId);
        int successfulPreviews = linkPreviewMapper.countByUserIdAndStatus(userId, LinkPreview.Status.SUCCESS);
        int failedPreviews = linkPreviewMapper.countByUserIdAndStatus(userId, LinkPreview.Status.FAILED);
        int pendingPreviews = linkPreviewMapper.countByUserIdAndStatus(userId, LinkPreview.Status.PENDING);

        statistics.put("total", totalPreviews);
        statistics.put("successful", successfulPreviews);
        statistics.put("failed", failedPreviews);
        statistics.put("pending", pendingPreviews);
        statistics.put("successRate", totalPreviews > 0 ? (double) successfulPreviews / totalPreviews : 0.0);

        return statistics;
    }

    @Override
    public Map<String, Object> searchPreviews(String keyword, int page, int pageSize) {
        log.debug("搜索预览: keyword={}, page={}, pageSize={}", keyword, page, pageSize);

        // 输入验证防止SQL注入
        if (keyword != null) {
            // 限制关键词长度并移除特殊字符
            keyword = keyword.trim();
            if (keyword.length() > 100) {
                keyword = keyword.substring(0, 100);
            }
            // 移除潜在的危险字符
            keyword = keyword.replaceAll("[';\"\\-\\-]", "");
        }
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;

        int offset = (page - 1) * pageSize;

        List<LinkPreview> previews = linkPreviewMapper.searchPreviews(keyword, offset, pageSize);
        int total = linkPreviewMapper.searchPreviewsCount(keyword);

        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("previews", previews);
        result.put("keyword", keyword);
        result.put("pagination", Map.of(
            "page", page,
            "pageSize", pageSize,
            "total", total,
            "totalPages", totalPages,
            "hasNext", page < totalPages,
            "hasPrev", page > 1
        ));

        return result;
    }

    @Override
    public List<Map<String, Object>> getPopularDomains(int limit) {
        log.debug("获取热门域名: limit={}", limit);
        return linkPreviewMapper.findPopularDomains(limit);
    }

    @Override
    public List<Map<String, Object>> getPreviewTrends(int days) {
        log.debug("获取预览趋势: days={}", days);
        return linkPreviewMapper.findPreviewTrends(days);
    }

    @Override
    @Transactional
    public int cleanupExpiredPreviews() {
        log.info("清理过期预览");

        try {
            int deletedCount = linkPreviewMapper.deleteExpiredPreviews();
            log.info("过期预览清理完成: deletedCount={}", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("清理过期预览失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            return "http".equals(scheme) || "https".equals(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getDomainInfo(String url) {
        try {
            URI uri = URI.create(url);
            String domain = uri.getHost();

            if (domain == null) {
                return Map.of("domain", "", "isValid", false);
            }

            // 移除 www. 前缀
            if (domain.startsWith("www.")) {
                domain = domain.substring(4);
            }

            return Map.of(
                "domain", domain,
                "isValid", true,
                "scheme", uri.getScheme(),
                "port", uri.getPort() != -1 ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80)
            );

        } catch (Exception e) {
            return Map.of("domain", "", "isValid", false);
        }
    }

    @Override
    @Transactional
    public boolean setUserRating(Long previewId, int rating, Long userId) {
        log.info("设置预览评分: previewId={}, rating={}, userId={}", previewId, rating, userId);

        try {
            LinkPreview preview = getPreviewById(previewId);

            // 只有创建者可以评分
            if (!preview.getCreatedBy().equals(userId)) {
                throw new WeebException("只有创建者可以评分");
            }

            preview.setUserRating(rating);
            linkPreviewMapper.update(preview);

            log.info("预览评分设置成功: previewId={}, rating={}", previewId, rating);
            return true;

        } catch (Exception e) {
            log.error("设置预览评分失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean extendExpiration(Long previewId, int hours, Long userId) {
        log.info("延长预览过期时间: previewId={}, hours={}, userId={}", previewId, hours, userId);

        try {
            LinkPreview preview = getPreviewById(previewId);

            // 只有创建者可以延长过期时间
            if (!preview.getCreatedBy().equals(userId)) {
                throw new WeebException("只有创建者可以延长过期时间");
            }

            preview.extendExpiration(hours);
            linkPreviewMapper.update(preview);

            log.info("预览过期时间延长成功: previewId={}, hours={}", previewId, hours);
            return true;

        } catch (Exception e) {
            log.error("延长预览过期时间失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getPreviewCache(String url) {
        // 这里可以实现 Redis 缓存逻辑
        log.debug("获取预览缓存: url={}", url);
        return Map.of("cached", false, "data", null);
    }

    @Override
    public boolean clearPreviewCache(String url) {
        // 这里可以实现清除 Redis 缓存的逻辑
        log.info("清除预览缓存: url={}", url);
        return true;
    }

    /**
     * 解析HTML内容
     */
    private void parseHtmlContent(LinkPreview preview, String html) {
        try {
            // 简单的HTML解析，提取关键信息
            // 在实际项目中，建议使用专业的HTML解析库如 jsoup

            // 提取标题
            String title = extractMetaTag(html, "title");
            if (title == null || title.trim().isEmpty()) {
                title = extractTitleTag(html);
            }
            preview.setTitle(title);

            // 提取描述
            String description = extractMetaTag(html, "description");
            if (description == null || description.trim().isEmpty()) {
                description = extractMetaProperty(html, "og:description");
            }
            preview.setDescription(description);

            // 提取网站名称
            String siteName = extractMetaProperty(html, "og:site_name");
            if (siteName == null || siteName.trim().isEmpty()) {
                siteName = extractDomainFromUrl(preview.getOriginalUrl());
            }
            preview.setSiteName(siteName);

            // 提取图片
            String imageUrl = extractMetaProperty(html, "og:image");
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = extractMetaProperty(html, "twitter:image");
            }
            preview.setImageUrl(imageUrl);

            // 提取内容类型
            String contentType = extractMetaProperty(html, "og:type");
            if (contentType == null || contentType.trim().isEmpty()) {
                contentType = LinkPreview.ContentType.WEBSITE;
            }
            preview.setContentType(contentType);

            // 存储Open Graph标签
            preview.setOgTags(extractAllOgTags(html));
            preview.setTwitterTags(extractAllTwitterTags(html));

        } catch (Exception e) {
            log.error("解析HTML内容失败: {}", e.getMessage(), e);
            preview.markAsFailed("HTML解析失败: " + e.getMessage());
        }
    }

    /**
     * 提取meta标签内容
     */
    private String extractMetaTag(String html, String name) {
        Pattern pattern = Pattern.compile(
            "<meta[^>]*name=[\"']" + Pattern.quote(name) + "[\"'][^>]*content=[\"']([^\"']*)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * 提取meta property标签内容
     */
    private String extractMetaProperty(String html, String property) {
        Pattern pattern = Pattern.compile(
            "<meta[^>]*property=[\"']" + Pattern.quote(property) + "[\"'][^>]*content=[\"']([^\"']*)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * 提取title标签内容
     */
    private String extractTitleTag(String html) {
        Pattern pattern = Pattern.compile("<title[^>]*>([^<]*)</title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * 提取所有Open Graph标签
     */
    private String extractAllOgTags(String html) {
        Pattern pattern = Pattern.compile("<meta[^>]*property=[\"']og:[^\"']*[\"'][^>]*content=[\"']([^\"']*)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);

        Map<String, String> ogTags = new HashMap<>();
        while (matcher.find()) {
            String tag = matcher.group();
            String property = extractPropertyFromTag(tag);
            String content = extractContentFromTag(tag);
            if (property != null && content != null) {
                ogTags.put(property, content);
            }
        }

        return ogTags.isEmpty() ? null : ogTags.toString();
    }

    /**
     * 提取所有Twitter Card标签
     */
    private String extractAllTwitterTags(String html) {
        Pattern pattern = Pattern.compile("<meta[^>]*name=[\"']twitter:[^\"']*[\"'][^>]*content=[\"']([^\"']*)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);

        Map<String, String> twitterTags = new HashMap<>();
        while (matcher.find()) {
            String tag = matcher.group();
            String name = extractNameFromTag(tag);
            String content = extractContentFromTag(tag);
            if (name != null && content != null) {
                twitterTags.put(name, content);
            }
        }

        return twitterTags.isEmpty() ? null : twitterTags.toString();
    }

    /**
     * 从URL提取域名
     */
    private String extractDomainFromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String domain = uri.getHost();
            if (domain != null && domain.startsWith("www.")) {
                domain = domain.substring(4);
            }
            return domain;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从meta标签中提取property属性
     */
    private String extractPropertyFromTag(String tag) {
        Pattern pattern = Pattern.compile("property=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(tag);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 从meta标签中提取name属性
     */
    private String extractNameFromTag(String tag) {
        Pattern pattern = Pattern.compile("name=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(tag);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 从meta标签中提取content属性
     */
    private String extractContentFromTag(String tag) {
        Pattern pattern = Pattern.compile("content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(tag);
        return matcher.find() ? matcher.group(1) : null;
    }
}