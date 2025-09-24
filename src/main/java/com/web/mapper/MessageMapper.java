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
}
