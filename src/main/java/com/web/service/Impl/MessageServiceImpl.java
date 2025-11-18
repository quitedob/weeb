package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.MessageMapper;
import com.web.mapper.MessageReactionMapper;
import com.web.model.Message;
import com.web.model.MessageReaction;
import com.web.model.elasticsearch.MessageDocument;
import com.web.service.MessageService;
import com.web.service.ElasticsearchSearchService;
import com.web.service.RedisCacheService;
import com.web.vo.message.ReactionVo;
import com.web.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 消息服务实现类
 * 处理消息发送、接收、撤回等业务逻辑
 */
@Slf4j
@Service
@Transactional
@SuppressWarnings("deprecation")
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageReactionMapper messageReactionMapper;

    @Autowired(required = false)
    private ElasticsearchSearchService elasticsearchSearchService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public Message send(Long userId, Message messageBody) {
        // 设置发送者ID
        messageBody.setSenderId(userId);
        // 设置发送时间
        messageBody.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        // 设置消息状态为正常
        // 使用新的状态字段替代已过时的isRecalled字段
        if (messageBody.getStatus() == null) {
            messageBody.setStatus(Message.STATUS_SENT);
        }
        
        // 根据消息类型选择不同的发送方式
        if (messageBody.getMessageType() != null && messageBody.getMessageType() == 1) {
            // 群组消息
            return sendMessageToGroup(userId, messageBody);
        } else {
            // 私聊消息
            return sendMessageToUser(userId, messageBody);
        }
    }

    @Override
    public List<Message> record(Long userId, Long targetId, int index, int num) {
        return messageMapper.record(userId, targetId, index, num);
    }

    @Override
    public Message recall(Long userId, Long msgId) {
        // 先查询消息是否存在且属于当前用户
        Message message = messageMapper.selectMessageById(msgId);
        if (message == null || !message.getSenderId().equals(userId)) {
            throw new WeebException("消息不存在或无权限撤回");
        }
        
        // 检查消息是否已经被撤回
        if (message.getIsRecalled() != null && message.getIsRecalled() == 1) {
            throw new WeebException("消息已经被撤回");
        }
        
        // 检查撤回时间限制（例如：只能撤回2分钟内的消息）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sendTime = message.getCreatedAt().toLocalDateTime();
        if (sendTime.plusMinutes(2).isBefore(now)) {
            throw new WeebException("超过撤回时间限制");
        }
        
        // 标记消息为已撤回
        int result = messageMapper.markMessageAsRecalled(msgId);
        if (result > 0) {
            message.setIsRecalled(1);
            return message;
        }
        
        throw new WeebException("撤回消息失败");
    }

    @Override
    public void deleteExpiredMessages(LocalDate expirationDate) {
        messageMapper.deleteExpiredMessages(expirationDate);
    }

    @Override
    public Message sendMessageToGroup(Long userId, Message messageBody) {
        // 设置消息类型为群组消息
        messageBody.setMessageType(1);

        // 插入群组消息
        int result = messageMapper.insertGroupMessage(messageBody);
        if (result > 0) {
            // 异步索引到Elasticsearch
            indexMessageToElasticsearch(messageBody);
            // 缓存消息到Redis
            cacheMessageToRedis(messageBody);
            return messageBody;
        }

        throw new WeebException("发送群组消息失败");
    }

    @Override
    public Message sendMessageToUser(Long userId, Message messageBody) {
        // 设置消息类型为私聊消息
        messageBody.setMessageType(0);

        // 插入私聊消息
        int result = messageMapper.insertMessage(messageBody);
        if (result > 0) {
            // 异步索引到Elasticsearch
            indexMessageToElasticsearch(messageBody);
            // 缓存消息到Redis
            cacheMessageToRedis(messageBody);
            return messageBody;
        }

        throw new WeebException("发送私聊消息失败");
    }

    @Override
    public void handleReaction(ReactionVo reactionVo, Long userId) {
        // 检查消息是否存在
        Message message = messageMapper.selectMessageById(reactionVo.getMessageId());
        if (message == null) {
            throw new WeebException("消息不存在");
        }
        
        // 检查用户是否已经对该消息有反应
        List<MessageReaction> existingReactions = messageReactionMapper.selectByMessageIdAndUserId(
            reactionVo.getMessageId(), userId);

        if ("add".equals(reactionVo.getAction())) {
            // 添加反应
            if (existingReactions != null && !existingReactions.isEmpty()) {
                // 如果已存在反应，更新反应类型
                MessageReaction existingReaction = existingReactions.get(0);
                existingReaction.setReactionType(reactionVo.getReactionType());
                messageReactionMapper.updateById(existingReaction);
            } else {
                // 创建新的反应
                MessageReaction newReaction = new MessageReaction();
                newReaction.setMessageId(reactionVo.getMessageId());
                newReaction.setUserId(userId);
                newReaction.setReactionType(reactionVo.getReactionType());
                newReaction.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                messageReactionMapper.insert(newReaction);
            }
        } else if ("remove".equals(reactionVo.getAction())) {
            // 移除反应
            if (existingReactions != null && !existingReactions.isEmpty()) {
                messageReactionMapper.deleteByMessageIdAndUserId(reactionVo.getMessageId(), userId);
            }
        } else {
            throw new WeebException("不支持的反应操作");
        }
    }

    // ==================== Elasticsearch集成方法 ====================

    /**
     * 异步索引消息到Elasticsearch
     * @param message 消息对象
     */
    private void indexMessageToElasticsearch(Message message) {
        if (elasticsearchSearchService == null) {
            log.debug("Elasticsearch服务未启用，跳过消息索引");
            return;
        }

        try {
            // 转换为ES文档
            MessageDocument document = convertToMessageDocument(message);
            // 异步索引
            elasticsearchSearchService.indexMessage(document);
            log.debug("消息已索引到Elasticsearch: messageId={}", message.getId());
        } catch (Exception e) {
            log.error("消息索引到Elasticsearch失败: messageId={}", message.getId(), e);
        }
    }

    /**
     * 转换消息为ES文档
     * @param message 消息对象
     * @return ES文档
     */
    private MessageDocument convertToMessageDocument(Message message) {
        MessageDocument document = new MessageDocument();
        document.setId(message.getId());
        document.setFromId(message.getSenderId());
        document.setChatListId(message.getChatListId());
        document.setContent(message.getContent().toString());
        document.setSendTime(message.getCreatedAt());
        return document;
    }

    // ==================== Redis缓存方法 ====================

    /**
     * 缓存消息到Redis
     * @param message 消息对象
     */
    private void cacheMessageToRedis(Message message) {
        try {
            String cacheKey = "message:" + message.getId();
            redisCacheService.set(cacheKey, message, 3600); // 缓存1小时
            log.debug("消息已缓存到Redis: messageId={}", message.getId());
        } catch (Exception e) {
            log.error("消息缓存到Redis失败: messageId={}", message.getId(), e);
        }
    }

    /**
     * 从Redis获取缓存的消息
     * @param messageId 消息ID
     * @return 消息对象，如果不存在返回null
     */
    public Message getCachedMessage(Long messageId) {
        try {
            String cacheKey = "message:" + messageId;
            Object cached = redisCacheService.get(cacheKey);
            if (cached instanceof Message) {
                log.debug("命中消息缓存: messageId={}", messageId);
                return (Message) cached;
            }
        } catch (Exception e) {
            log.error("获取消息缓存失败: messageId={}", messageId, e);
        }
        return null;
    }

    /**
     * 删除消息缓存
     * @param messageId 消息ID
     */
    public void evictMessageCache(Long messageId) {
        try {
            String cacheKey = "message:" + messageId;
            redisCacheService.delete(cacheKey);
            log.debug("删除消息缓存: messageId={}", messageId);
        } catch (Exception e) {
            log.error("删除消息缓存失败: messageId={}", messageId, e);
        }
    }

    @Override
    public Message saveWebSocketMessage(java.util.Map<String, Object> messageData, Long userId) {
        try {
            // 创建消息对象
            Message message = new Message();

            // 设置基本信息
            message.setSenderId(userId);
            message.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            message.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            // 从Map中提取数据
            String content = (String) messageData.get("content");
            String roomId = (String) messageData.get("roomId");
            String type = (String) messageData.getOrDefault("type", "text");

            // 设置消息内容
            com.web.vo.message.TextMessageContent textContent = new com.web.vo.message.TextMessageContent();
            textContent.setContent(content);

            // 根据类型设置contentType
            if ("image".equals(type)) {
                textContent.setContentType(com.web.constant.TextContentType.IMAGE.getCode());
                textContent.setUrl((String) messageData.get("url"));
            } else if ("file".equals(type)) {
                textContent.setContentType(com.web.constant.TextContentType.FILE.getCode());
                textContent.setUrl((String) messageData.get("url"));
            } else {
                textContent.setContentType(com.web.constant.TextContentType.TEXT.getCode());
            }

            message.setContent(textContent);

            // 设置消息类型 (0: 私聊, 1: 群聊)
            if (roomId != null && roomId.startsWith("group_")) {
                message.setMessageType(1); // 群聊
                // 这里需要根据roomId获取chatId，暂时使用roomId转换
                message.setChatId(extractChatIdFromRoomId(roomId));
            } else {
                message.setMessageType(0); // 私聊
                message.setChatId(extractChatIdFromRoomId(roomId));
            }

            // 设置其他默认值
            message.setReadStatus(0); // 未读
            message.setIsRecalled(0); // 未撤回
            message.setIsShowTime(1); // 显示时间
            message.setUserIp("WebSocket"); // 标记来源为WebSocket
            message.setSource("WebSocket");

            // 设置回复信息（如果有）
            if (messageData.containsKey("replyToMessageId")) {
                Object replyToId = messageData.get("replyToMessageId");
                if (replyToId != null) {
                    message.setReplyToMessageId(Long.valueOf(replyToId.toString()));
                }
            }

            // 设置线程信息（如果有）
            if (messageData.containsKey("threadId")) {
                Object threadId = messageData.get("threadId");
                if (threadId != null) {
                    message.setThreadId(Long.valueOf(threadId.toString()));
                }
            }

            // 保存到数据库
            messageMapper.insert(message);

            // 缓存到Redis
            cacheMessageToRedis(message);

            // 如果启用了Elasticsearch，索引到ES
            if (elasticsearchSearchService != null) {
                try {
                    elasticsearchSearchService.indexMessage(convertToMessageDocument(message));
                    log.debug("消息已索引到Elasticsearch: messageId={}", message.getId());
                } catch (Exception e) {
                    log.warn("消息索引到Elasticsearch失败: messageId={}", message.getId(), e);
                }
            }

            log.info("WebSocket消息已保存: messageId={}, userId={}, roomId={}",
                    message.getId(), userId, roomId);

            return message;

        } catch (Exception e) {
            log.error("保存WebSocket消息失败: userId={}, messageData={}", userId, messageData, e);
            throw new WeebException("保存消息失败: " + e.getMessage());
        }
    }

    /**
     * 从roomId中提取chatId
     * 这里需要根据实际的业务逻辑来实现
     */
    private Long extractChatIdFromRoomId(String roomId) {
        try {
            if (roomId == null) {
                return null;
            }

            // 如果roomId是数字格式，直接转换
            if (roomId.matches("\\d+")) {
                return Long.valueOf(roomId);
            }

            // 如果roomId包含前缀，提取数字部分
            if (roomId.startsWith("private_")) {
                String idStr = roomId.substring("private_".length());
                return Long.valueOf(idStr);
            }

            if (roomId.startsWith("group_")) {
                String idStr = roomId.substring("group_".length());
                return Long.valueOf(idStr);
            }

            // 默认情况下，尝试直接转换
            return Long.valueOf(roomId);

        } catch (NumberFormatException e) {
            log.warn("无法从roomId提取chatId: roomId={}", roomId);
            return null;
        }
    }

    // ==================== 消息线程相关方法实现 ====================

    @Override
    public Message createThread(Long userId, Long parentMessageId, String content) {
        try {
            // 验证父消息是否存在
            Message parentMessage = messageMapper.selectById(parentMessageId);
            if (parentMessage == null) {
                throw new WeebException("父消息不存在: " + parentMessageId);
            }

            // 创建线程消息
            Message threadMessage = new Message();
            threadMessage.setSenderId(userId);
            threadMessage.setChatId(parentMessage.getChatId());
            threadMessage.setThreadId(parentMessageId); // 设置线程ID为父消息ID
            threadMessage.setReplyToMessageId(parentMessageId); // 设置回复消息ID
            threadMessage.setMessageType(1); // 文本消息类型
            threadMessage.setReadStatus(0); // 未读状态
            threadMessage.setIsRecalled(0); // 未撤回状态
            threadMessage.setIsShowTime(1); // 显示时间

            // 设置消息内容
            com.web.vo.message.TextMessageContent textContent = new com.web.vo.message.TextMessageContent();
            textContent.setContent(content);
            textContent.setContentType(com.web.constant.TextContentType.TEXT.getCode());
            threadMessage.setContent(textContent);

            // 设置时间戳
            Timestamp now = new Timestamp(System.currentTimeMillis());
            threadMessage.setCreatedAt(now);
            threadMessage.setUpdatedAt(now);

            // 保存到数据库
            int result = messageMapper.insert(threadMessage);
            if (result <= 0) {
                throw new WeebException("创建线程消息失败");
            }

            // 缓存消息
            cacheMessageToRedis(threadMessage);

            // 索引到Elasticsearch
            indexMessageToElasticsearch(threadMessage);

            log.info("用户 {} 创建了线程消息，父消息ID: {}", userId, parentMessageId);

            return threadMessage;

        } catch (Exception e) {
            log.error("创建消息线程失败: userId={}, parentMessageId={}, content={}",
                     userId, parentMessageId, content, e);
            throw new WeebException("创建线程失败: " + e.getMessage());
        }
    }

    @Override
    public java.util.Map<String, Object> getThreadMessages(Long parentMessageId, int page, int pageSize) {
        try {
            // 计算偏移量
            int offset = (page - 1) * pageSize;

            // 查询线程消息列表
            List<Message> threadMessages = messageMapper.selectThreadMessages(
                parentMessageId, offset, pageSize);

            // 查询总数量
            int totalCount = messageMapper.countThreadMessages(parentMessageId);

            // 构建返回结果
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("messages", threadMessages);
            result.put("pagination", java.util.Map.of(
                "page", page,
                "pageSize", pageSize,
                "total", totalCount,
                "totalPages", (int) Math.ceil((double) totalCount / pageSize)
            ));

            return result;

        } catch (Exception e) {
            log.error("获取线程消息失败: parentMessageId={}, page={}, pageSize={}",
                     parentMessageId, page, pageSize, e);
            throw new WeebException("获取线程消息失败: " + e.getMessage());
        }
    }

    @Override
    public java.util.Map<String, Object> getThreadSummary(Long parentMessageId) {
        try {
            // 查询线程摘要信息
            java.util.Map<String, Object> summary = messageMapper.selectThreadSummary(parentMessageId);

            if (summary == null || summary.isEmpty()) {
                return java.util.Map.of(
                    "messageCount", 0,
                    "participantCount", 0,
                    "lastReplyTime", null,
                    "lastReplyUser", null
                );
            }

            return summary;

        } catch (Exception e) {
            log.error("获取线程摘要失败: parentMessageId={}", parentMessageId, e);
            throw new WeebException("获取线程摘要失败: " + e.getMessage());
        }
    }

    @Override
    public Message replyToThread(Long userId, Long threadId, String content) {
        try {
            // 验证线程消息是否存在
            Message threadMessage = messageMapper.selectById(threadId);
            if (threadMessage == null) {
                throw new WeebException("线程消息不存在: " + threadId);
            }

            // 创建回复消息
            Message replyMessage = new Message();
            replyMessage.setSenderId(userId);
            replyMessage.setChatId(threadMessage.getChatId());
            replyMessage.setThreadId(threadMessage.getThreadId()); // 使用相同的线程ID
            replyMessage.setReplyToMessageId(threadId); // 回复当前线程消息
            replyMessage.setMessageType(1); // 文本消息类型
            replyMessage.setReadStatus(0); // 未读状态
            replyMessage.setIsRecalled(0); // 未撤回状态
            replyMessage.setIsShowTime(1); // 显示时间

            // 设置消息内容
            com.web.vo.message.TextMessageContent textContent = new com.web.vo.message.TextMessageContent();
            textContent.setContent(content);
            textContent.setContentType(com.web.constant.TextContentType.TEXT.getCode());
            replyMessage.setContent(textContent);

            // 设置时间戳
            Timestamp now = new Timestamp(System.currentTimeMillis());
            replyMessage.setCreatedAt(now);
            replyMessage.setUpdatedAt(now);

            // 保存到数据库
            int result = messageMapper.insert(replyMessage);
            if (result <= 0) {
                throw new WeebException("回复线程消息失败");
            }

            // 缓存消息
            cacheMessageToRedis(replyMessage);

            // 索引到Elasticsearch
            indexMessageToElasticsearch(replyMessage);

            log.info("用户 {} 回复了线程消息，线程ID: {}", userId, threadId);

            return replyMessage;

        } catch (Exception e) {
            log.error("回复线程失败: userId={}, threadId={}, content={}",
                     userId, threadId, content, e);
            throw new WeebException("回复线程失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteThread(Long threadId, Long userId) {
        try {
            // 验证线程消息是否存在
            Message threadMessage = messageMapper.selectById(threadId);
            if (threadMessage == null) {
                throw new WeebException("线程消息不存在: " + threadId);
            }

            // 检查权限：只有发送者可以删除自己的消息
            if (!threadMessage.getSenderId().equals(userId)) {
                throw new WeebException("无权限删除此消息");
            }

            // 删除消息
            int result = messageMapper.deleteById(threadId);
            if (result <= 0) {
                throw new WeebException("删除线程消息失败");
            }

            // 清除缓存
            evictMessageCache(threadId);

            // 从Elasticsearch删除索引
            if (elasticsearchSearchService != null) {
                try {
                    elasticsearchSearchService.deleteMessage(threadId);
                } catch (Exception e) {
                    log.warn("从Elasticsearch删除消息索引失败: messageId={}", threadId, e);
                }
            }

            log.info("用户 {} 删除了线程消息: {}", userId, threadId);

            return true;

        } catch (Exception e) {
            log.error("删除线程失败: userId={}, threadId={}", userId, threadId, e);
            throw new WeebException("删除线程失败: " + e.getMessage());
        }
    }

    @Override
    public java.util.Map<String, Object> getUserThreads(Long userId, int page, int pageSize) {
        try {
            // 计算偏移量
            int offset = (page - 1) * pageSize;

            // 查询用户参与的线程
            List<java.util.Map<String, Object>> userThreads = messageMapper.selectUserThreads(
                userId, offset, pageSize);

            // 查询总数量
            int totalCount = messageMapper.countUserThreads(userId);

            // 构建返回结果
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("threads", userThreads);
            result.put("pagination", java.util.Map.of(
                "page", page,
                "pageSize", pageSize,
                "total", totalCount,
                "totalPages", (int) Math.ceil((double) totalCount / pageSize)
            ));

            return result;

        } catch (Exception e) {
            log.error("获取用户线程失败: userId={}, page={}, pageSize={}",
                     userId, page, pageSize, e);
            throw new WeebException("获取用户线程失败: " + e.getMessage());
        }
    }
}