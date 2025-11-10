package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.MessageReaction;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 消息反应Mapper接口
 */
@Mapper
public interface MessageReactionMapper extends BaseMapper<MessageReaction> {

    /**
     * 查找用户对消息的特定反应
     */
    @Select("SELECT * FROM message_reaction WHERE message_id = #{messageId} AND user_id = #{userId} AND reaction_type = #{reactionType}")
    MessageReaction findByMessageUserAndType(@Param("messageId") Long messageId, 
                                            @Param("userId") Long userId, 
                                            @Param("reactionType") String reactionType);

    /**
     * 删除用户对消息的特定反应
     */
    @Delete("DELETE FROM message_reaction WHERE message_id = #{messageId} AND user_id = #{userId} AND reaction_type = #{reactionType}")
    int deleteByMessageUserAndType(@Param("messageId") Long messageId, 
                                   @Param("userId") Long userId, 
                                   @Param("reactionType") String reactionType);

    /**
     * 获取消息的所有反应统计
     * 返回格式：[{reactionType: "👍", count: 5, userIds: [1,2,3,4,5]}, ...]
     */
    @Select("SELECT reaction_type, COUNT(*) as count, GROUP_CONCAT(user_id) as user_ids " +
            "FROM message_reaction " +
            "WHERE message_id = #{messageId} " +
            "GROUP BY reaction_type")
    @Results({
        @Result(property = "reactionType", column = "reaction_type"),
        @Result(property = "count", column = "count"),
        @Result(property = "userIds", column = "user_ids")
    })
    List<Map<String, Object>> getReactionStatsByMessageId(@Param("messageId") Long messageId);

    /**
     * 获取消息的所有反应
     */
    @Select("SELECT * FROM message_reaction WHERE message_id = #{messageId}")
    List<MessageReaction> findByMessageId(@Param("messageId") Long messageId);

    /**
     * 批量获取多条消息的反应统计
     */
    @Select("<script>" +
            "SELECT message_id, reaction_type, COUNT(*) as count, GROUP_CONCAT(user_id) as user_ids " +
            "FROM message_reaction " +
            "WHERE message_id IN " +
            "<foreach item='id' collection='messageIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "GROUP BY message_id, reaction_type" +
            "</script>")
    List<Map<String, Object>> getReactionStatsByMessageIds(@Param("messageIds") List<Long> messageIds);
}
