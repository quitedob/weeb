package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.ChatListMapper;
import com.web.mapper.MessageMapper;
import com.web.model.ChatList;
import com.web.model.Message;
import com.web.service.ChatService;
import com.web.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天服务实现类
 * 实现聊天相关的核心业务逻辑
 */
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

        // ✅ 修复1：为当前用户创建chat_list记录（使用sharedChatId作为ID的一部分）
        ChatList userChatList = new ChatList();
        userChatList.setId(String.valueOf(sharedChatId) + "_" + userId); // 使用 sharedChatId_userId 格式
        userChatList.setUserId(userId);
        userChatList.setSharedChatId(sharedChatId);
        userChatList.setTargetId(targetId);
        userChatList.setType("PRIVATE");
        userChatList.setTargetInfo("Private Chat");
        userChatList.setUnreadCount(0);

        chatListMapper.insertChatList(userChatList);

        // ✅ 修复1：检查对方是否已有chat_list记录，如果没有则创建（使用相同的sharedChatId）
        ChatList targetChatList = chatListMapper.selectChatListByUserAndTarget(targetId, userId);
        if (targetChatList == null) {
            targetChatList = new ChatList();
            targetChatList.setId(String.valueOf(sharedChatId) + "_" + targetId); // 使用 sharedChatId_targetId 格式
            targetChatList.setUserId(targetId);
            targetChatList.setSharedChatId(sharedChatId); // ✅ 关键：使用相同的sharedChatId
            targetChatList.setTargetId(userId);
            targetChatList.setType("PRIVATE");
            targetChatList.setTargetInfo("Private Chat");
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

    @Override
    public void addReaction(Long userId, Long messageId, String reactionType) {
        // 检查消息是否存在
        Message message = messageMapper.selectMessageById(messageId);
        if (message == null) {
            throw new WeebException("消息不存在");
        }

        // 添加或取消反应（这里简化处理，实际应该有专门的反应表）
        // 这里暂时在消息内容中记录反应信息
        // 实际实现应该有MessageReaction表和MessageReactionMapper
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

        // 查找用户的chat_list记录
        ChatList chatList = chatListMapper.selectChatListByUserIdAndSharedChatId(userId, sharedChatId);
        if (chatList == null) {
            throw new WeebException("聊天会话不存在");
        }
        
        // 获取最后一条消息ID
        Message lastMessage = messageMapper.selectLastMessageByChatId(sharedChatId);
        Long lastMessageId = lastMessage != null ? lastMessage.getId() : null;
        
        // 使用新的未读计数服务
        chatUnreadCountService.markAsRead(userId, sharedChatId, lastMessageId);
        
        // 同时更新chat_list表
        return chatListMapper.resetUnreadCountByChatId(chatList.getId()) > 0;
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

