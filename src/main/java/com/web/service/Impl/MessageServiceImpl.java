package com.web.service.Impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.constant.MessageSource;
import com.web.constant.MessageType;
import com.web.constant.TextContentType;
import com.web.constant.UserType;
import com.web.exception.WeebException;
import com.web.mapper.MessageMapper;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.*;
import com.web.util.CacheUtil;
import com.web.util.IpUtil;
import com.web.vo.message.TextMessageContent;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Date; // For MessageReaction
import org.springframework.transaction.annotation.Transactional;
import com.web.mapper.MessageReactionMapper; // Assuming this will be created
import com.web.model.MessageReaction;
import com.web.vo.message.ReactionVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper; // For reaction query
import com.web.mapper.GroupMemberMapper; // Added for group message routing
import com.web.mapper.ChatListMapper;   // Added for group message routing
import com.web.model.ChatList;         // Added for group message routing
import com.web.constant.ChatListType;  // Added for group message routing
import org.springframework.data.redis.core.RedisTemplate; // Added for Redis Pub/Sub
import com.web.Config.RedisConfig; // Added for Redis Pub/Sub
import com.web.dto.RedisBroadcastMsg; // Added for Redis Pub/Sub
import com.web.dto.NotifyDto; // For recall and reaction notifications
import com.web.constant.WsContentType; // For notification types
import java.util.Map; // For NotifyDto data for reactions
import org.springframework.scheduling.annotation.Async; // Added for @Async
import com.web.model.elasticsearch.MessageDocument; // Added for ES
import com.web.repository.MessageSearchRepository; // Added for ES

/**
 * 消息服务实现类，处理消息的发送、记录获取和撤回操作
 */
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private static final int MAX_MESSAGE_LENGTH = 500; // Added constant

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private ChatListService chatListService;

    @Resource
    private AuthService authService;

    @Resource
    private WebSocketService webSocketService;

    @Resource
    private CacheUtil cacheUtil;

    @Resource
    private SensitiveWordBs sensitiveWordBs;

    @Resource
    private AiChatService aiChatService;

    @Resource // or @Autowired
    private MessageReactionMapper messageReactionMapper; // Assuming this will be created

    @Resource
    private GroupMemberMapper groupMemberMapper; // Injected for group message routing

    @Resource
    private ChatListMapper chatListMapper;     // Injected for group message routing

    @Resource
    private RedisTemplate<String, Object> redisTemplate; // Injected for Redis Pub/Sub

    @Autowired(required = false)
    private MessageSearchRepository messageSearchRepository; // Injected for ES (optional)

    /**
     * 发送消息，根据消息来源调用相应的方法
     *
     * @param userId      当前用户ID，类型为 Long
     * @param messageBody 消息对象，包含消息内容、目标等信息
     * @return 发送后的消息对象
     */
    @Override
    public Message send(Long userId, Message messageBody) {
        if (MessageSource.Group.equals(messageBody.getSource())) {
            return sendMessageToGroup(userId, messageBody);
        } else {
            return sendMessageToUser(userId, messageBody);
        }
    }

    /**
     * 获取聊天记录
     *
     * @param userId   当前用户ID，类型为 Long
     * @param targetId 目标聊天对象ID，类型为 Long
     * @param index    起始索引
     * @param num      查询条数
     * @return 聊天记录列表
     */
    @Override
    public List<Message> record(Long userId, Long targetId, int index, int num) {
        List<Message> messages = messageMapper.record(userId, targetId, index, num);
        cacheUtil.putUserReadCache(userId, targetId); // 修正为传入 Long 类型
        return messages;
    }

    /**
     * 撤回消息
     *
     * @param userId 当前用户ID，类型为 Long
     * @param msgId  要撤回的消息ID，类型为 Long
     * @return 撤回后的消息对象
     */
    @Override
    public Message recall(Long userId, Long msgId) {
        Message message = getById(msgId);
        if (message == null) {
            throw new WeebException("消息不存在~");
        }
        if (!message.getSenderId().equals(userId)) {
            throw new WeebException("仅能撤回自己的消息~");
        }

        // 检查消息是否在2分钟内
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long minutes = DateUtil.between(message.getCreatedAt(), now, DateUnit.MINUTE);
        if (minutes > 2) {
            throw new WeebException("消息已超过2分钟，无法撤回~");
        }

        // 设置消息为已撤回
        message.setMessageType(1); // 设置为撤回类型
        message.setContent(null); // 清空内容
        message.setIsRecalled(1);
        message.setUpdatedAt(now);
        updateById(message);

        // Notification part:
        WebSocketService.WsContent wsContent = new WebSocketService.WsContent();
        wsContent.setType(WsContentType.MSG.getCode()); // Or a specific "recall_notify" type
        wsContent.setContent(message); // Send the updated message object
        String wsContentJson = JSONUtil.toJsonStr(wsContent);

        if (MessageSource.Group.equals(message.getSource())) {
            chatListService.updateGroupChatLastMessage(message); // Already here
            // Publish to all group members
            ChatList chatListForGroup = chatListMapper.selectById(message.getChatId());
            if (chatListForGroup != null && ChatListType.GROUP.getCode().equals(chatListForGroup.getType()) && chatListForGroup.getGroupId() != null) {
                List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(chatListForGroup.getGroupId());
                if (memberIds != null && !memberIds.isEmpty()) {
                    for (Long memberId : memberIds) {
                        RedisBroadcastMsg broadcastMsg = RedisBroadcastMsg.builder()
                                .targetUserId(memberId)
                                .messageBody(wsContentJson)
                                .build();
                        redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, broadcastMsg);
                    }
                    log.info("Published recall notification for messageId {} to group members of groupId {}", msgId, chatListForGroup.getGroupId());
                }
            }
        } else { // Private chat
            chatListService.updatePrivateChatLastMessage(userId, message.getChatId(), message); // Already here

            // Determine other participant in private chat
            ChatList privateChat = chatListMapper.selectById(message.getChatId());
            if (privateChat != null) {
                Long user1 = privateChat.getUserId();
                Long user2 = privateChat.getTargetId();
                Long otherParticipantId = userId.equals(user1) ? user2 : user1;

                // Publish to sender (for sync) and other participant
                RedisBroadcastMsg toSenderMsg = RedisBroadcastMsg.builder().targetUserId(userId).messageBody(wsContentJson).build();
                redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, toSenderMsg);

                if (!userId.equals(otherParticipantId)) {
                   RedisBroadcastMsg toReceiverMsg = RedisBroadcastMsg.builder().targetUserId(otherParticipantId).messageBody(wsContentJson).build();
                   redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, toReceiverMsg);
                }
                log.info("Published recall notification for messageId {} to participants of private chat {}", msgId, message.getChatId());
            }
        }
        return message;
    }

    /**
     * 删除过期消息
     *
     * @param expirationDate 过期日期
     */
    @Override
    public void deleteExpiredMessages(LocalDate expirationDate) {
        Timestamp expirationTimestamp = Timestamp.valueOf(expirationDate.atStartOfDay());
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(Message::getCreatedAt, expirationTimestamp);
        if (remove(queryWrapper)) {
            log.info("---清理过期消息成功---");
        } else {
            log.warn("---清理过期消息失败---");
        }
    }

    /**
     * 发送群组消息
     *
     * @param userId      当前用户ID，类型为 Long
     * @param messageBody 消息对象，包含消息内容、目标等信息
     * @return 发送后的消息对象
     */
    @Override
    public Message sendMessageToGroup(Long userId, Message messageBody) {
        Message message = sendMessage(userId, messageBody, MessageSource.Group);

        if (message != null && message.getChatId() != null) {
            ChatList chatListForGroup = chatListMapper.selectById(message.getChatId());

            if (chatListForGroup != null && ChatListType.GROUP.getCode().equals(chatListForGroup.getType())) {
                Long actualGroupId = chatListForGroup.getGroupId();
                if (actualGroupId != null) {
                    List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(actualGroupId);

                    if (memberIds != null && !memberIds.isEmpty()) {
                        log.info("Publishing group messageId: {} to Redis for members: {} of groupId: {}", message.getId(), memberIds, actualGroupId);

                        WebSocketService.WsContent wsContent = new WebSocketService.WsContent();
                        wsContent.setType(WsContentType.MSG.getCode());
                        wsContent.setContent(message);
                        String wsContentJson = JSONUtil.toJsonStr(wsContent);

                        for (Long memberId : memberIds) {
                            RedisBroadcastMsg broadcastMsg = RedisBroadcastMsg.builder()
                                    .targetUserId(memberId)
                                    .messageBody(wsContentJson)
                                    .build();
                            redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, broadcastMsg);
                        }
                    }
                } else {
                    log.error("ChatList entry for group chat (id: {}) has a null groupId field.", chatListForGroup.getId());
                }
            } else {
                 log.error("Could not find ChatList or it's not a GROUP type for chatId: {} from messageId: {}", message.getChatId(), message.getId());
            }
        } else {
            log.error("Message object or its chatId is null after saving. Cannot route group message for messageBody with chatId: {}", messageBody.getChatId());
            if (message == null && messageBody != null) {
                log.error("Message save operation may have failed for senderId: {}, targetChatId: {}", userId, messageBody.getChatId());
            }
        }

        if (message != null) {
            chatListService.updateGroupChatLastMessage(message);
        }
        return message;
    }

    /**
     * 发送私聊消息
     *
     * @param userId      当前用户ID，类型为 Long
     * @param messageBody 消息对象，包含消息内容、目标等信息
     * @return 发送后的消息对象
     */
    @Override
    public Message sendMessageToUser(Long userId, Message messageBody) {
        Message message = sendMessage(userId, messageBody, MessageSource.User);
        // 更新私聊列表
        chatListService.updatePrivateChatLastMessage(userId, messageBody.getChatId(), message);
        // 发送消息到用户
        webSocketService.sendMsgToUser(message, userId, messageBody.getChatId());
        return message;
    }

    /**
     * 发送消息的内部方法
     *
     * @param userId      当前用户ID，类型为 Long
     * @param messageBody 消息对象
     * @param source      消息来源（Group/User）
     * @return 发送后的消息对象
     */
    private Message sendMessage(Long userId, Message messageBody, String source) {
        // 获取上一条需要显示时间的消息
        Message previousMessage = messageMapper.getPreviousShowTimeMsg(userId, messageBody.getChatId());

        // 设置消息基本信息
        messageBody.setId(IdUtil.getSnowflakeNextId()); // 生成唯一的 Long 类型 ID
        messageBody.setSenderId(userId);
        messageBody.setSource(source);
        messageBody.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        messageBody.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        messageBody.setIsRecalled(0); // 初始为未撤回

        // --- ADD LENGTH CHECK HERE ---
        if (messageBody.getContent() != null &&
            com.web.constant.TextContentType.TEXT.getCode() == messageBody.getContent().getContentType()) {

            String textContent = messageBody.getContent().getContent();
            if (textContent != null && textContent.length() > MAX_MESSAGE_LENGTH) {
                throw new WeebException("消息内容过长，请控制在" + MAX_MESSAGE_LENGTH + "字符以内");
            }
        }
        // --- END OF LENGTH CHECK ---

        StringBuilder sb = new StringBuilder();
        // 用于保存机器人回复相关信息
        AtomicReference<User> botUserRef = new AtomicReference<>(null);

        if (messageBody.getMessageType() == 1) { // 使用数字类型判断文本消息
            String msgContent = messageBody.getContent().getContent();
            // 如果消息内容不是以 '[' 开头，说明格式不正确，则尝试包装为 JSON 数组
            if (!msgContent.trim().startsWith("[")) {
                msgContent = "[" + JSONUtil.toJsonStr(msgContent) + "]";
            }

            List<TextMessageContent> contents = JSONUtil.toList(msgContent, TextMessageContent.class);
            for (TextMessageContent content : contents) {
                if (TextContentType.TEXT.getCode() == content.getContentType()) {
                    // 替换敏感词
                    String sanitizedContent = sensitiveWordBs.replace(content.getContent());
                    content.setContent(sanitizedContent);
                }
            }
            // 将内容重新转换为 JSON 字符串，确保格式正确
            TextMessageContent newContent = new TextMessageContent();
            newContent.setContentType(TextContentType.TEXT.getCode());
            newContent.setContent(JSONUtil.toJsonStr(contents));
            messageBody.setContent(newContent);
        }

        // 获取发送者的详细信息
        User sender = authService.getUserByIdForTalk(userId);
        if (sender == null) {
            throw new WeebException("发送者信息不存在");
        }

        // 处理参考消息 - 暂时注释掉，因为Message实体中没有referenceMsgId字段
        // if (messageBody.getReferenceMsgId() != null) {
        //     Message referenceMessage = getById(messageBody.getReferenceMsgId());
        //     if (referenceMessage != null) {
        //         messageBody.setReferenceMsgId(referenceMessage.getId());
        //     }
        // }

        // 设置消息的基本信息
        messageBody.setSenderId(userId);
        messageBody.setSource(source);
        messageBody.setUserIp("127.0.0.1"); // 临时设置IP，实际应该从请求中获取
        messageBody.setReadStatus(0); // 未读
        messageBody.setIsRecalled(0); // 未撤回
        messageBody.setIsShowTime(0); // 根据需要设置

        // 保存消息到数据库
        save(messageBody);

        // 更新聊天列表的最后消息
        if (MessageSource.Group.equals(source)) {
            chatListService.updateGroupChatLastMessage(messageBody);
        } else {
            chatListService.updatePrivateChatLastMessage(userId, messageBody.getChatId(), messageBody);
        }

        // 发送WebSocket通知
        if (MessageSource.Group.equals(source)) {
            // 群聊消息通知
            List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(messageBody.getChatId());
            if (memberIds != null && !memberIds.isEmpty()) {
                for (Long memberId : memberIds) {
                    if (!memberId.equals(userId)) { // 不通知发送者自己
                        WebSocketService.WsContent wsContent = new WebSocketService.WsContent();
                        wsContent.setType(WsContentType.MSG.getCode());
                        wsContent.setContent(messageBody);
                        String wsContentJson = JSONUtil.toJsonStr(wsContent);

                        RedisBroadcastMsg broadcastMsg = RedisBroadcastMsg.builder()
                                .targetUserId(memberId)
                                .messageBody(wsContentJson)
                                .build();
                        redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, broadcastMsg);
                    }
                }
            }
        } else {
            // 私聊消息通知
            ChatList privateChat = chatListMapper.selectById(messageBody.getChatId());
            if (privateChat != null) {
                Long targetUserId = privateChat.getUserId().equals(userId) ? 
                    privateChat.getTargetId() : privateChat.getUserId();

                WebSocketService.WsContent wsContent = new WebSocketService.WsContent();
                wsContent.setType(WsContentType.MSG.getCode());
                wsContent.setContent(messageBody);
                String wsContentJson = JSONUtil.toJsonStr(wsContent);

                RedisBroadcastMsg broadcastMsg = RedisBroadcastMsg.builder()
                        .targetUserId(targetUserId)
                        .messageBody(wsContentJson)
                        .build();
                redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, broadcastMsg);
            }
        }

        // 异步索引消息到Elasticsearch
        indexMessage(messageBody);

        return messageBody;
    }

    @Override
    @Transactional // Ensure atomicity
    public void handleReaction(ReactionVo reactionVo, Long userId) { // Changed userId to Long
        Message message = getById(reactionVo.getMessageId());
        if (message == null || message.getIsRecalled() == 1) { // Using isRecalled from Message.java
            throw new WeebException("消息不存在或已撤回"); // Assuming WeebException is a custom exception class
        }

        // Assuming MessageReactionMapper has:
        // - selectOne(QueryWrapper)
        // - deleteById(Long id)
        // - insert(MessageReaction reaction)

        MessageReaction existingReaction = messageReactionMapper.selectOne(
            new QueryWrapper<MessageReaction>()
                .eq("message_id", reactionVo.getMessageId())
                .eq("user_id", userId)
                .eq("reaction_type", reactionVo.getEmoji())
        );

        if (existingReaction != null) {
            // Reaction exists, so remove it
            messageReactionMapper.deleteById(existingReaction.getId());
            log.info("Reaction removed for messageId: {}, userId: {}, emoji: {}",
                reactionVo.getMessageId(), userId, reactionVo.getEmoji());
        } else {
            // Reaction does not exist, so add it
            MessageReaction newReaction = new MessageReaction();
            newReaction.setMessageId(reactionVo.getMessageId());
            newReaction.setUserId(userId); // Use Long type
            newReaction.setReactionType(reactionVo.getEmoji()); // Use reactionType field
            newReaction.setCreateTime(new Date()); // Set current time for creation
            messageReactionMapper.insert(newReaction);
            log.info("Reaction added for messageId: {}, userId: {}, emoji: {}",
                reactionVo.getMessageId(), userId, reactionVo.getEmoji());
        }

        log.info("Reaction change by user {} for message {}. Publishing to Redis.", userId, reactionVo.getMessageId());
        Message reactedMessage = getById(reactionVo.getMessageId()); // Use getById from ServiceImpl (superClass)
        if (reactedMessage == null) {
            log.error("Cannot send reaction notification, original message {} not found.", reactionVo.getMessageId());
            return;
        }

        Map<String, Object> reactionData = Map.of(
            "messageId", reactionVo.getMessageId(),
            "emoji", reactionVo.getEmoji(),
            "reactingUserId", userId,
            "action", (existingReaction != null) ? "removed" : "added"
            // "updatedReactions": fetchUpdatedReactionsForMessage(reactionVo.getMessageId()) // Example for more complex payload
        );
        // Using WsContentType.NOTIFICATION for generic notifications. Client needs to check data content.
        // Could define WsContentType.REACTION_UPDATE for specific handling.
        NotifyDto<Map<String, Object>> notifyDto = new NotifyDto<>(WsContentType.NOTIFICATION.getCode(), reactionData);

        WebSocketService.WsContent wsContent = new WebSocketService.WsContent();
        wsContent.setType(WsContentType.NOTIFICATION.getCode());
        wsContent.setContent(notifyDto);
        String wsContentJson = JSONUtil.toJsonStr(wsContent);

        if (MessageSource.Group.equals(reactedMessage.getSource())) {
            ChatList chatListForGroup = chatListMapper.selectById(reactedMessage.getChatId());
            if (chatListForGroup != null && ChatListType.GROUP.getCode().equals(chatListForGroup.getType()) && chatListForGroup.getGroupId() != null) {
                List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(chatListForGroup.getGroupId());
                if (memberIds != null && !memberIds.isEmpty()) {
                    for (Long memberId : memberIds) {
                        RedisBroadcastMsg broadcastMsg = RedisBroadcastMsg.builder().targetUserId(memberId).messageBody(wsContentJson).build();
                        redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, broadcastMsg);
                    }
                    log.info("Published reaction notification for messageId {} to group members of groupId {}", reactionVo.getMessageId(), chatListForGroup.getGroupId());
                }
            }
        } else { // Private chat
            ChatList privateChat = chatListMapper.selectById(reactedMessage.getChatId());
            if (privateChat != null) {
                Long user1 = privateChat.getUserId();
                Long user2 = privateChat.getTargetId();

                RedisBroadcastMsg toUser1Msg = RedisBroadcastMsg.builder().targetUserId(user1).messageBody(wsContentJson).build();
                redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, toUser1Msg);
                if (!user1.equals(user2)) {
                    RedisBroadcastMsg toUser2Msg = RedisBroadcastMsg.builder().targetUserId(user2).messageBody(wsContentJson).build();
                    redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, toUser2Msg);
                }
                log.info("Published reaction notification for messageId {} to participants of private chat {}", reactionVo.getMessageId(), reactedMessage.getChatId());
            }
        }
    }

    @Async
    public void indexMessage(Message message) { // Should be public for Spring AOP to proxy @Async
        if (message == null) {
            log.warn("Attempted to index a null message.");
            return;
        }
        try {
            MessageDocument doc = new MessageDocument();
            doc.setId(message.getId());
            doc.setFromId(message.getSenderId()); // Corrected fromId mapping
            doc.setChatListId(message.getChatId()); // Corrected chatListId mapping

            if (message.getContent() != null && message.getContent().getContentType() == com.web.constant.TextContentType.TEXT.getCode()) {
               doc.setContent(message.getContent().getContent());
            } else if (message.getContent() != null && message.getContent().getContentType() == com.web.constant.TextContentType.FILE.getCode()) {
               doc.setContent(message.getContent().getContent()); // Indexing file description
            } else {
               if (message.getContent() != null) {
                   doc.setContent(message.getContent().getContent());
               } else {
                   doc.setContent(""); // Fallback for null content
               }
            }

            doc.setSendTime(message.getCreatedAt());

            // 只有在Elasticsearch可用时才保存
            if (messageSearchRepository != null) {
                messageSearchRepository.save(doc);
                log.debug("Message {} indexed to Elasticsearch", message.getId());
            } else {
                log.debug("Elasticsearch is disabled, skipping message indexing for message {}", message.getId());
            }
        } catch (Exception e) {
            log.error("Failed to index message {} to Elasticsearch: {}", message.getId(), e.getMessage(), e);
        }
    }
}
