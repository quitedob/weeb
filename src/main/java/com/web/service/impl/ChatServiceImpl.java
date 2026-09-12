package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.ChatListMapper;
import com.web.mapper.MessageMapper;
import com.web.model.ChatList;
import com.web.model.Message;
import com.web.service.ChatService;
import com.web.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天服务实现类
 * 实现聊天相关的核心业务逻辑
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatListMapper chatListMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private com.web.service.MessageBroadcastService messageBroadcastService;

    @Autowired
    private com.web.service.ChatUnreadCountService chatUnreadCountService;

    @Autowired
    private com.web.mapper.UserMapper userMapper;

    @Autowired
    private com.web.service.ChatAccessService chatAccessService;

    @Autowired
    private com.web.service.UserPreferencesService preferencesService;

    @Autowired
    private com.web.service.MessageOutboxService messageOutboxService;

    @Override
    public List<ChatList> getChatList(Long userId) {
        // 输入验证
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("无效的用户ID");
        }

        // 获取用户的所有聊天会话
        List<ChatList> chatLists = chatListMapper.selectChatListByUserId(userId);

        java.util.Map<Long, ChatList> uniqueChats = new java.util.LinkedHashMap<>();
        for (ChatList chat : chatLists) {
            Long sharedChatId = chat.getSharedChatId();
            try {
                chatAccessService.requireAccess(userId, sharedChatId);
            } catch (org.springframework.security.access.AccessDeniedException denied) {
                continue;
            }
            ChatList existing = uniqueChats.get(sharedChatId);
            if (existing == null || (chat.getUpdateTime() != null && (existing.getUpdateTime() == null
                    || chat.getUpdateTime().isAfter(existing.getUpdateTime())))) {
                uniqueChats.put(sharedChatId, chat);
            }
        }
        for (ChatList chat : uniqueChats.values()) {
            Message lastMessage = messageMapper.selectLastMessageByChatId(chat.getSharedChatId());
            chat.setLastMessage(lastMessage != null && lastMessage.getContent() != null
                    ? lastMessage.getContent().getContent() : "");
            chat.setUnreadCount(chatUnreadCountService.getUnreadCount(userId, chat.getSharedChatId()));
        }

        return new java.util.ArrayList<>(uniqueChats.values());
    }

    @Override
    public ChatList createChat(Long userId, Long targetId) {
        // 输入验证
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("无效的用户ID");
        }
        if (!ValidationUtils.validateId(targetId, "目标用户ID")) {
            throw new WeebException("无效的目标用户ID");
        }
        if (userId.equals(targetId)) {
            throw new WeebException("不能与自己创建聊天");
        }

        // ✅ 修复1：查找或创建共享聊天ID（确保双方使用同一个sharedChatId）
        Long sharedChatId = findOrCreateSharedChatId(userId, targetId);

        // ✅ 修复1：检查当前用户是否已有该聊天
        ChatList existingChat = chatListMapper.selectChatListByUserAndTarget(userId, targetId);
        if (existingChat != null) {
            // 确保现有聊天记录有正确的sharedChatId
            if (existingChat.getSharedChatId() == null || !existingChat.getSharedChatId().equals(sharedChatId)) {
                existingChat.setSharedChatId(sharedChatId);
                chatListMapper.updateChatListById(existingChat);
            }
            return existingChat;
        }

        // ✅ 修复：获取目标用户信息以动态生成targetInfo
        com.web.model.User targetUser = userMapper.selectById(targetId);
        String targetUserInfo = "{\"id\":" + targetId + ",\"username\":\"Unknown\",\"name\":\"Unknown\"}";
        if (targetUser != null) {
            String displayName = targetUser.getNickname() != null ? targetUser.getNickname() : targetUser.getUsername();
            targetUserInfo = "{\"id\":" + targetId + ",\"username\":\"" + targetUser.getUsername() + "\",\"name\":\"" + displayName + "\",\"avatar\":\"" + (targetUser.getAvatar() != null ? targetUser.getAvatar() : "") + "\"}";
        }

        // ✅ 修复：为当前用户创建chat_list记录（使用sharedChatId作为ID的一部分）
        ChatList userChatList = new ChatList();
        userChatList.setId(String.valueOf(sharedChatId) + "_" + userId); // 使用 sharedChatId_userId 格式
        userChatList.setUserId(userId);
        userChatList.setSharedChatId(sharedChatId);
        userChatList.setTargetId(targetId);
        userChatList.setType("PRIVATE");
        userChatList.setTargetInfo(targetUserInfo);
        userChatList.setUnreadCount(0);

        chatListMapper.insertChatList(userChatList);

        // ✅ 修复：获取当前用户信息以生成对方的targetInfo
        com.web.model.User currentUser = userMapper.selectById(userId);
        String currentUserInfo = "{\"id\":" + userId + ",\"username\":\"Unknown\",\"name\":\"Unknown\"}";
        if (currentUser != null) {
            String displayName = currentUser.getNickname() != null ? currentUser.getNickname() : currentUser.getUsername();
            currentUserInfo = "{\"id\":" + userId + ",\"username\":\"" + currentUser.getUsername() + "\",\"name\":\"" + displayName + "\",\"avatar\":\"" + (currentUser.getAvatar() != null ? currentUser.getAvatar() : "") + "\"}";
        }

        // ✅ 修复：检查对方是否已有chat_list记录，如果没有则创建（使用相同的sharedChatId）
        ChatList targetChatList = chatListMapper.selectChatListByUserAndTarget(targetId, userId);
        if (targetChatList == null) {
            targetChatList = new ChatList();
            targetChatList.setId(String.valueOf(sharedChatId) + "_" + targetId); // 使用 sharedChatId_targetId 格式
            targetChatList.setUserId(targetId);
            targetChatList.setSharedChatId(sharedChatId); // ✅ 关键：使用相同的sharedChatId
            targetChatList.setTargetId(userId);
            targetChatList.setType("PRIVATE");
            targetChatList.setTargetInfo(currentUserInfo);
            targetChatList.setUnreadCount(0);

            chatListMapper.insertChatList(targetChatList);
        } else if (targetChatList.getSharedChatId() == null || !targetChatList.getSharedChatId().equals(sharedChatId)) {
            // 确保对方的聊天记录也有正确的sharedChatId
            targetChatList.setSharedChatId(sharedChatId);
            chatListMapper.updateChatListById(targetChatList);
        }

        return userChatList;
    }

    /**
     * 查找或创建共享聊天ID
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @return 共享聊天ID
     */
    private Long findOrCreateSharedChatId(Long userId1, Long userId2) {
        // 确保participant_1_id < participant_2_id（用于唯一约束）
        Long participant1 = Math.min(userId1, userId2);
        Long participant2 = Math.max(userId1, userId2);

        // 查找现有的共享聊天
        Long existingSharedChatId = chatListMapper.findSharedChatId(participant1, participant2);
        if (existingSharedChatId != null) {
            return existingSharedChatId;
        }

        chatListMapper.createSharedChat(participant1, participant2, "PRIVATE");
        
        // 再次查询获取刚创建的ID
        return chatListMapper.findSharedChatId(participant1, participant2);
    }

    @Override
    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<Message> getChatMessages(Long userId, String chatId, Integer page, Integer size) {
        // 输入验证
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new WeebException("无效的聊天ID");
        }
        if (!ValidationUtils.validatePageParams(page, size, "消息查询")) {
            throw new WeebException("无效的分页参数");
        }

        // ✅ 新架构：获取当前聊天的信息
        // 注意：selectChatListById需要Long类型，但chat_list.id是VARCHAR，需要特殊处理
        ChatList currentChat = chatAccessService.requireOwnedChat(userId, chatId);
        if (currentChat == null) {
            throw new WeebException("聊天会话不存在");
        }

        // ✅ 新架构：使用sharedChatId查询消息
        Long sharedChatId = currentChat.getSharedChatId();
        if (sharedChatId == null) {
            throw new WeebException("聊天会话配置错误：缺少共享聊天ID");
        }

        int offset = (page - 1) * size;
        return populateMessageReactions(messageMapper.selectMessagesBySharedChatId(sharedChatId, offset, size));
    }

    @Override
    public Message sendMessage(Long userId, String chatId, Message message) {
        ChatList chat = chatAccessService.requireOwnedChat(userId, chatId);
        return sendMessageBySharedChatId(userId, chat.getSharedChatId(), message);
    }

    @Override
    public boolean markAsRead(Long userId, String chatId) {
        ChatList chat = chatAccessService.requireOwnedChat(userId, chatId);
        return markAsReadBySharedChatId(userId, chat.getSharedChatId());
    }

    @Override
    public boolean deleteChat(Long userId, String chatId) {
        // ✅ 使用String类型的chatId查询聊天信息
        ChatList chatList = chatListMapper.selectChatListByIdString(chatId);
        if (chatList == null || !chatList.getUserId().equals(userId)) {
            return false;
        }

        // 删除聊天会话
        return chatListMapper.deleteChatList(chatId) > 0;
    }

    @Autowired
    private com.web.mapper.MessageReactionMapper messageReactionMapper;

    private List<Message> populateMessageReactions(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return messages;
        List<Long> messageIds = messages.stream().map(Message::getId).filter(java.util.Objects::nonNull).distinct().toList();
        java.util.Map<Long, List<java.util.Map<String, Object>>> summaries = reactionSummaries(messageIds);
        for (Message message : messages) {
            message.setReactions(summaries.getOrDefault(message.getId(), List.of()));
        }
        return messages;
    }

    /** Load each page in one query and avoid truncating user IDs with GROUP_CONCAT. */
    private java.util.Map<Long, List<java.util.Map<String, Object>>> reactionSummaries(List<Long> messageIds) {
        java.util.Map<Long, List<java.util.Map<String, Object>>> summaries = new java.util.HashMap<>();
        if (messageIds.isEmpty()) return summaries;
        java.util.Map<Long, java.util.Map<String, java.util.Set<Long>>> grouped = new java.util.LinkedHashMap<>();
        for (com.web.model.MessageReaction reaction : messageReactionMapper.selectByMessageIds(messageIds)) {
            grouped.computeIfAbsent(reaction.getMessageId(), key -> new java.util.LinkedHashMap<>())
                    .computeIfAbsent(reaction.getReactionType(), key -> new java.util.LinkedHashSet<>())
                    .add(reaction.getUserId());
        }
        grouped.forEach((messageId, reactions) -> {
            List<java.util.Map<String, Object>> messageSummaries = new java.util.ArrayList<>();
            reactions.forEach((emoji, userIds) -> messageSummaries.add(java.util.Map.of(
                    "emoji", emoji, "reactionType", emoji, "count", userIds.size(), "userIds", List.copyOf(userIds))));
            summaries.put(messageId, messageSummaries);
        });
        return summaries;
    }

    @Override
    public void addReaction(Long userId, Long messageId, String reactionType) {
        changeReaction(userId, messageId, reactionType, null);
    }

    @Override
    public java.util.Map<String, Object> setReaction(Long userId, Long messageId, String reactionType, boolean present) {
        return changeReaction(userId, messageId, reactionType, present);
    }

    private java.util.Map<String, Object> changeReaction(Long userId, Long messageId, String reactionType, Boolean present) {
        if (reactionType == null || reactionType.isBlank()
                || reactionType.codePointCount(0, reactionType.length()) > 50) {
            throw new IllegalArgumentException("Invalid reaction");
        }
        reactionType = reactionType.trim();
        Message observed = messageMapper.selectMessageById(messageId);
        if (observed == null) throw new IllegalArgumentException("Message unavailable");
        chatAccessService.requireAccess(userId, observed.getChatId());
        lockConversation(observed.getChatId());
        Message message = messageMapper.selectMessageForUpdate(messageId);
        if (message == null || Integer.valueOf(1).equals(message.getIsRecalled())) {
            throw new IllegalArgumentException("Message unavailable");
        }
        com.web.model.MessageReaction existing = messageReactionMapper.findByMessageUserAndType(messageId, userId, reactionType);
        boolean desired = present == null ? existing == null : present;
        boolean changed = desired != (existing != null);
        if (changed) {
            if (desired) {
                com.web.model.MessageReaction reaction = new com.web.model.MessageReaction();
                reaction.setMessageId(messageId);
                reaction.setUserId(userId);
                reaction.setReactionType(reactionType);
                reaction.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                messageReactionMapper.insert(reaction);
            } else {
                messageReactionMapper.deleteByMessageUserAndType(messageId, userId, reactionType);
            }
            messageMapper.incrementReactionVersion(messageId);
            message.setReactionVersion((message.getReactionVersion() == null ? 0 : message.getReactionVersion()) + 1);
        }
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("type", "MESSAGE_REACTION_CHANGE");
        result.put("messageId", messageId);
        result.put("chatId", message.getChatId());
        result.put("sharedChatId", message.getChatId());
        result.put("userId", userId);
        result.put("reactionType", reactionType);
        result.put("action", desired ? "add" : "remove");
        result.put("reactionVersion", message.getReactionVersion() == null ? 0L : message.getReactionVersion());
        result.put("reactions", reactionSummaries(List.of(messageId)).getOrDefault(messageId, List.of()));
        if (changed) messageOutboxService.enqueue("REACTION:" + messageId + ":" + result.get("reactionVersion"),
                "REACTION", message.getChatId(), messageId, messageMapper.selectCurrentRecipients(message.getChatId()), result);
        return result;
    }

    @Override
    public boolean recallMessage(Long userId, Long messageId) {
        // 检查消息是否存在
        Message message = messageMapper.selectMessageById(messageId);
        if (message == null) {
            throw new WeebException("消息不存在");
        }
        chatAccessService.requireAccess(userId, message.getChatId());

        // 验证消息是否属于当前用户
        if (!message.getSenderId().equals(userId)) {
            throw new WeebException("无权撤回他人消息");
        }

        // 检查消息是否已被撤回
        if (message.getIsRecalled() != null && message.getIsRecalled() == 1) {
            throw new WeebException("消息已被撤回");
        }

        // ✅ 检查消息发送时间，超过5分钟不允许撤回
        long currentTime = System.currentTimeMillis();
        long messageTime = message.getCreatedAt().getTime();
        long timeDiff = currentTime - messageTime;
        long fiveMinutesInMillis = 5 * 60 * 1000; // 5分钟 = 300秒 = 300000毫秒

        if (timeDiff > fiveMinutesInMillis) {
            throw new WeebException("消息发送超过5分钟，无法撤回");
        }

        // 标记消息为已撤回
        return messageMapper.markMessageAsRecalled(messageId) > 0;
    }

    @Override
    public Long findOrCreatePrivateChat(Long userId, Long targetUserId) {
        // 参数验证
        if (userId == null || targetUserId == null) {
            throw new WeebException("用户ID不能为空");
        }

        if (userId.equals(targetUserId)) {
            throw new WeebException("不能与自己创建私聊");
        }

        // ✅ 修复：使用新架构的sharedChatId系统
        // 查找或创建共享聊天ID
        Long sharedChatId = findOrCreateSharedChatId(userId, targetUserId);

        // 查找现有的私聊会话
        ChatList existingChat = chatListMapper.selectChatListByUserAndTarget(userId, targetUserId);
        if (existingChat != null) {
            // 确保现有聊天记录有正确的sharedChatId
            if (existingChat.getSharedChatId() == null || !existingChat.getSharedChatId().equals(sharedChatId)) {
                existingChat.setSharedChatId(sharedChatId);
                chatListMapper.updateChatListById(existingChat);
            }
            return sharedChatId; // ✅ 返回sharedChatId而不是UUID字符串
        }

        // 创建新的私聊会话
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        ChatList newChat = new ChatList();
        newChat.setId(String.valueOf(sharedChatId) + "_" + userId); // ✅ 使用 sharedChatId_userId 格式
        newChat.setUserId(userId);
        newChat.setSharedChatId(sharedChatId); // ✅ 设置sharedChatId
        newChat.setTargetId(targetUserId);
        newChat.setType("PRIVATE");
        newChat.setTargetInfo("Private Chat");
        newChat.setUnreadCount(0);
        newChat.setCreateTime(now);
        newChat.setUpdateTime(now);

        chatListMapper.insertChatList(newChat);

        // 为目标用户也创建一个对应的聊天会话记录
        ChatList targetChat = new ChatList();
        targetChat.setId(String.valueOf(sharedChatId) + "_" + targetUserId); // ✅ 使用 sharedChatId_targetUserId 格式
        targetChat.setUserId(targetUserId);
        targetChat.setSharedChatId(sharedChatId); // ✅ 设置相同的sharedChatId
        targetChat.setTargetId(userId);
        targetChat.setType("PRIVATE");
        targetChat.setTargetInfo("Private Chat");
        targetChat.setUnreadCount(0);
        targetChat.setCreateTime(now);
        targetChat.setUpdateTime(now);

        chatListMapper.insertChatList(targetChat);

        return sharedChatId; // ✅ 返回sharedChatId（Long类型）而不是UUID字符串
    }

    // ==================== ✅ 新增：基于sharedChatId的方法实现 ====================

    @Override
    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<Message> getChatMessagesBySharedChatId(Long userId, Long sharedChatId, Integer page, Integer size) {
        // 输入验证
        if (!ValidationUtils.validateId(sharedChatId, "共享聊天ID")) {
            throw new WeebException("无效的共享聊天ID");
        }
        if (!ValidationUtils.validatePageParams(page, size, "消息查询")) {
            throw new WeebException("无效的分页参数");
        }

        chatAccessService.requireAccess(userId, sharedChatId);
        int offset = (page - 1) * size;
        return populateMessageReactions(messageMapper.selectMessagesBySharedChatId(sharedChatId, offset, size));
    }

    @Override
    public Message sendMessageBySharedChatId(Long userId, Long sharedChatId, Message message) {
        chatAccessService.requireAccess(userId, sharedChatId);
        if (message == null || !ValidationUtils.validateMessageType(message.getMessageType())
                || message.getContent() == null || !ValidationUtils.validateChatMessage(message.getContent().getContent())) {
            throw new IllegalArgumentException("Invalid message content or type");
        }
        String clientId = message.getClientMessageId();
        if (clientId != null && (clientId.isBlank() || clientId.length() > 100)) {
            throw new IllegalArgumentException("Client message ID must contain 1 to 100 characters");
        }
        lockConversation(sharedChatId);
        ChatList senderChat = chatListMapper.selectChatListByUserIdAndSharedChatId(userId, sharedChatId);
        if (senderChat == null) {
            com.web.model.Group group = chatListMapper.selectGroupBySharedChatId(sharedChatId);
            if (group == null) throw new org.springframework.security.access.AccessDeniedException("Conversation unavailable");
            senderChat = createChatListForGroupMember(userId, group);
        }
        message.setReceiverId("PRIVATE".equals(senderChat.getType()) ? senderChat.getTargetId() : null);
        requireMessagesAllowed(message.getReceiverId());
        message.setGroupId("GROUP".equals(senderChat.getType()) ? senderChat.getGroupId() : null);
        validateReferencedMessage(message.getReplyToMessageId(), sharedChatId);
        if (message.getThreadId() != null) throw new WeebException("Message threads are not enabled");
        message.setSenderId(userId);
        message.setChatId(sharedChatId);
        if (message.getContent().getAtUidList() == null) message.getContent().setAtUidList(List.of());
        if (clientId != null) {
            Message existing = messageMapper.selectBySenderAndClientIdForUpdate(userId, clientId);
            if (existing != null) return requireSameMessage(existing, message);
        }
        message.setId(null);
        message.setStatus(Message.STATUS_SENT);
        message.setIsRecalled(0);
        message.setReadStatus(0);
        message.setIsRead(0);
        message.setReactionVersion(0L);
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        try {
            messageMapper.insertMessage(message);
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            Message existing = clientId == null ? null : messageMapper.selectBySenderAndClientIdForUpdate(userId, clientId);
            if (existing == null) throw duplicate;
            return requireSameMessage(existing, message);
        }
        chatListMapper.updateLastMessage(senderChat.getId(), message.getContent().getContent());
        if (message.getReceiverId() != null
                && chatListMapper.selectChatListByUserAndTarget(message.getReceiverId(), userId) == null) {
            ChatList receiver = new ChatList();
            receiver.setId(sharedChatId + "_" + message.getReceiverId());
            receiver.setUserId(message.getReceiverId());
            receiver.setSharedChatId(sharedChatId);
            receiver.setTargetId(userId);
            receiver.setType("PRIVATE");
            receiver.setTargetInfo("Private Chat");
            receiver.setUnreadCount(0);
            chatListMapper.insertChatList(receiver);
        }
        List<Long> recipients = messageMapper.selectCurrentRecipients(sharedChatId);
        for (Long recipient : recipients) {
            if (recipient != null && !recipient.equals(userId)) {
                chatUnreadCountService.incrementUnreadCount(recipient, sharedChatId, 1);
            }
        }
        messageOutboxService.enqueueMessage(message, recipients);
        return message;
    }

    private Message requireSameMessage(Message existing, Message requested) {
        if (!java.util.Objects.equals(existing.getChatId(), requested.getChatId())
                || !java.util.Objects.equals(existing.getMessageType(), requested.getMessageType())
                || !java.util.Objects.equals(existing.getContent(), requested.getContent())
                || !java.util.Objects.equals(existing.getReplyToMessageId(), requested.getReplyToMessageId())) {
            throw new IllegalArgumentException("Client message ID already used for different content or conversation");
        }
        return existing;
    }

    private void lockConversation(Long sharedChatId) {
        if (messageMapper.lockSharedChat(sharedChatId) == null) {
            throw new org.springframework.security.access.AccessDeniedException("Conversation unavailable");
        }
    }

    @Override
    public boolean markAsReadBySharedChatId(Long userId, Long sharedChatId) {
        markAsReadBySharedChatId(userId, sharedChatId, null);
        return true;
    }

    @Override
    public java.util.Map<String, Object> markAsReadBySharedChatId(Long userId, Long sharedChatId, Long lastReadMessageId) {
        chatAccessService.requireAccess(userId, sharedChatId);
        lockConversation(sharedChatId);
        Long boundary = lastReadMessageId;
        if (boundary == null) boundary = messageMapper.selectLatestMessageId(sharedChatId);
        if (boundary == null) boundary = 0L;
        if (boundary < 0) throw new IllegalArgumentException("Invalid read boundary");
        if (boundary > 0) {
            Message lastRead = messageMapper.selectMessageForUpdate(boundary);
            if (lastRead == null || !sharedChatId.equals(lastRead.getChatId())) {
                throw new org.springframework.security.access.AccessDeniedException("Read boundary is outside this conversation");
            }
        }
        long actualBoundary = chatUnreadCountService.markThrough(userId, sharedChatId, boundary);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("chatId", sharedChatId);
        result.put("sharedChatId", sharedChatId);
        result.put("messageId", actualBoundary);
        result.put("lastReadMessageId", actualBoundary);
        result.put("readerId", userId);
        result.put("status", Message.STATUS_READ);
        result.put("unreadCount", chatUnreadCountService.getUnreadCount(userId, sharedChatId));
        if (actualBoundary > 0) messageOutboxService.enqueue("READ:" + userId + ":" + actualBoundary,
                "READ", sharedChatId, actualBoundary, messageMapper.selectCurrentRecipients(sharedChatId), result);
        return result;
    }

    @Override
    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public java.util.Map<String, Object> syncMessages(Long userId, Long sharedChatId, Long afterMessageId, int size) {
        chatAccessService.requireAccess(userId, sharedChatId);
        if (afterMessageId == null || afterMessageId < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid message synchronization cursor or size");
        }
        List<Message> fetched = messageMapper.selectMessagesAfter(sharedChatId, afterMessageId, size + 1);
        boolean more = fetched.size() > size;
        List<Message> page = more ? new java.util.ArrayList<>(fetched.subList(0, size)) : fetched;
        populateMessageReactions(page);
        long next = page.isEmpty() ? afterMessageId : page.get(page.size() - 1).getId();
        return java.util.Map.of("list", page, "nextAfterMessageId", next, "hasMore", more);
    }

    @Override
    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<Message> getMessageStates(Long userId, Long sharedChatId, List<Long> messageIds) {
        chatAccessService.requireAccess(userId, sharedChatId);
        if (messageIds == null || messageIds.isEmpty() || messageIds.size() > 100
                || messageIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Request between 1 and 100 valid message IDs");
        }
        return populateMessageReactions(messageMapper.selectMessageStates(sharedChatId, messageIds.stream().distinct().toList()));
    }

    /**
     * 根据sharedChatId查找群组
     */
    private com.web.model.Group findGroupBySharedChatId(Long sharedChatId) {
        try {
            return chatListMapper.selectGroupBySharedChatId(sharedChatId);
        } catch (Exception e) {
            log.debug("未找到对应的群组: sharedChatId={}", sharedChatId);
            return null;
        }
    }
    
    /**
     * 根据groupId查找群组
     */
    private com.web.model.Group findGroupById(Long groupId) {
        try {
            return chatListMapper.selectGroupById(groupId);
        } catch (Exception e) {
            log.debug("未找到对应的群组: groupId={}", groupId);
            return null;
        }
    }
    
    /**
     * 检查用户是否是群成员
     */
    private boolean isUserGroupMember(Long userId, Long groupId) {
        try {
            return chatListMapper.isUserGroupMember(userId, groupId);
        } catch (Exception e) {
            log.error("检查群成员失败: userId={}, groupId={}", userId, groupId, e);
            return false;
        }
    }
    
    /**
     * 为群成员创建chat_list记录
     */
    private ChatList createChatListForGroupMember(Long userId, com.web.model.Group group) {
        ChatList chatList = new ChatList();
        chatList.setId(java.util.UUID.randomUUID().toString());
        chatList.setUserId(userId);
        chatList.setSharedChatId(group.getSharedChatId());
        chatList.setGroupId(group.getId());
        chatList.setType("GROUP");
        chatList.setTargetInfo(group.getGroupName());
        chatList.setUnreadCount(0);
        chatList.setCreateTime(java.time.LocalDateTime.now());
        chatList.setUpdateTime(java.time.LocalDateTime.now());
        
        chatListMapper.insertChatList(chatList);
        return chatList;
    }

    @Override
    public boolean deleteChatBySharedChatId(Long userId, Long sharedChatId) {
        // 输入验证
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("无效的用户ID");
        }
        if (!ValidationUtils.validateId(sharedChatId, "共享聊天ID")) {
            throw new WeebException("无效的共享聊天ID");
        }

        // 查找用户的chat_list记录
        ChatList chatList = chatListMapper.selectChatListByUserIdAndSharedChatId(userId, sharedChatId);
        if (chatList == null || !chatList.getUserId().equals(userId)) {
            return false;
        }

        // 删除聊天会话
        return chatListMapper.deleteChatList(chatList.getId()) > 0;
    }
    private void requireMessagesAllowed(Long recipientId) {
        if (recipientId != null && !preferencesService.canReceiveMessages(recipientId)) {
            throw new org.springframework.security.access.AccessDeniedException("该用户已关闭私信");
        }
    }

    private void validateReferencedMessage(Long messageId, Long sharedChatId) {
        if (messageId == null) {
            return;
        }
        Message referenced = messageMapper.selectMessageById(messageId);
        if (referenced == null || !sharedChatId.equals(referenced.getChatId())) {
            throw new org.springframework.security.access.AccessDeniedException("Referenced message is not in this conversation");
        }
    }

}

