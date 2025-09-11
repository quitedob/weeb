package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.MessageReaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息反应数据访问接口
 * 简化注释：消息反应Mapper
 */
@Mapper
public interface MessageReactionMapper extends BaseMapper<MessageReaction> {

    /**
     * 根据消息ID获取所有反应
     * @param messageId 消息ID
     * @return 反应列表
     */
    List<MessageReaction> selectByMessageId(@Param("messageId") Long messageId);

    /**
     * 根据消息ID和用户ID获取特定反应
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 反应对象
     */
    MessageReaction selectByMessageIdAndUserId(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /**
     * 删除用户对特定消息的反应
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 影响的行数
     */
    int deleteByMessageIdAndUserId(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /**
     * 统计消息的反应数量
     * @param messageId 消息ID
     * @return 反应数量
     */
    int countByMessageId(@Param("messageId") Long messageId);
} 