package com.web.service;

import com.web.model.ChatList;
import com.web.model.Message;
import java.util.List;

/**
 * 聊天服务接口
 * 定义聊天相关的业务逻辑契约
 */
public interface ChatService {

    /**
     * 获取用户的聊天列表
     * @param userId 用户ID
     * @return 聊天列表
     */
    List<ChatList> getChatList(Long userId);

    /**
     * 创建新的聊天会话
     * @param userId 用户ID
     * @param targetId 目标用户ID
     * @return 创建的聊天会话
     */
    ChatList createChat(Long userId, Long targetId);

    /**
     * 获取聊天消息历史记录
     * @param chatId 聊天ID
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    List<Message> getChatMessages(Long chatId, Integer page, Integer size);

    /**
     * 发送聊天消息
     * @param userId 用户ID
     * @param message 消息对象
     * @return 发送的完整消息对象
     */
    Message sendMessage(Long userId, Message message);

    /**
     * 标记消息为已读
     * @param userId 用户ID
     * @param chatId 聊天ID
     * @return 操作结果
     */
    boolean markAsRead(Long userId, Long chatId);

    /**
     * 删除聊天会话
     * @param userId 用户ID
     * @param chatId 聊天ID
     * @return 删除结果
     */
    boolean deleteChat(Long userId, Long chatId);

    /**
     * 对消息添加反应
     * @param userId 用户ID
     * @param messageId 消息ID
     * @param reactionType 反应类型
     */
    void addReaction(Long userId, Long messageId, String reactionType);

    /**
     * 撤回消息
     * @param userId 用户ID
     * @param messageId 消息ID
     * @return 撤回结果
     */
    boolean recallMessage(Long userId, Long messageId);
}

