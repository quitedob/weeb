package com.web.service.Impl;

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
@Transactional(rollbackFor = Exception.class)
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

    @Override
    public List<ChatList> getChatList(Long userId) {
        // 输入验证
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("无效的用户ID");
        }

        // 获取用户的所有聊天会话
        List<ChatList> chatLists = chatListMapper.selectChatListByUserId(userId);

        // ✅ 修复：去重处理，基于target_id避免显示同一个好友多次
        java.util.Map<Long, ChatList> uniqueChats = new java.util.LinkedHashMap<>();
        for (ChatList chatList : chatLists) {
            Long targetId = chatList.getTargetId();
            Long sharedChatId = chatList.getSharedChatId();
            
            if (targetId != null && !uniqueChats.containsKey(targetId)) {
                // 为每个聊天会话获取最后一条消息（使用sharedChatId而不是String类型的id）
                if (sharedChatId != null) {
                    Message lastMessage = messageMapper.selectLastMessageByChatId(sharedChatId);
                    chatList.setLastMessage(lastMessage != null ? lastMessage.getContent().toString() : "");
                }
                uniqueChats.put(targetId, chatList);
            } else if (targetId != null) {
                // 如果已存在该目标用户的聊天，保留最新的一个（基于update_time）
                ChatList existing = uniqueChats.get(targetId);
                if (chatList.getUpdateTime() != null && existing.getUpdateTime() != null &&
                    chatList.getUpdateTime().isAfter(existing.getUpdateTime())) {
                    if (sharedChatId != null) {
                        Message lastMessage = messageMapper.selectLastMessageByChatId(sharedChatId);
                        chatList.setLastMessage(lastMessage != null ? lastMessage.getContent().toString() : "");
                    }
                    uniqueChats.put(targetId, chatList);
                }
            }
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

        // 创建新的共享聊天记录
        // 使用Map传递参数以便MyBatis能正确设置返回的ID
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("participant1Id", participant1);
        params.put("participant2Id", participant2);
        params.put("chatType", "PRIVATE");
        
        chatListMapper.createSharedChat(participant1, participant2, "PRIVATE");
        
        // 再次查询获取刚创建的ID
        return chatListMapper.findSharedChatId(participant1, participant2);
    }

    @Override
    public List<Message> getChatMessages(String chatId, Integer page, Integer size) {
        // 输入验证
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new WeebException("无效的聊天ID");
        }
        if (!ValidationUtils.validatePageParams(page, size, "消息查询")) {
            throw new WeebException("无效的分页参数");
        }

        // ✅ 新架构：获取当前聊天的信息
        // 注意：selectChatListById需要Long类型，但chat_list.id是VARCHAR，需要特殊处理
        ChatList currentChat = chatListMapper.selectChatListByIdString(chatId);
        if (currentChat == null) {
            throw new WeebException("聊天会话不存在");
        }

        // ✅ 新架构：使用sharedChatId查询消息
        Long sharedChatId = currentChat.getSharedChatId();
        if (sharedChatId == null) {
            throw new WeebException("聊天会话配置错误：缺少共享聊天ID");
        }

        int offset = (page - 1) * size;
        return messageMapper.selectMessagesBySharedChatId(sharedChatId, offset, size);
    }

    @Override
    public Message sendMessage(Long userId, String chatId, Message message) {
        // 输入验证
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("无效的用户ID");
        }
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new WeebException("无效的聊天ID");
        }
        if (message == null) {
            throw new WeebException("消息内容不能为空");
        }
        if (!ValidationUtils.validateMessageType(message.getMessageType())) {
            throw new WeebException("消息类型无效");
        }

        // 验证消息内容
        if (message.getContent() != null && message.getContent().getContent() != null) {
            if (!ValidationUtils.validateChatMessage(message.getContent().getContent())) {
                throw new WeebException("消息内容无效");
            }
        }

        // ✅ 新架构：使用String类型的chatId查询聊天信息
        ChatList currentChat = chatListMapper.selectChatListByIdString(chatId);
        if (currentChat == null) {
            throw new WeebException("聊天会话不存在");
        }

        Long sharedChatId = currentChat.getSharedChatId();
        if (sharedChatId == null) {
            throw new WeebException("聊天会话配置错误：缺少共享聊天ID");
        }

        // ✅ 新架构：将消息的chatId设置为sharedChatId
        message.setChatId(sharedChatId);

        // 设置消息发送时间
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        message.setCreatedAt(now);
        message.setUpdatedAt(now);

        // 插入消息记录
        messageMapper.insertMessage(message);

        // ✅ 新架构：更新发送方的聊天列表
        chatListMapper.updateLastMessageAndUnreadCount(currentChat.getId(),
                message.getContent() != null ? message.getContent().getContent() : "");

        // ✅ 新架构：处理消息路由和未读计数
        if ("GROUP".equals(currentChat.getType())) {
            // 群聊：转发给所有群成员
            Long groupId = currentChat.getGroupId();
            if (groupId != null) {
                messageBroadcastService.broadcastMessageToGroup(message, groupId);
            }
        } else {
            // 私聊：确定接收者
            Long targetReceiverId = currentChat.getTargetId();

            if (targetReceiverId != null && !targetReceiverId.equals(userId)) {
                // 查找接收者的对应聊天记录
                ChatList receiverChatList = chatListMapper.selectChatListByUserAndTarget(targetReceiverId, userId);
                
                if (receiverChatList != null) {
                    // ✅ 更新接收者的聊天列表
                    chatListMapper.updateLastMessageAndUnreadCount(receiverChatList.getId(),
                            message.getContent() != null ? message.getContent().getContent() : "");
                    
                    // 增加接收者的未读计数
                    chatUnreadCountService.incrementUnreadCount(targetReceiverId, sharedChatId, 1);
                } else {
                    // ✅ 如果接收者没有聊天记录，自动创建一个（使用相同的sharedChatId）
                    ChatList newReceiverChat = new ChatList();
                    newReceiverChat.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                    newReceiverChat.setUserId(targetReceiverId);
                    newReceiverChat.setSharedChatId(sharedChatId);
                    newReceiverChat.setTargetId(userId);
                    newReceiverChat.setType("PRIVATE");
                    newReceiverChat.setTargetInfo("Private Chat");
                    newReceiverChat.setUnreadCount(1);
                    newReceiverChat.setLastMessage(message.getContent() != null ? message.getContent().getContent() : "");
                    chatListMapper.insertChatList(newReceiverChat);
                    
                    chatUnreadCountService.incrementUnreadCount(targetReceiverId, sharedChatId, 1);
                }

                // 转发消息给接收者
                messageBroadcastService.broadcastMessageToReceiver(message, targetReceiverId);
            }
        }

        return message;
    }

    @Override
    public boolean markAsRead(Long userId, String chatId) {
        // ✅ 使用String类型的chatId查询聊天信息
        ChatList chatList = chatListMapper.selectChatListByIdString(chatId);
        if (chatList == null) {
            throw new WeebException("聊天会话不存在");
        }
        
        Long sharedChatId = chatList.getSharedChatId();
        if (sharedChatId == null) {
            throw new WeebException("聊天会话配置错误：缺少共享聊天ID");
        }
        
        // 获取最后一条消息ID
        Message lastMessage = messageMapper.selectLastMessageByChatId(sharedChatId);
        Long lastMessageId = lastMessage != null ? lastMessage.getId() : null;
        
        // ✅ 使用新的未读计数服务
        chatUnreadCountService.markAsRead(userId, sharedChatId, lastMessageId);
        
        // 同时更新chat_list表
        return chatListMapper.resetUnreadCountByChatId(chatId) > 0;
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

    @Override
    public void addReaction(Long userId, Long messageId, String reactionType) {
        log.info("添加消息反应: userId={}, messageId={}, reactionType={}", userId, messageId, reactionType);
        
        // 输入验证
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("无效的用户ID");
        }
        if (!ValidationUtils.validateId(messageId, "消息ID")) {
            throw new WeebException("无效的消息ID");
        }
        if (reactionType == null || reactionType.trim().isEmpty()) {
            throw new WeebException("反应类型不能为空");
        }

        // 检查消息是否存在
        Message message = messageMapper.selectMessageById(messageId);
        if (message == null) {
            throw new WeebException("消息不存在");
        }

        // 检查用户是否已经对该消息添加了相同的反应
        com.web.model.MessageReaction existingReaction = messageReactionMapper.findByMessageUserAndType(
            messageId, userId, reactionType);

        if (existingReaction != null) {
            // 如果已存在，则删除（取消反应）
            messageReactionMapper.deleteByMessageUserAndType(messageId, userId, reactionType);
            log.info("取消消息反应: userId={}, messageId={}, reactionType={}", userId, messageId, reactionType);
        } else {
            // 如果不存在，则添加
            com.web.model.MessageReaction reaction = new com.web.model.MessageReaction();
            reaction.setMessageId(messageId);
            reaction.setUserId(userId);
            reaction.setReactionType(reactionType);
            reaction.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            
            messageReactionMapper.insert(reaction);
            log.info("添加消息反应成功: reactionId={}", reaction.getId());
        }

        // ✅ 关键：广播反应变更给所有相关用户
        try {
            // 获取消息的聊天ID
            Long chatId = message.getChatId();
            
            // 获取该消息的所有反应统计
            List<java.util.Map<String, Object>> reactionStats = messageReactionMapper.getReactionStatsByMessageId(messageId);
            
            // 构造广播数据
            java.util.Map<String, Object> reactionData = new java.util.HashMap<>();
            reactionData.put("messageId", messageId);
            reactionData.put("chatId", chatId);
            reactionData.put("userId", userId);
            reactionData.put("reactionType", reactionType);
            reactionData.put("action", existingReaction != null ? "remove" : "add");
            reactionData.put("reactions", reactionStats);
            
            // 广播反应变更
            messageBroadcastService.broadcastReactionChange(chatId, reactionData);
            
            log.info("广播消息反应变更成功: messageId={}, chatId={}", messageId, chatId);
        } catch (Exception e) {
            log.error("广播消息反应变更失败: messageId={}", messageId, e);
            // 不抛出异常，因为反应已经保存成功
        }
    }

    @Override
    public boolean recallMessage(Long userId, Long messageId) {
        // 检查消息是否存在
        Message message = messageMapper.selectMessageById(messageId);
        if (message == null) {
            throw new WeebException("消息不存在");
        }

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
    public List<Message> getChatMessagesBySharedChatId(Long sharedChatId, Integer page, Integer size) {
        // 输入验证
        if (!ValidationUtils.validateId(sharedChatId, "共享聊天ID")) {
            throw new WeebException("无效的共享聊天ID");
        }
        if (!ValidationUtils.validatePageParams(page, size, "消息查询")) {
            throw new WeebException("无效的分页参数");
        }

        int offset = (page - 1) * size;
        return messageMapper.selectMessagesBySharedChatId(sharedChatId, offset, size);
    }

    @Override
    public Message sendMessageBySharedChatId(Long userId, Long sharedChatId, Message message) {
        // 输入验证
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("无效的用户ID");
        }
        if (!ValidationUtils.validateId(sharedChatId, "共享聊天ID")) {
            throw new WeebException("无效的共享聊天ID");
        }
        if (message == null) {
            throw new WeebException("消息内容不能为空");
        }
        if (!ValidationUtils.validateMessageType(message.getMessageType())) {
            throw new WeebException("消息类型无效");
        }

        // 验证消息内容
        if (message.getContent() != null && message.getContent().getContent() != null) {
            if (!ValidationUtils.validateChatMessage(message.getContent().getContent())) {
                throw new WeebException("消息内容无效");
            }
        }

        // ✅ 直接使用sharedChatId
        message.setChatId(sharedChatId);

        // 设置消息发送时间
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        message.setCreatedAt(now);
        message.setUpdatedAt(now);

        // 插入消息记录
        messageMapper.insertMessage(message);

        // ✅ 查找发送者和接收者的chat_list记录
        ChatList senderChat = chatListMapper.selectChatListByUserIdAndSharedChatId(userId, sharedChatId);
        if (senderChat != null) {
            // 更新发送者的聊天列表
            chatListMapper.updateLastMessageAndUnreadCount(senderChat.getId(),
                    message.getContent() != null ? message.getContent().getContent() : "");
        }

        // ✅ 确定接收者并更新其聊天列表
        if (senderChat != null && "PRIVATE".equals(senderChat.getType())) {
            Long targetReceiverId = senderChat.getTargetId();

            if (targetReceiverId != null && !targetReceiverId.equals(userId)) {
                // 查找接收者的对应聊天记录
                ChatList receiverChatList = chatListMapper.selectChatListByUserAndTarget(targetReceiverId, userId);
                
                if (receiverChatList != null) {
                    // 更新接收者的聊天列表
                    chatListMapper.updateLastMessageAndUnreadCount(receiverChatList.getId(),
                            message.getContent() != null ? message.getContent().getContent() : "");
                    
                    // 增加接收者的未读计数
                    chatUnreadCountService.incrementUnreadCount(targetReceiverId, sharedChatId, 1);
                } else {
                    // 如果接收者没有聊天记录，自动创建一个（使用相同的sharedChatId）
                    ChatList newReceiverChat = new ChatList();
                    newReceiverChat.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                    newReceiverChat.setUserId(targetReceiverId);
                    newReceiverChat.setSharedChatId(sharedChatId);
                    newReceiverChat.setTargetId(userId);
                    newReceiverChat.setType("PRIVATE");
                    newReceiverChat.setTargetInfo("Private Chat");
                    newReceiverChat.setUnreadCount(1);
                    newReceiverChat.setLastMessage(message.getContent() != null ? message.getContent().getContent() : "");
                    chatListMapper.insertChatList(newReceiverChat);
                    
                    chatUnreadCountService.incrementUnreadCount(targetReceiverId, sharedChatId, 1);
                }

                // 转发消息给接收者
                messageBroadcastService.broadcastMessageToReceiver(message, targetReceiverId);
            }
        } else if (senderChat != null && "GROUP".equals(senderChat.getType())) {
            // 群聊：转发给所有群成员
            Long groupId = senderChat.getGroupId();
            if (groupId != null) {
                messageBroadcastService.broadcastMessageToGroup(message, groupId);
            }
        }

        return message;
    }

    @Override
    public boolean markAsReadBySharedChatId(Long userId, Long sharedChatId) {
        // 输入验证
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("无效的用户ID");
        }
        if (!ValidationUtils.validateId(sharedChatId, "共享聊天ID")) {
            throw new WeebException("无效的共享聊天ID");
        }

        log.info("🔍 标记已读: userId={}, sharedChatId={}", userId, sharedChatId);

        // 查找用户的chat_list记录
        ChatList chatList = chatListMapper.selectChatListByUserIdAndSharedChatId(userId, sharedChatId);
        
        // ✅ 修复：如果chat_list记录不存在，尝试自动创建（针对群组）
        if (chatList == null) {
            log.warn("⚠️ chat_list记录不存在，尝试自动修复: userId={}, sharedChatId={}", userId, sharedChatId);
            
            // 方案1: 检查是否是群组的shared_chat_id
            com.web.model.Group group = findGroupBySharedChatId(sharedChatId);
            
            // 方案2: 如果方案1失败，检查sharedChatId是否实际上是groupId（前端可能传错了）
            if (group == null) {
                log.warn("⚠️ 未找到shared_chat_id={}的群组，尝试作为groupId查找", sharedChatId);
                group = findGroupById(sharedChatId);
                if (group != null && group.getSharedChatId() != null) {
                    log.info("✅ 找到群组，但传入的是groupId而非sharedChatId。groupId={}, 正确的sharedChatId={}", 
                        sharedChatId, group.getSharedChatId());
                    // 递归调用，使用正确的sharedChatId
                    return markAsReadBySharedChatId(userId, group.getSharedChatId());
                }
            }
            
            if (group != null) {
                log.info("✅ 找到群组: groupId={}, groupName={}, sharedChatId={}", 
                    group.getId(), group.getGroupName(), group.getSharedChatId());
                
                // 检查用户是否是群成员
                if (isUserGroupMember(userId, group.getId())) {
                    // 自动为该用户创建chat_list记录
                    chatList = createChatListForGroupMember(userId, group);
                    log.info("✅ 自动为群成员创建chat_list记录: userId={}, groupId={}, sharedChatId={}", 
                        userId, group.getId(), group.getSharedChatId());
                } else {
                    log.error("❌ 用户不是群成员: userId={}, groupId={}", userId, group.getId());
                    throw new WeebException("您不是该群组成员");
                }
            } else {
                log.error("❌ 未找到对应的群组或私聊: sharedChatId={}", sharedChatId);
                throw new WeebException("聊天会话不存在");
            }
        }
        
        // 获取最后一条消息ID
        Message lastMessage = messageMapper.selectLastMessageByChatId(sharedChatId);
        Long lastMessageId = lastMessage != null ? lastMessage.getId() : null;
        
        // 使用新的未读计数服务
        chatUnreadCountService.markAsRead(userId, sharedChatId, lastMessageId);
        
        // 同时更新chat_list表
        return chatListMapper.resetUnreadCountByChatId(chatList.getId()) > 0;
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
}

