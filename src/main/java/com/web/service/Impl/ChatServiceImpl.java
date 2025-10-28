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

    @Override
    public List<ChatList> getChatList(Long userId) {
        // 获取用户的所有聊天会话
        List<ChatList> chatLists = chatListMapper.selectChatListByUserId(userId);

        // 为每个聊天会话获取最后一条消息
        for (ChatList chatList : chatLists) {
            Message lastMessage = messageMapper.selectLastMessageByChatId(Long.valueOf(chatList.getId()));
            chatList.setLastMessage(lastMessage != null ? lastMessage.getContent().toString() : "");
        }

        return chatLists;
    }

    @Override
    public ChatList createChat(Long userId, Long targetId) {
        // 检查是否已存在聊天会话
        ChatList existingChat = chatListMapper.selectChatListByUserAndTarget(userId, targetId);
        if (existingChat != null) {
            return existingChat;
        }

        // 创建新的私聊会话
        ChatList chatList = new ChatList();
        chatList.setUserId(userId);
        chatList.setTargetId(targetId);
        chatList.setType("PRIVATE");
        chatList.setTargetInfo("Private Chat"); // 这里应该获取目标用户的昵称
        chatList.setUnreadCount(0);

        chatListMapper.insertChatList(chatList);
        return chatList;
    }

    @Override
    public List<Message> getChatMessages(Long chatId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        return messageMapper.selectMessagesByChatId(chatId, offset, size);
    }

    @Override
    public Message sendMessage(Long userId, Message message) {
        // 设置消息发送时间
        message.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        message.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

        // 插入消息记录
        messageMapper.insertMessage(message);

        // 更新聊天列表的最后消息和未读数
        chatListMapper.updateLastMessageAndUnreadCount(message.getChatId(),
                message.getContent().toString());

        return message;
    }

    @Override
    public boolean markAsRead(Long userId, Long chatId) {
        return chatListMapper.resetUnreadCountByChatId(chatId) > 0;
    }

    @Override
    public boolean deleteChat(Long userId, Long chatId) {
        // 检查聊天会话是否属于当前用户
        ChatList chatList = chatListMapper.selectChatListById(chatId);
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

        // 检查消息发送时间，超过2分钟不允许撤回
        long currentTime = System.currentTimeMillis();
        long messageTime = message.getCreatedAt().getTime();
        long timeDiff = currentTime - messageTime;
        long twoMinutesInMillis = 2 * 60 * 1000;

        if (timeDiff > twoMinutesInMillis) {
            throw new WeebException("消息发送超过2分钟，无法撤回");
        }

        // 标记消息为已撤回
        return messageMapper.markMessageAsRecalled(messageId) > 0;
    }
}

