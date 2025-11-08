package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 消息映射器接口，负责消息的数据库操作
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 获取上一条需要显示时间的消息
     *
     * @param userId    用户ID
     * @param targetId  目标聊天对象ID
     * @return 上一条显示时间的消息
     */
    Message getPreviousShowTimeMsg(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 获取聊天记录
     *
     * @param userId   用户ID
     * @param targetId 目标聊天对象ID
     * @param index    起始索引
     * @param num      查询条数
     * @return 消息列表
     */
    List<Message> record(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("index") int index, @Param("num") int num);

    /**
     * 根据用户ID查询消息（用于selectMessagesByUsers方法）
     *
     * @param userId   用户ID
     * @param targetId 目标用户ID
     * @param index    起始索引
     * @param num      查询条数
     * @return 消息列表
     */
    List<Message> selectMessagesByUsers(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("index") int index, @Param("num") int num);

    /**
     * 插入消息记录
     * @param message 消息对象
     * @return 受影响行数
     */
    int insertMessage(Message message);

    /**
     * 插入群消息记录
     * @param message 消息对象
     * @return 受影响行数
     */
    int insertGroupMessage(Message message);

    /**
     * 根据消息ID查询消息
     * @param msgId 消息ID
     * @return 消息对象
     */
    Message selectMessageById(@Param("msgId") Long msgId);

    /**
     * 标记消息为已撤回
     * @param msgId 消息ID
     * @return 受影响行数
     */
    int markMessageAsRecalled(@Param("msgId") Long msgId);

    /**
     * 删除过期消息
     * @param expirationDate 过期日期
     * @return 受影响行数
     */
    int deleteExpiredMessages(@Param("expirationDate") LocalDate expirationDate);

    /**
     * 处理消息反应
     * @param reactionVo 反应视图对象
     * @param userId 用户ID
     * @return 受影响行数
     */
    int handleReaction(@Param("reactionVo") com.web.vo.message.ReactionVo reactionVo, @Param("userId") Long userId);

    /**
     * 根据聊天ID查询最后一条消息
     * @param chatId 聊天ID
     * @return 最后一条消息
     */
    Message selectLastMessageByChatId(@Param("chatId") Long chatId);

    /**
     * 根据聊天ID分页查询消息
     * @param chatId 聊天ID
     * @param offset 偏移量
     * @param size 每页大小
     * @return 消息列表
     */
    List<Message> selectMessagesByChatId(@Param("chatId") Long chatId, @Param("offset") int offset, @Param("size") int size);

    // ==================== 消息线程相关方法 ====================

    /**
     * 查询线程消息列表
     * @param parentMessageId 父消息ID（线程ID）
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 线程消息列表
     */
    List<Message> selectThreadMessages(@Param("parentMessageId") Long parentMessageId,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize);

    /**
     * 统计线程消息数量
     * @param parentMessageId 父消息ID（线程ID）
     * @return 消息数量
     */
    int countThreadMessages(@Param("parentMessageId") Long parentMessageId);

    /**
     * 查询线程摘要信息
     * @param parentMessageId 父消息ID（线程ID）
     * @return 线程摘要信息
     */
    java.util.Map<String, Object> selectThreadSummary(@Param("parentMessageId") Long parentMessageId);

    /**
     * 查询用户参与的线程
     * @param userId 用户ID
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 用户参与的线程列表
     */
    List<java.util.Map<String, Object>> selectUserThreads(@Param("userId") Long userId,
                                                       @Param("offset") int offset,
                                                       @Param("pageSize") int pageSize);

    /**
     * 统计用户参与的线程数量
     * @param userId 用户ID
     * @return 线程数量
     */
    int countUserThreads(@Param("userId") Long userId);

    /**
     * 根据两个聊天ID查询消息（用于私聊双向消息查询）- 已废弃，使用selectMessagesBySharedChatId
     * @param chatId1 聊天ID1
     * @param chatId2 聊天ID2
     * @param offset 偏移量
     * @param size 每页大小
     * @return 消息列表
     * @deprecated 使用selectMessagesBySharedChatId代替
     */
    @Deprecated
    List<Message> selectMessagesByTwoChatIds(@Param("chatId1") Long chatId1, 
                                            @Param("chatId2") Long chatId2, 
                                            @Param("offset") int offset, 
                                            @Param("size") int size);

    /**
     * 根据共享聊天ID查询消息（新架构）
     * @param sharedChatId 共享聊天ID
     * @param offset 偏移量
     * @param size 每页大小
     * @return 消息列表
     */
    List<Message> selectMessagesBySharedChatId(@Param("sharedChatId") Long sharedChatId, 
                                              @Param("offset") int offset, 
                                              @Param("size") int size);
}
