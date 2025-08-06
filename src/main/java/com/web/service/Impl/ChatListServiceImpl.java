package com.web.service.Impl;  // 定义包路径

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;  // 导入 MyBatis-Plus 的 ServiceImpl
import com.web.mapper.ChatListMapper;        // 导入自定义的 ChatListMapper 接口
import com.web.model.ChatList;               // 导入实体类 ChatList
import com.web.model.Group;                  // 导入实体类 Group
import com.web.model.Message;                // 导入实体类 Message
import com.web.model.User;                   // 导入实体类 User
import com.web.service.AuthService;          // 导入 AuthService 接口
import com.web.service.ChatListService;      // 导入 ChatListService 接口
import org.springframework.context.annotation.Lazy;  // 导入 Spring 的 @Lazy 注解
import org.springframework.stereotype.Service;         // 导入 Spring 的 @Service 注解
import org.springframework.transaction.annotation.Transactional; // Added import

import java.time.LocalDateTime;            // 导入 LocalDateTime 用于获取当前时间
import java.time.format.DateTimeFormatter; // 导入 DateTimeFormatter 用于格式化时间
import java.util.List;                     // 导入 List 集合

/**
 * ChatListServiceImpl 类，实现了聊天列表相关的业务逻辑
 * 该实现类继承自 MyBatis-Plus 的 ServiceImpl 类，并使用自定义的 ChatListMapper
 */
@Service  // 声明该类为 Spring 的 Service 组件
public class ChatListServiceImpl extends ServiceImpl<ChatListMapper, ChatList> implements ChatListService {

    // 使用构造器注入 AuthService，并添加 @Lazy 注解，避免循环依赖
    private final AuthService authService;

    /**
     * 构造函数，使用 @Lazy 注解注入 AuthService
     * @param authService 用户认证服务接口
     */
    public ChatListServiceImpl(@Lazy AuthService authService) {
        this.authService = authService;  // 将 authService 注入到成员变量中
    }

    /**
     * 获取指定用户的私聊列表
     * @param userId 用户ID
     * @return 私聊记录列表
     */
    @Override
    public List<ChatList> getPrivateChatList(Long userId) {
        // 通过调用自定义 Mapper 方法，根据用户ID和聊天类型（"0"：私聊）查询聊天记录
        return baseMapper.selectByUserIdAndType(userId, 0);
    }

    /**
     * 获取或创建群聊记录
     * @param userId 用户ID
     * @return 群聊记录对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatList getOrCreateGroupChat(Long userId) {
        // 调用自定义 Mapper 方法，根据用户ID和聊天类型（"1"：群聊）查询聊天记录
        ChatList chat = baseMapper.selectOneByUserIdAndType(userId, 1);
        if (chat == null) {  // 若记录不存在，则创建新记录
            chat = new ChatList();
            // 生成唯一ID，使用当前时间戳转换为字符串
            chat.setId(String.valueOf(System.currentTimeMillis()));
            chat.setType("1");                         // 设置聊天类型为群聊，类型字段采用 String 类型（例如："1"）
            chat.setUserId(userId);                  // 设置用户ID
            chat.setTargetId(1L);                    // 设置默认群组ID

            // 设置群组信息（这里只作示例，实际可调用 GroupService 获取群组详情）
            Group group = new Group();
            group.setId(1L);
            group.setGroupName("默认群聊");               // 默认群聊标题
            group.setGroupAvatarUrl("default_avatar.png");
            // 将群聊标题存入 targetInfo 字段
            chat.setTargetInfo(group.getGroupName());
            chat.setLastMessage("");  // 初始化最后消息为空字符串

            // 设置当前时间为更新时间和创建时间
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String now = LocalDateTime.now().format(formatter);
            chat.setUpdateTime(now);
            chat.setCreateTime(now);

            // 通过调用自定义 Mapper 方法插入新创建的聊天记录
            baseMapper.insertChatList(chat);
        }
        return chat;  // 返回查询或创建后的聊天记录
    }

    /**
     * 创建私聊记录
     * @param userId 发起私聊的用户ID
     * @param targetId 目标用户ID
     * @return 私聊记录对象，若用户自己与自己私聊或目标用户不存在，则返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatList createPrivateChat(Long userId, Long targetId) {
        if (userId.equals(targetId)) {
            return null;  // 用户不能与自己创建聊天
        }
        // 调用自定义 Mapper 方法，根据用户ID和目标用户ID查询私聊记录
        ChatList existingChat = baseMapper.selectPrivateChat(userId, targetId);
        if (existingChat != null) {
            return existingChat;  // 若已存在私聊记录，则直接返回
        }
        // 通过 AuthService 检查目标用户是否存在
        User targetUser = authService.findByUserID(targetId);
        if (targetUser == null) {
            return null;  // 目标用户不存在，返回 null
        }
        // 创建新的私聊记录
        ChatList chatList = new ChatList();
        // 生成唯一ID，转换为字符串
        chatList.setId(String.valueOf(System.currentTimeMillis()));
        chatList.setUserId(userId);                   // 设置当前用户ID
        chatList.setTargetId(targetId);               // 设置目标用户ID
        chatList.setType("0");                          // 设置聊天类型为私聊，类型字段采用 String 类型（例如："0"）
        chatList.setLastMessage("");                  // 初始化最后消息为空字符串
        // 设置 targetInfo 为目标用户的名称（ User 对象有 getUserName() 方法）
        chatList.setTargetInfo(targetUser.getUsername());

        // 设置当前时间为更新时间和创建时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String now = LocalDateTime.now().format(formatter);
        chatList.setUpdateTime(now);
        chatList.setCreateTime(now);

        // 通过调用自定义 Mapper 方法插入新记录
        baseMapper.insertChatList(chatList);
        return chatList;  // 返回创建的私聊记录
    }

    /**
     * 更新群聊最后消息字段
     * @param message 消息对象，消息内容将更新到群聊的最后消息字段
     * @return 如果更新成功返回 true，否则返回 false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGroupChatLastMessage(Message message) {
        // 通过调用自定义 Mapper 方法更新群聊记录的最后消息字段，并判断影响行数是否大于 0
        return baseMapper.updateGroupChatLastMessage(message.getContent().getContent()) > 0;
    }

    /**
     * 更新私聊最后消息字段
     * @param userId 当前用户ID
     * @param targetId 目标用户ID
     * @param message 消息对象
     * @return 如果更新成功返回 true，否则返回 false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePrivateChatLastMessage(Long userId, Long targetId, Message message) {
        // 查询私聊记录
        ChatList chatList = baseMapper.selectPrivateChat(userId, targetId);
        if (chatList == null) {  // 若私聊记录不存在，则尝试创建
            return createPrivateChat(userId, targetId) != null;
        }
        // 更新最后消息内容
        chatList.setLastMessage(message.getContent().getContent());
        // 更新更新时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String now = LocalDateTime.now().format(formatter);
        chatList.setUpdateTime(now);
        // 确保 userId 和 targetId 不为 null
        if (chatList.getUserId() == null) {
            chatList.setUserId(userId);
        }
        if (chatList.getTargetId() == null) {
            chatList.setTargetId(targetId);
        }
        // 如果 targetInfo 为空，则调用 AuthService 获取目标用户信息并设置
        if (chatList.getTargetInfo() == null) {
            User targetUser = authService.getUserByIdForTalk(targetId);
            if (targetUser != null) {
                chatList.setTargetInfo(targetUser.getUsername());
            } else {
                chatList.setTargetInfo("");
            }
        }
        // 不再更新 createTime，因为创建时间应保持不变
        return baseMapper.updateChatListById(chatList) > 0;
    }


    /**
     * 重置指定用户与目标之间的未读消息计数为 0
     * @param userId 用户ID
     * @param targetId 目标用户或群组ID
     * @return 如果更新成功返回 true，否则返回 false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetUnreadCount(Long userId, Long targetId) {
        // 通过调用自定义 Mapper 方法重置未读计数，并判断影响行数是否大于 0
        return baseMapper.resetUnreadCount(userId, targetId) > 0;
    }

    /**
     * 删除指定用户下的聊天记录
     * @param userId 用户ID
     * @param chatListId 聊天记录ID
     * @return 如果删除成功返回 true，否则返回 false
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteChatRecord(Long userId, Long chatListId) {
        // 通过调用自定义 Mapper 方法删除聊天记录，并判断影响行数是否大于 0
        return baseMapper.deleteChatRecord(userId, chatListId) > 0;
    }

    /**
     * 私有辅助方法，根据用户ID和目标用户ID查询私聊记录
     * @param userId 用户ID
     * @param targetId 目标用户ID
     * @return 私聊记录对象
     */
    private ChatList getTargetChatList(Long userId, Long targetId) {
        // 内部调用自定义 Mapper 方法查询私聊记录
        return baseMapper.selectPrivateChat(userId, targetId);
    }
}
