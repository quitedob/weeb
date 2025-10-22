package com.web.service.impl;

import com.web.mapper.UserMentionMapper;
import com.web.mapper.UserMapper;
import com.web.mapper.MessageMapper;
import com.web.model.UserMention;
import com.web.model.User;
import com.web.model.Message;
import com.web.service.UserMentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户提及服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMentionServiceImpl implements UserMentionService {

    private final UserMentionMapper userMentionMapper;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;

    // @username 正则表达式
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w{1,30})");

    @Override
    @Transactional
    public UserMention createMention(Long messageId, Long mentionerId, Long mentionedUserId,
                                    String mentionText, Integer startPosition, Integer endPosition,
                                    String mentionType) {
        log.info("创建用户提及: messageId={}, mentionerId={}, mentionedUserId={}",
                messageId, mentionerId, mentionedUserId);

        try {
            // 输入验证
            if (messageId == null || messageId <= 0) {
                throw new com.web.exception.WeebException("无效的消息ID");
            }
            if (mentionerId == null || mentionerId <= 0) {
                throw new com.web.exception.WeebException("无效的提及者ID");
            }
            if (mentionedUserId == null || mentionedUserId <= 0) {
                throw new com.web.exception.WeebException("无效的被提及用户ID");
            }
            if (mentionText != null && mentionText.length() > 500) {
                throw new com.web.exception.WeebException("提及文本过长");
            }
            if (startPosition != null && startPosition < 0) {
                throw new com.web.exception.WeebException("起始位置不能为负数");
            }
            if (endPosition != null && endPosition < 0) {
                throw new com.web.exception.WeebException("结束位置不能为负数");
            }

            // 验证消息是否存在
            Message message = messageMapper.findById(messageId);
            if (message == null) {
                throw new com.web.exception.WeebException("消息不存在: " + messageId);
            }

            // 验证提及者是否存在
            User mentioner = userMapper.findById(mentionerId);
            if (mentioner == null) {
                throw new com.web.exception.WeebException("提及者不存在: " + mentionerId);
            }

            // 验证被提及用户是否存在
            User mentionedUser = userMapper.findById(mentionedUserId);
            if (mentionedUser == null) {
                throw new com.web.exception.WeebException("被提及用户不存在: " + mentionedUserId);
            }

            // 创建提及
            UserMention mention = new UserMention();
            mention.setMessageId(messageId);
            mention.setMentionerId(mentionerId);
            mention.setMentionerUsername(mentioner.getUsername());
            mention.setMentionedUserId(mentionedUserId);
            mention.setMentionedUsername(mentionedUser.getUsername());
            mention.setMentionText(mentionText);
            mention.setStartPosition(startPosition);
            mention.setEndPosition(endPosition);
            mention.setMentionType(mentionType);

            userMentionMapper.insert(mention);

            log.info("用户提及创建成功: mentionId={}", mention.getId());
            return mention;

        } catch (Exception e) {
            log.error("创建用户提及失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建用户提及失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int batchCreateMentions(List<UserMention> mentions) {
        log.info("批量创建用户提及: count={}", mentions.size());

        try {
            int successCount = 0;
            for (UserMention mention : mentions) {
                if (validateMention(mention)) {
                    userMentionMapper.insert(mention);
                    successCount++;
                }
            }

            log.info("批量创建用户提及完成: successCount={}", successCount);
            return successCount;

        } catch (Exception e) {
            log.error("批量创建用户提及失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量创建用户提及失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UserMention> getMentionsByMessage(Long messageId) {
        log.debug("获取消息提及: messageId={}", messageId);
        return userMentionMapper.findByMessageId(messageId);
    }

    @Override
    public Map<String, Object> getMentionsByMentioner(Long mentionerId, int page, int pageSize) {
        log.debug("获取用户发起的提及: mentionerId={}, page={}, pageSize={}", mentionerId, page, pageSize);

        int offset = (page - 1) * pageSize;

        List<UserMention> mentions = userMentionMapper.findByMentionerId(mentionerId, offset, pageSize);
        int total = userMentionMapper.countByMentionerId(mentionerId);

        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("mentions", mentions);
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
    public Map<String, Object> getMentionsByMentionedUser(Long mentionedUserId, int page, int pageSize) {
        log.debug("获取用户收到的提及: mentionedUserId={}, page={}, pageSize={}", mentionedUserId, page, pageSize);

        int offset = (page - 1) * pageSize;

        List<UserMention> mentions = userMentionMapper.findByMentionedUserId(mentionedUserId, offset, pageSize);
        int total = userMentionMapper.countByMentionedUserId(mentionedUserId);

        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("mentions", mentions);
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
    public List<UserMention> getUnreadMentions(Long mentionedUserId, int limit) {
        log.debug("获取用户未读提及: mentionedUserId={}, limit={}", mentionedUserId, limit);
        return userMentionMapper.findUnreadByMentionedUserId(mentionedUserId, limit);
    }

    @Override
    @Transactional
    public boolean markMentionAsRead(Long mentionId, Long userId) {
        log.info("标记提及为已读: mentionId={}, userId={}", mentionId, userId);

        try {
            UserMention mention = userMentionMapper.findById(mentionId);
            if (mention == null) {
                throw new com.web.exception.WeebException("提及不存在: " + mentionId);
            }

            // 只有被提及用户可以标记为已读
            if (!mention.getMentionedUserId().equals(userId)) {
                throw new com.web.exception.WeebException("只有被提及用户可以标记为已读");
            }

            mention.markAsRead();
            userMentionMapper.update(mention);

            log.info("提及标记为已读成功: mentionId={}", mentionId);
            return true;

        } catch (Exception e) {
            log.error("标记提及为已读失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public int batchMarkMentionsAsRead(List<Long> mentionIds, Long userId) {
        log.info("批量标记提及为已读: count={}, userId={}", mentionIds.size(), userId);

        try {
            int successCount = 0;
            for (Long mentionId : mentionIds) {
                if (markMentionAsRead(mentionId, userId)) {
                    successCount++;
                }
            }

            log.info("批量标记提及为已读完成: successCount={}", successCount);
            return successCount;

        } catch (Exception e) {
            log.error("批量标记提及为已读失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public int markAllMentionsAsRead(Long mentionedUserId) {
        log.info("标记用户所有提及为已读: mentionedUserId={}", mentionedUserId);

        try {
            int updatedCount = userMentionMapper.markAllAsReadByMentionedUserId(mentionedUserId);
            log.info("标记所有提及为已读完成: updatedCount={}", updatedCount);
            return updatedCount;

        } catch (Exception e) {
            log.error("标记所有提及为已读失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public List<UserMention> parseMentionsFromContent(String content, Long messageId, Long senderId) {
        log.debug("解析消息中的提及: messageId={}, senderId", messageId, senderId);

        List<UserMention> mentions = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            return mentions;
        }

        try {
            Matcher matcher = MENTION_PATTERN.matcher(content);
            while (matcher.find()) {
                String username = matcher.group(1);
                int startPosition = matcher.start();
                int endPosition = matcher.end();

                // 查找用户
                User mentionedUser = userMapper.findByUsername(username);
                if (mentionedUser != null && !mentionedUser.getId().equals(senderId)) {
                    // 不能提及自己
                    UserMention mention = new UserMention();
                    mention.setMessageId(messageId);
                    mention.setMentionerId(senderId);
                    mention.setMentionedUserId(mentionedUser.getId());
                    mention.setMentionText(matcher.group());
                    mention.setStartPosition(startPosition);
                    mention.setEndPosition(endPosition);
                    mention.setMentionType(UserMention.MentionType.MENTION);

                    mentions.add(mention);
                }
            }

            log.debug("解析出 {} 个提及", mentions.size());
            return mentions;

        } catch (Exception e) {
            log.error("解析提及失败: {}", e.getMessage(), e);
            return mentions;
        }
    }

    @Override
    public Map<String, Object> getMentionStatistics(Long userId) {
        log.debug("获取用户提及统计: userId={}", userId);

        Map<String, Object> statistics = new HashMap<>();

        // 发出的提及统计
        int sentTotal = userMentionMapper.countByMentionerId(userId);
        int sentToday = userMentionMapper.countByMentionerIdAndDate(userId, LocalDateTime.now().toLocalDate());
        int sentThisWeek = userMentionMapper.countByMentionerIdAndDays(userId, 7);

        // 收到的提及统计
        int receivedTotal = userMentionMapper.countByMentionedUserId(userId);
        int receivedUnread = userMentionMapper.countUnreadByMentionedUserId(userId);
        int receivedToday = userMentionMapper.countByMentionedUserIdAndDate(userId, LocalDateTime.now().toLocalDate());
        int receivedThisWeek = userMentionMapper.countByMentionedUserIdAndDays(userId, 7);

        statistics.put("sent", Map.of(
            "total", sentTotal,
            "today", sentToday,
            "thisWeek", sentThisWeek
        ));

        statistics.put("received", Map.of(
            "total", receivedTotal,
            "unread", receivedUnread,
            "today", receivedToday,
            "thisWeek", receivedThisWeek
        ));

        return statistics;
    }

    @Override
    @Transactional
    public boolean deleteMention(Long mentionId, Long userId) {
        log.info("删除提及: mentionId={}, userId={}", mentionId, userId);

        try {
            UserMention mention = userMentionMapper.findById(mentionId);
            if (mention == null) {
                throw new com.web.exception.WeebException("提及不存在: " + mentionId);
            }

            // 只有提及者可以删除提及
            if (!mention.getMentionerId().equals(userId)) {
                throw new com.web.exception.WeebException("只有提及者可以删除提及");
            }

            userMentionMapper.deleteById(mentionId);

            log.info("提及删除成功: mentionId={}", mentionId);
            return true;

        } catch (Exception e) {
            log.error("删除提及失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> searchMentions(String keyword, int page, int pageSize) {
        log.debug("搜索提及: keyword={}, page={}, pageSize={}", keyword, page, pageSize);

        int offset = (page - 1) * pageSize;

        List<UserMention> mentions = userMentionMapper.searchMentions(keyword, offset, pageSize);
        int total = userMentionMapper.searchMentionsCount(keyword);

        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("mentions", mentions);
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
    public Map<String, Object> getMentionContext(Long mentionId) {
        log.debug("获取提及上下文: mentionId={}", mentionId);

        UserMention mention = userMentionMapper.findById(mentionId);
        if (mention == null) {
            throw new com.web.exception.WeebException("提及不存在: " + mentionId);
        }

        // 获取消息信息
        Message message = messageMapper.findById(mention.getMessageId());

        Map<String, Object> context = new HashMap<>();
        context.put("mention", mention);
        context.put("message", message);

        return context;
    }

    @Override
    public boolean validateMention(UserMention mention) {
        if (mention == null) {
            return false;
        }

        try {
            // 基本字段验证
            if (!mention.isValid()) {
                return false;
            }

            // 验证消息是否存在
            Message message = messageMapper.findById(mention.getMessageId());
            if (message == null) {
                log.warn("提及关联的消息不存在: messageId={}", mention.getMessageId());
                return false;
            }

            // 验证用户是否存在
            User mentioner = userMapper.findById(mention.getMentionerId());
            if (mentioner == null) {
                log.warn("提及者不存在: mentionerId={}", mention.getMentionerId());
                return false;
            }

            User mentionedUser = userMapper.findById(mention.getMentionedUserId());
            if (mentionedUser == null) {
                log.warn("被提及用户不存在: mentionedUserId={}", mention.getMentionedUserId());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("验证提及失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getPopularMentionedUsers(int limit) {
        log.debug("获取热门提及用户: limit={}", limit);
        return userMentionMapper.findPopularMentionedUsers(limit);
    }

    @Override
    public List<Map<String, Object>> getMentionTrends(Long userId, int days) {
        log.debug("获取用户提及趋势: userId={}, days={}", userId, days);
        return userMentionMapper.findMentionTrends(userId, days);
    }
}