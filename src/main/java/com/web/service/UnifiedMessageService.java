package com.web.service;

import com.web.model.Message;
import com.web.model.ChatList;
import com.web.vo.message.SendMessageVo;

import java.util.List;
import java.util.Map;

/**
 * 统一消息服务接口
 * 整合群组消息和私聊消息的处理逻辑
 * 提供统一的消息发送、接收、查询等功能
 */
public interface UnifiedMessageService {

    /**
     * 统一发送消息接口
     * 根据消息类型自动判断是私聊还是群聊消息
     * @param sendMessageVo 消息内容
     * @param userId 发送者ID
     * @return 发送的消息
     */
    Message sendMessage(SendMessageVo sendMessageVo, Long userId);

    /**
     * 发送私聊消息
     * @param targetUserId 目标用户ID
     * @param content 消息内容
     * @param senderId 发送者ID
     * @return 发送的消息
     */
    Message sendPrivateMessage(Long targetUserId, String content, Long senderId);

    /**
     * 发送群聊消息
     * @param groupId 群组ID
     * @param content 消息内容
     * @param senderId 发送者ID
     * @return 发送的消息
     */
    Message sendGroupMessage(Long groupId, String content, Long senderId);

    /**
     * 获取统一的消息列表
     * 包含私聊和群聊的所有消息
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    Map<String, Object> getUnifiedMessageList(Long userId, int page, int size);

    /**
     * 获取私聊消息历史
     * @param userId 当前用户ID
     * @param targetUserId 目标用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    List<Message> getPrivateMessageHistory(Long userId, Long targetUserId, int page, int size);

    /**
     * 获取群聊消息历史
     * @param groupId 群组ID
     * @param userId 用户ID（用于权限检查）
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    List<Message> getGroupMessageHistory(Long groupId, Long userId, int page, int size);

    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markMessageAsRead(Long messageId, Long userId);

    /**
     * 标记私聊会话为已读
     * @param targetUserId 目标用户ID
     * @param userId 当前用户ID
     * @return 是否成功
     */
    boolean markPrivateChatAsRead(Long targetUserId, Long userId);

    /**
     * 标记群聊为已读
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markGroupChatAsRead(Long groupId, Long userId);

    /**
     * 获取未读消息统计
     * @param userId 用户ID
     * @return 未读消息统计信息
     */
    Map<String, Object> getUnreadMessageStats(Long userId);

    /**
     * 删除消息
     * @param messageId 消息ID
     * @param userId 用户ID（用于权限检查）
     * @return 是否成功
     */
    boolean deleteMessage(Long messageId, Long userId);

    /**
     * 撤回消息
     * @param messageId 消息ID
     * @param userId 用户ID（用于权限检查）
     * @return 是否成功
     */
    boolean recallMessage(Long messageId, Long userId);

    /**
     * 搜索消息
     * @param userId 用户ID
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Map<String, Object> searchMessages(Long userId, String keyword, int page, int size);

    /**
     * 获取消息详情
     * @param messageId 消息ID
     * @param userId 用户ID（用于权限检查）
     * @return 消息详情
     */
    Message getMessageById(Long messageId, Long userId);

    /**
     * 检查用户是否有权限访问消息
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean hasMessagePermission(Long messageId, Long userId);

    /**
     * 获取统一聊天列表
     * 包含私聊和群聊的会话列表
     * @param userId 用户ID
     * @return 聊天列表
     */
    List<ChatList> getUnifiedChatList(Long userId);
}