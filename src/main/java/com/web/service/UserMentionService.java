package com.web.service;

import com.web.model.UserMention;
import java.util.List;
import java.util.Map;

/**
 * 用户提及服务接口
 */
public interface UserMentionService {

    /**
     * 创建用户提及
     * @param messageId 消息ID
     * @param mentionerId 提及者ID
     * @param mentionedUserId 被提及用户ID
     * @param mentionText 提及文本
     * @param startPosition 起始位置
     * @param endPosition 结束位置
     * @param mentionType 提及类型
     * @return 创建的提及
     */
    UserMention createMention(Long messageId, Long mentionerId, Long mentionedUserId,
                             String mentionText, Integer startPosition, Integer endPosition,
                             String mentionType);

    /**
     * 批量创建用户提及
     * @param mentions 提及列表
     * @return 创建成功的提及数量
     */
    int batchCreateMentions(List<UserMention> mentions);

    /**
     * 获取消息中的所有提及
     * @param messageId 消息ID
     * @return 提及列表
     */
    List<UserMention> getMentionsByMessage(Long messageId);

    /**
     * 获取用户发起的提及
     * @param mentionerId 提及者ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 提及列表和分页信息
     */
    Map<String, Object> getMentionsByMentioner(Long mentionerId, int page, int pageSize);

    /**
     * 获取用户收到的提及
     * @param mentionedUserId 被提及用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 提及列表和分页信息
     */
    Map<String, Object> getMentionsByMentionedUser(Long mentionedUserId, int page, int pageSize);

    /**
     * 获取用户未读的提及
     * @param mentionedUserId 被提及用户ID
     * @param limit 限制数量
     * @return 未读提及列表
     */
    List<UserMention> getUnreadMentions(Long mentionedUserId, int limit);

    /**
     * 标记提及为已读
     * @param mentionId 提及ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    boolean markMentionAsRead(Long mentionId, Long userId);

    /**
     * 批量标记提及为已读
     * @param mentionIds 提及ID列表
     * @param userId 操作用户ID
     * @return 标记成功的数量
     */
    int batchMarkMentionsAsRead(List<Long> mentionIds, Long userId);

    /**
     * 标记用户所有提及为已读
     * @param mentionedUserId 被提及用户ID
     * @return 标记成功的数量
     */
    int markAllMentionsAsRead(Long mentionedUserId);

    /**
     * 解析消息中的提及
     * @param content 消息内容
     * @param messageId 消息ID
     * @param senderId 发送者ID
     * @return 解析出的提及列表
     */
    List<UserMention> parseMentionsFromContent(String content, Long messageId, Long senderId);

    /**
     * 获取提及统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getMentionStatistics(Long userId);

    /**
     * 删除提及
     * @param mentionId 提及ID
     * @param userId 操作用户ID（只有提及者可以删除）
     * @return 是否成功
     */
    boolean deleteMention(Long mentionId, Long userId);

    /**
     * 搜索提及
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    Map<String, Object> searchMentions(String keyword, int page, int pageSize);

    /**
     * 获取提及上下文
     * @param mentionId 提及ID
     * @return 上下文信息
     */
    Map<String, Object> getMentionContext(Long mentionId);

    /**
     * 验证提及有效性
     * @param mention 提及对象
     * @return 是否有效
     */
    boolean validateMention(UserMention mention);

    /**
     * 获取热门提及用户
     * @param limit 限制数量
     * @return 用户提及统计
     */
    List<Map<String, Object>> getPopularMentionedUsers(int limit);

    /**
     * 获取用户提及趋势
     * @param userId 用户ID
     * @param days 天数
     * @return 趋势数据
     */
    List<Map<String, Object>> getMentionTrends(Long userId, int days);
}