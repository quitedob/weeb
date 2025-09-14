package com.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.web.model.ChatList;
import com.web.model.Message;

import java.util.List;

/**
 * 聊天列表服务接口，定义了操作 ChatList 的方法
 */
public interface ChatListService extends IService<ChatList> {

    /**
     * 获取用户的私聊列表
     *
     * @param userId 用户ID
     * @return 返回该用户的私聊列表
     */
    List<ChatList> getPrivateChatList(Long userId);

    /**
     * 获取用户的群聊记录，如果不存在则自动创建默认群聊记录
     *
     * @param userId 用户ID
     * @return 返回群聊记录
     */
    ChatList getOrCreateGroupChat(Long userId);

    /**
     * 创建一个新的私聊记录
     *
     * @param userId   当前用户ID
     * @param targetId 目标用户ID
     * @return 返回创建的聊天记录
     */
    ChatList createPrivateChat(Long userId, Long targetId);

    /**
     * 更新群聊的最后一条消息记录
     *
     * @param message 最新的消息对象
     * @return 返回更新结果
     */
    boolean updateGroupChatLastMessage(Message message);

    /**
     * 更新用户之间的私聊记录
     *
     * @param userId   当前用户ID
     * @param targetId 目标用户ID
     * @param message  新的消息对象
     * @return 返回更新结果
     */
    boolean updatePrivateChatLastMessage(Long userId, Long targetId, Message message);

    /**
     * 清零用户与目标用户之间的未读消息数
     *
     * @param userId   当前用户ID
     * @param targetId 目标用户ID
     * @return 返回操作结果
     */
    boolean resetUnreadCount(Long userId, Long targetId);

    /**
     * 删除用户的指定聊天记录
     *
     * @param userId     当前用户ID
     * @param chatListId 聊天记录ID
     * @return 返回删除结果
     */
    boolean deleteChatRecord(Long userId, Long chatListId);
}
