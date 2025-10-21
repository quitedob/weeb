package com.web.service.Impl;

import com.web.mapper.MessageThreadMapper;
import com.web.mapper.MessageMapper;
import com.web.mapper.UserMapper;
import com.web.model.MessageThread;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.MessageThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息线索服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageThreadServiceImpl implements MessageThreadService {

    private final MessageThreadMapper messageThreadMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public MessageThread createThread(Long rootMessageId, String title, Long createdBy) {
        log.info("创建消息线索: rootMessageId={}, title={}, createdBy={}", rootMessageId, title, createdBy);

        try {
            // 验证根消息是否存在
            Message rootMessage = messageMapper.findById(rootMessageId);
            if (rootMessage == null) {
                throw new IllegalArgumentException("根消息不存在: " + rootMessageId);
            }

            // 验证用户是否存在
            User creator = userMapper.findById(createdBy);
            if (creator == null) {
                throw new IllegalArgumentException("创建者不存在: " + createdBy);
            }

            // 创建线索
            MessageThread thread = new MessageThread();
            thread.setRootMessageId(rootMessageId);
            thread.setTitle(title != null ? title : generateTitleFromMessage(rootMessage));
            thread.setCreatedBy(createdBy);
            thread.setCreatedByUsername(creator.getUsername());

            messageThreadMapper.insert(thread);

            log.info("消息线索创建成功: threadId={}", thread.getId());
            return thread;

        } catch (Exception e) {
            log.error("创建消息线索失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建消息线索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public MessageThread getThreadById(Long threadId) {
        log.debug("获取线索详情: threadId={}", threadId);

        MessageThread thread = messageThreadMapper.findById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("线索不存在: " + threadId);
        }

        return thread;
    }

    @Override
    public Map<String, Object> getThreadMessages(Long threadId, int page, int pageSize) {
        log.debug("获取线索消息: threadId={}, page={}, pageSize={}", threadId, page, pageSize);

        // 验证线索是否存在
        MessageThread thread = getThreadById(threadId);

        // 计算偏移量
        int offset = (page - 1) * pageSize;

        // 获取消息列表
        List<Message> messages = messageThreadMapper.getThreadMessages(threadId, offset, pageSize);

        // 获取总数
        int total = messageThreadMapper.getThreadMessageCount(threadId);

        // 计算分页信息
        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("thread", thread);
        result.put("messages", messages);
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
    public Map<String, Object> replyToThread(Long threadId, String replyMessage, Long userId) {
        log.info("回复线索: threadId={}, userId={}", threadId, userId);

        try {
            // 验证线索是否存在且可回复
            MessageThread thread = getThreadById(threadId);
            if (!thread.canReply(userId)) {
                throw new IllegalArgumentException("线索不可回复");
            }

            // 创建回复消息
            Message reply = new Message();
            reply.setContent(replyMessage);
            reply.setUserId(userId);
            reply.setChatId(thread.getRootMessageId()); // 使用根消息的聊天ID
            reply.setReplyToId(thread.getRootMessageId()); // 标记为对根消息的回复
            reply.setThreadId(threadId); // 关联到线索

            messageMapper.insert(reply);

            // 更新线索最后回复信息
            User replier = userMapper.findById(userId);
            thread.setLastReplyAt(LocalDateTime.now());
            thread.setLastReplyBy(userId);
            thread.setLastReplyByUsername(replier != null ? replier.getUsername() : "未知用户");
            messageThreadMapper.update(thread);

            log.info("线索回复成功: threadId={}, messageId={}", threadId, reply.getId());

            return Map.of(
                "success", true,
                "message", reply,
                "thread", thread
            );

        } catch (Exception e) {
            log.error("回复线索失败: {}", e.getMessage(), e);
            throw new RuntimeException("回复线索失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean joinThread(Long threadId, Long userId) {
        log.info("加入线索: threadId={}, userId={}", threadId, userId);

        try {
            MessageThread thread = getThreadById(threadId);

            // 检查是否已经加入
            if (messageThreadMapper.isParticipant(threadId, userId)) {
                log.debug("用户已在线索中: threadId={}, userId={}", threadId, userId);
                return true;
            }

            // 添加参与者
            messageThreadMapper.addParticipant(threadId, userId);

            // 更新参与者数量
            thread.setParticipantCount(thread.getParticipantCount() + 1);
            messageThreadMapper.update(thread);

            log.info("成功加入线索: threadId={}, userId={}", threadId, userId);
            return true;

        } catch (Exception e) {
            log.error("加入线索失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean leaveThread(Long threadId, Long userId) {
        log.info("离开线索: threadId={}, userId={}", threadId, userId);

        try {
            MessageThread thread = getThreadById(threadId);

            // 检查是否在线索中
            if (!messageThreadMapper.isParticipant(threadId, userId)) {
                log.debug("用户不在线索中: threadId={}, userId={}", threadId, userId);
                return false;
            }

            // 移除参与者
            messageThreadMapper.removeParticipant(threadId, userId);

            // 更新参与者数量
            int newCount = Math.max(1, thread.getParticipantCount() - 1); // 至少保持1个参与者（创建者）
            thread.setParticipantCount(newCount);
            messageThreadMapper.update(thread);

            log.info("成功离开线索: threadId={}, userId={}", threadId, userId);
            return true;

        } catch (Exception e) {
            log.error("离开线索失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean archiveThread(Long threadId, Long userId) {
        log.info("归档线索: threadId={}, userId={}", threadId, userId);

        try {
            MessageThread thread = getThreadById(threadId);

            // 只有创建者可以归档线索
            if (!thread.isCreatedBy(userId)) {
                throw new IllegalArgumentException("只有线索创建者可以归档线索");
            }

            thread.setStatus(MessageThread.Status.ARCHIVED);
            messageThreadMapper.update(thread);

            log.info("线索归档成功: threadId={}", threadId);
            return true;

        } catch (Exception e) {
            log.error("归档线索失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean closeThread(Long threadId, Long userId) {
        log.info("关闭线索: threadId={}, userId={}", threadId, userId);

        try {
            MessageThread thread = getThreadById(threadId);

            // 只有创建者可以关闭线索
            if (!thread.isCreatedBy(userId)) {
                throw new IllegalArgumentException("只有线索创建者可以关闭线索");
            }

            thread.setStatus(MessageThread.Status.CLOSED);
            messageThreadMapper.update(thread);

            log.info("线索关闭成功: threadId={}", threadId);
            return true;

        } catch (Exception e) {
            log.error("关闭线索失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean pinThread(Long threadId, Long userId, boolean isPinned) {
        log.info("置顶线索: threadId={}, userId={}, isPinned={}", threadId, userId, isPinned);

        try {
            MessageThread thread = getThreadById(threadId);

            // 只有创建者可以置顶线索
            if (!thread.isCreatedBy(userId)) {
                throw new IllegalArgumentException("只有线索创建者可以置顶线索");
            }

            thread.setIsPinned(isPinned);
            messageThreadMapper.update(thread);

            log.info("线索置顶{}成功: threadId={}", isPinned ? "" : "取消", threadId);
            return true;

        } catch (Exception e) {
            log.error("置顶线索失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean lockThread(Long threadId, Long userId, boolean isLocked) {
        log.info("锁定线索: threadId={}, userId={}, isLocked={}", threadId, userId, isLocked);

        try {
            MessageThread thread = getThreadById(threadId);

            // 只有创建者可以锁定线索
            if (!thread.isCreatedBy(userId)) {
                throw new IllegalArgumentException("只有线索创建者可以锁定线索");
            }

            thread.setIsLocked(isLocked);
            messageThreadMapper.update(thread);

            log.info("线索锁定{}成功: threadId={}", isLocked ? "" : "解除", threadId);
            return true;

        } catch (Exception e) {
            log.error("锁定线索失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getUserThreads(Long userId, int page, int pageSize) {
        log.debug("获取用户线索: userId={}, page={}, pageSize={}", userId, page, pageSize);

        int offset = (page - 1) * pageSize;

        List<MessageThread> threads = messageThreadMapper.getUserThreads(userId, offset, pageSize);
        int total = messageThreadMapper.getUserThreadCount(userId);

        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("threads", threads);
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
    public Map<String, Object> getActiveThreads(int page, int pageSize) {
        log.debug("获取活跃线索: page={}, pageSize={}", page, pageSize);

        int offset = (page - 1) * pageSize;

        List<MessageThread> threads = messageThreadMapper.getActiveThreads(offset, pageSize);
        int total = messageThreadMapper.getActiveThreadCount();

        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("threads", threads);
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
    public Map<String, Object> getUserCreatedThreads(Long userId, int page, int pageSize) {
        log.debug("获取用户创建的线索: userId={}, page={}, pageSize={}", userId, page, pageSize);

        int offset = (page - 1) * pageSize;

        List<MessageThread> threads = messageThreadMapper.getUserCreatedThreads(userId, offset, pageSize);
        int total = messageThreadMapper.getUserCreatedThreadCount(userId);

        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("threads", threads);
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
    public Map<String, Object> searchThreads(String keyword, int page, int pageSize) {
        log.debug("搜索线索: keyword={}, page={}, pageSize={}", keyword, page, pageSize);

        int offset = (page - 1) * pageSize;

        List<MessageThread> threads = messageThreadMapper.searchThreads(keyword, offset, pageSize);
        int total = messageThreadMapper.searchThreadCount(keyword);

        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("threads", threads);
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
    public Map<String, Object> getThreadStatistics(Long threadId) {
        log.debug("获取线索统计: threadId={}", threadId);

        MessageThread thread = getThreadById(threadId);

        // 获取消息统计
        int messageCount = messageThreadMapper.getThreadMessageCount(threadId);
        int participantCount = thread.getParticipantCount();

        // 获取最近活动
        Message lastMessage = messageThreadMapper.getLastThreadMessage(threadId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("threadId", threadId);
        statistics.put("title", thread.getTitle());
        statistics.put("status", thread.getStatus());
        statistics.put("messageCount", messageCount);
        statistics.put("participantCount", participantCount);
        statistics.put("createdAt", thread.getCreatedAt());
        statistics.put("lastReplyAt", thread.getLastReplyAt());
        statistics.put("lastReplyBy", thread.getLastReplyByUsername());
        statistics.put("isPinned", thread.getIsPinned());
        statistics.put("isLocked", thread.getIsLocked());
        statistics.put("lastMessage", lastMessage);

        return statistics;
    }

    @Override
    public Map<String, Object> getThreadContext(Long messageId, Long userId) {
        log.debug("获取消息线索上下文: messageId={}, userId={}", messageId, userId);

        // 获取消息
        Message message = messageMapper.findById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("消息不存在: " + messageId);
        }

        // 检查消息是否属于某个线索
        Long threadId = message.getThreadId();
        if (threadId == null) {
            // 如果消息不属于线索，检查是否可以作为根消息创建线索
            return Map.of(
                "canCreateThread", true,
                "message", message,
                "thread", null
            );
        }

        // 获取线索信息
        MessageThread thread = getThreadById(threadId);

        // 检查用户是否有权限访问该线索
        boolean hasAccess = messageThreadMapper.isParticipant(threadId, userId) ||
                           thread.isCreatedBy(userId);

        Map<String, Object> context = new HashMap<>();
        context.put("thread", thread);
        context.put("message", message);
        context.put("hasAccess", hasAccess);
        context.put("isParticipant", messageThreadMapper.isParticipant(threadId, userId));
        context.put("canReply", hasAccess && thread.canReply(userId));

        return context;
    }

    /**
     * 从消息内容生成标题
     */
    private String generateTitleFromMessage(Message message) {
        String content = message.getContent();
        if (content == null || content.trim().isEmpty()) {
            return "新对话线索";
        }

        // 截取前50个字符作为标题
        String title = content.length() > 50 ? content.substring(0, 47) + "..." : content;
        return title.replace("\n", " ").trim();
    }
}