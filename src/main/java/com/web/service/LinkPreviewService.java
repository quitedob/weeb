package com.web.service;

import com.web.model.LinkPreview;
import java.util.List;
import java.util.Map;

/**
 * 链接预览服务接口
 */
public interface LinkPreviewService {

    /**
     * 创建链接预览
     * @param originalUrl 原始链接
     * @param messageId 消息ID
     * @param createdBy 创建者ID
     * @return 创建的预览
     */
    LinkPreview createPreview(String originalUrl, Long messageId, Long createdBy);

    /**
     * 异步生成链接预览
     * @param previewId 预览ID
     */
    void generatePreviewAsync(Long previewId);

    /**
     * 获取链接预览
     * @param previewId 预览ID
     * @return 预览信息
     */
    LinkPreview getPreviewById(Long previewId);

    /**
     * 获取消息的链接预览列表
     * @param messageId 消息ID
     * @return 预览列表
     */
    List<LinkPreview> getPreviewsByMessage(Long messageId);

    /**
     * 获取用户创建的预览列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 预览列表和分页信息
     */
    Map<String, Object> getPreviewsByUser(Long userId, int page, int pageSize);

    /**
     * 刷新链接预览
     * @param previewId 预览ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    boolean refreshPreview(Long previewId, Long userId);

    /**
     * 禁用链接预览
     * @param previewId 预览ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    boolean disablePreview(Long previewId, Long userId);

    /**
     * 启用链接预览
     * @param previewId 预览ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    boolean enablePreview(Long previewId, Long userId);

    /**
     * 删除链接预览
     * @param previewId 预览ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    boolean deletePreview(Long previewId, Long userId);

    /**
     * 从消息内容中提取链接并创建预览
     * @param content 消息内容
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 创建的预览数量
     */
    int extractAndCreatePreviews(String content, Long messageId, Long userId);

    /**
     * 批量生成预览
     * @param previewIds 预览ID列表
     * @return 成功生成的数量
     */
    int batchGeneratePreviews(List<Long> previewIds);

    /**
     * 获取预览统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getPreviewStatistics(Long userId);

    /**
     * 搜索链接预览
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    Map<String, Object> searchPreviews(String keyword, int page, int pageSize);

    /**
     * 获取热门域名预览
     * @param limit 限制数量
     * @return 域名统计
     */
    List<Map<String, Object>> getPopularDomains(int limit);

    /**
     * 获取预览趋势
     * @param days 天数
     * @return 趋势数据
     */
    List<Map<String, Object>> getPreviewTrends(int days);

    /**
     * 清理过期预览
     * @return 清理的数量
     */
    int cleanupExpiredPreviews();

    /**
     * 验证URL有效性
     * @param url URL地址
     * @return 是否有效
     */
    boolean validateUrl(String url);

    /**
     * 获取域名信息
     * @param url URL地址
     * @return 域名信息
     */
    Map<String, Object> getDomainInfo(String url);

    /**
     * 设置用户评分
     * @param previewId 预览ID
     * @param rating 评分(1-5)
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean setUserRating(Long previewId, int rating, Long userId);

    /**
     * 延长预览过期时间
     * @param previewId 预览ID
     * @param hours 延长小时数
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean extendExpiration(Long previewId, int hours, Long userId);

    /**
     * 获取预览缓存信息
     * @param url URL地址
     * @return 缓存信息
     */
    Map<String, Object> getPreviewCache(String url);

    /**
     * 清除预览缓存
     * @param url URL地址
     * @return 是否成功
     */
    boolean clearPreviewCache(String url);
}