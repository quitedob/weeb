package com.web.mapper;  // 定义包路径

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.ChatList;  // 导入 ChatList 实体类
import org.apache.ibatis.annotations.Mapper;  // 导入 MyBatis 的 @Mapper 注解
import org.apache.ibatis.annotations.Param;   // 导入 MyBatis 的 @Param 注解
import java.util.List;  // 导入 List 集合

/**
 * ChatListMapper 接口，负责操作数据库中 chat_list 表的 CRUD 操作
 */
@Mapper  // 标识此接口为 MyBatis Mapper 接口
public interface ChatListMapper extends BaseMapper<ChatList> {

    /**
     * 根据用户ID和会话类型查询聊天记录集合
     * @param userId 用户ID
     * @param type 会话类型（例如：0 表示私聊，1 表示群聊）
     * @return 符合条件的聊天记录列表
     */
    List<ChatList> selectByUserIdAndType(@Param("userId") Long userId, @Param("type") Integer type);

    /**
     * 根据用户ID和会话类型查询单个聊天记录
     * @param userId 用户ID
     * @param type 会话类型
     * @return 符合条件的一条聊天记录
     */
    ChatList selectOneByUserIdAndType(@Param("userId") Long userId, @Param("type") Integer type);

    /**
     * 根据用户ID和目标用户ID查询私聊记录（假设私聊类型为 0）
     * @param userId 用户ID
     * @param targetId 目标用户ID
     * @return 私聊记录，如果不存在则返回 null
     */
    ChatList selectPrivateChat(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 插入新的聊天记录
     * @param chatList 聊天记录对象
     * @return 受影响的行数
     */
    int insertChatList(ChatList chatList);

    /**
     * 根据主键ID更新聊天记录
     * @param chatList 聊天记录对象
     * @return 受影响的行数
     */
    int updateChatListById(ChatList chatList);

    /**
     * 更新所有群聊记录的最后一条消息
     * @param lastMessage 最后一条消息内容
     * @return 受影响的行数
     */
    int updateGroupChatLastMessage(@Param("lastMessage") String lastMessage);

    /**
     * 重置指定用户与目标用户之间的未读消息数为 0
     * @param userId 用户ID
     * @param targetId 目标用户ID
     * @return 受影响的行数
     */
    int resetUnreadCount(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 根据用户ID和聊天记录ID删除聊天记录
     * @param userId 用户ID
     * @param chatListId 聊天记录ID
     * @return 受影响的行数
     */
    int deleteChatRecord(@Param("userId") Long userId, @Param("chatListId") Long chatListId);
}
