package com.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.exception.WeebException;
import com.web.mapper.ChatListMapper;
import com.web.model.ChatList;
import com.web.model.Message;
import com.web.service.ChatListService;
import com.web.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天列表服务实现类
 * 处理聊天列表的创建、更新、删除等业务逻辑
 */
@Service
@Transactional
public class ChatListServiceImpl extends ServiceImpl<ChatListMapper, ChatList> implements ChatListService {

    @Autowired
    private ChatListMapper chatListMapper;

    @Override
    public List<ChatList> getPrivateChatList(Long userId) {
        // 获取用户的所有私聊记录
        return chatListMapper.selectByUserIdAndType(userId, 0);
    }

    @Override
    public ChatList getOrCreateGroupChat(Long userId) {
        // 获取用户的群聊记录
        ChatList groupChat = chatListMapper.selectOneByUserIdAndType(userId, 1);
        
        if (groupChat == null) {
            // 如果不存在群聊记录，创建默认群聊记录
            groupChat = new ChatList();
            groupChat.setUserId(userId);
            groupChat.setTargetId(0L); // 群聊的targetId设为0
            groupChat.setType("1"); // "1"表示群聊
            groupChat.setUnreadCount(0);
            groupChat.setLastMessage("");
            groupChat.setCreateTime(LocalDateTime.now().toString());
            groupChat.setUpdateTime(LocalDateTime.now().toString());
            
            int result = chatListMapper.insertChatList(groupChat);
            if (result <= 0) {
                throw new WeebException("创建群聊记录失败");
            }
        }
        
        return groupChat;
    }

    @Override
    public ChatList createPrivateChat(Long userId, Long targetId) {
        // 检查是否已存在私聊记录
        ChatList existingChat = chatListMapper.selectPrivateChat(userId, targetId);
        if (existingChat != null) {
            return existingChat;
        }
        
        // 创建新的聊天记录
        ChatList chatList = new ChatList();
        chatList.setUserId(userId);
        chatList.setTargetId(targetId);
        chatList.setType("0"); // "0"表示私聊
        chatList.setUnreadCount(0);
        chatList.setLastMessage("");
        chatList.setCreateTime(LocalDateTime.now().toString());
        chatList.setUpdateTime(LocalDateTime.now().toString());
        
        int result = chatListMapper.insertChatList(chatList);
        if (result > 0) {
            return chatList;
        }
        
        throw new WeebException("创建聊天记录失败");
    }

    @Override
    public boolean updateGroupChatLastMessage(Message message) {
        try {
            String lastMessage = message.getContent() != null ? message.getContent().toString() : "";
            if (lastMessage.length() > 100) {
                lastMessage = lastMessage.substring(0, 100) + "...";
            }
            
            int result = chatListMapper.updateGroupChatLastMessage(lastMessage);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean updatePrivateChatLastMessage(Long userId, Long targetId, Message message) {
        try {
            // 查找聊天记录
            ChatList chatList = chatListMapper.selectPrivateChat(userId, targetId);
            if (chatList != null) {
                String lastMessage = message.getContent() != null ? message.getContent().toString() : "";
                if (lastMessage.length() > 100) {
                    lastMessage = lastMessage.substring(0, 100) + "...";
                }
                
                chatList.setLastMessage(lastMessage);
                chatList.setUpdateTime(LocalDateTime.now().toString());
                
                // 如果消息不是当前用户发送的，增加未读数
                if (!message.getSenderId().equals(userId)) {
                    chatList.setUnreadCount(chatList.getUnreadCount() + 1);
                }
                
                int result = chatListMapper.updateChatListById(chatList);
                return result > 0;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean resetUnreadCount(Long userId, Long targetId) {
        // 重置未读消息数为0
        int result = chatListMapper.resetUnreadCount(userId, targetId);
        return result > 0;
    }

    @Override
    public boolean deleteChatRecord(Long userId, Long chatListId) {
        // 删除聊天记录
        int result = chatListMapper.deleteChatRecord(userId, chatListId);
        return result > 0;
    }

    @Override
    public List<ChatList> getAllChatList(Long userId) {
        // 获取用户的所有私聊记录
        List<ChatList> privateChats = chatListMapper.selectByUserIdAndType(userId, 0);

        // 获取用户的群聊记录
        ChatList groupChat = chatListMapper.selectOneByUserIdAndType(userId, 1);
        if (groupChat != null) {
            privateChats.add(groupChat);
        }

        return privateChats;
    }
}