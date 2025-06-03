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

import javax.annotation.Resource;
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

/**
 * 消息服务实现类，处理消息的发送、记录获取和撤回操作
 */
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

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
        message.setMsgType(MessageType.Recall); // 设置为 "Recall" 类型的字符串
        message.setMsgContent("");
        message.setIsRecalled(1);
        message.setUpdatedAt(now);
        updateById(message);

        // 发送更新后的消息到相应的聊天对象
        if (MessageSource.Group.equals(message.getSource())) {
            chatListService.updateGroupChatLastMessage(message);
            webSocketService.sendMsgToGroup(message);
        } else {
            chatListService.updatePrivateChatLastMessage(userId, message.getChatId(), message);
            webSocketService.sendMsgToUser(message, userId, message.getChatId());
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
        Message message = sendMessage(userId, messageBody, MessageSource.Group); // This saves the message

        // --- Start of Group Message Routing Logic ---
        if (message != null && message.getChatId() != null) {
            // message.getChatId() in this context is the ID of the ChatList entry for the group chat.
            ChatList chatListForGroup = chatListMapper.selectById(message.getChatId());

            if (chatListForGroup != null && ChatListType.GROUP.getCode().equals(chatListForGroup.getType())) {
                Long actualGroupId = chatListForGroup.getGroupId(); // Use the new groupId field from ChatList
                if (actualGroupId != null) {
                    List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(actualGroupId); // Returns List<Long>

                    log.info("Routing group messageId: {} to members: {} of groupId: {}", message.getId(), memberIds, actualGroupId);
                    // TODO: Replace existing webSocketService.sendMsgToGroup(message) with a method
                    // that takes memberIds and the message, e.g.,
                    // webSocketService.sendMessageToSpecificUsers(memberIds, message);
                    // For now, retain original call but log the members.
                    webSocketService.sendMsgToGroup(message); // Original call
                    log.warn("WebSocket call in sendMessageToGroup needs to be updated to use fetched memberIds if direct fan-out is intended here.");

                } else {
                    log.error("ChatList entry for group chat (id: {}) has a null groupId field.", chatListForGroup.getId());
                    // Fallback to original behavior if groupId is missing in ChatList
                    webSocketService.sendMsgToGroup(message);
                }
            } else {
                log.error("Could not find ChatList or it's not a GROUP type for chatId: {} from messageId: {}", message.getChatId(), message.getId());
                // Fallback or error handling if chatList is not found or not a group.
                webSocketService.sendMsgToGroup(message);
            }
        } else {
            log.error("Message object or its chatId is null after saving. Cannot route group message for messageBody with chatId: {}", messageBody.getChatId());
             // If message is null but messageBody is not, consider logging messageBody details if helpful
            if (message == null && messageBody != null) {
                log.error("Message save operation may have failed for senderId: {}, targetChatId: {}", userId, messageBody.getChatId());
            }
        }
        // --- End of Group Message Routing Logic ---

        // The original call to updateGroupChatLastMessage should still be here if message is not null
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

        StringBuilder sb = new StringBuilder();
        // 用于保存机器人回复相关信息
        AtomicReference<User> botUserRef = new AtomicReference<>(null);

        if (MessageType.Text.equals(messageBody.getMsgType())) {
            String msgContent = messageBody.getMsgContent();
            // 如果消息内容不是以 '[' 开头，说明格式不正确，则尝试包装为 JSON 数组
            if (!msgContent.trim().startsWith("[")) {
                msgContent = "[" + JSONUtil.toJsonStr(msgContent) + "]";
            }
            // 解析 msgContent 为 TextMessageContent 列表
            List<TextMessageContent> contents = JSONUtil.toList(msgContent, TextMessageContent.class);
            for (TextMessageContent content : contents) {
                if (TextContentType.Text.equals(content.getType())) {
                    // 替换敏感词
                    String sanitizedContent = sensitiveWordBs.replace(content.getContent());
                    content.setContent(sanitizedContent);
                    sb.append(sanitizedContent);
                } else {
                    // 解析用户类型
                    User user = JSONUtil.toBean(content.getContent(), User.class);
                    if (UserType.Bot.equals(user.getType())) { // 使用 User.getType()
                        botUserRef.set(user);
                    }
                }
            }
            // 将内容重新转换为 JSON 字符串，确保格式正确
            messageBody.setMsgContent(JSONUtil.toJsonStr(contents));
        }

        // 获取发送者的详细信息
        User user = authService.getUserByIdForTalk(userId); // 确保该方法存在并正确实现
        if (user != null) {
            user.setIpOwnership(IpUtil.getIpRegion(messageBody.getUserIp()));
            // 如需在消息中存储发送者信息，可在 Message 类中添加相关字段
        }

        // 判断是否需要显示时间
        if (previousMessage == null) {
            // 第一条消息，显示时间
            messageBody.setIsShowTime(1);
        } else {
            // 检查与上一条消息的时间差是否超过5分钟
            long minutesDifference = DateUtil.between(previousMessage.getCreatedAt(), messageBody.getCreatedAt(), DateUnit.MINUTE);
            if (minutesDifference > 5) {
                messageBody.setIsShowTime(1);
            } else {
                messageBody.setIsShowTime(0);
            }
        }

        // 处理参考消息
        if (messageBody.getReferenceMsgId() != null) {
            Message referenceMessage = getById(messageBody.getReferenceMsgId());
            if (referenceMessage != null) {
                messageBody.setReferenceMsgId(referenceMessage.getId());
            }
        }

        // 保存消息到数据库
        boolean saveResult = save(messageBody);
        if (saveResult) {
            // 如果存在 @机器人回复需求
            User botUser = botUserRef.get();
            if (botUser != null) {
                aiChatService.sendBotReply(userId, messageBody.getChatId(), botUser, sb.toString());
            }
            return messageBody;
        }

        // 如果保存失败，返回 null
        return null;
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
                .eq("emoji", reactionVo.getEmoji())
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
            newReaction.setUserId(userId.intValue()); // Cast Long to Integer for MessageReaction.userId
            newReaction.setEmoji(reactionVo.getEmoji());
            newReaction.setCreateTime(new Date()); // Set current time for creation
            messageReactionMapper.insert(newReaction);
            log.info("Reaction added for messageId: {}, userId: {}, emoji: {}",
                reactionVo.getMessageId(), userId, reactionVo.getEmoji());
        }

        // TODO: Through WebSocket, notify all users in the session that the reactions for this message have been updated.
        // webSocketService.sendReactionUpdate(reactionVo.getMessageId(), updatedReactionData);
        log.warn("WebSocket notification for reaction update is a TODO.");
    }
}
