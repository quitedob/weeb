package com.web.mapper;

import com.web.model.MessageThread;
import com.web.model.Message;
import com.web.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 消息线索Mapper接口
 */
@Mapper
public interface MessageThreadMapper {

    /**
     * 插入消息线索
     */
    @Insert("INSERT INTO message_threads (root_message_id, title, description, created_by, created_by_username, " +
            "status, participant_count, is_pinned, is_locked, tags, metadata, created_at, updated_at) " +
            "VALUES (#{rootMessageId}, #{title}, #{description}, #{createdBy}, #{createdByUsername}, " +
            "#{status}, #{participantCount}, #{isPinned}, #{isLocked}, #{tags}, #{metadata}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MessageThread thread);

    /**
     * 更新消息线索
     */
    @Update("UPDATE message_threads SET title = #{title}, description = #{description}, " +
            "status = #{status}, participant_count = #{participantCount}, " +
            "last_reply_at = #{lastReplyAt}, last_reply_by = #{lastReplyBy}, " +
            "last_reply_by_username = #{lastReplyByUsername}, is_pinned = #{isPinned}, " +
            "is_locked = #{isLocked}, tags = #{tags}, metadata = #{metadata}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int update(MessageThread thread);

    /**
     * 根据ID查找线索
     */
    @Select("SELECT * FROM message_threads WHERE id = #{threadId}")
    MessageThread findById(Long threadId);

    /**
     * 获取线索中的消息列表
     */
    @Select("SELECT m.* FROM messages m " +
            "WHERE m.thread_id = #{threadId} " +
            "ORDER BY m.created_at ASC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<Message> getThreadMessages(@Param("threadId") Long threadId,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    /**
     * 获取线索消息总数
     */
    @Select("SELECT COUNT(*) FROM messages WHERE thread_id = #{threadId}")
    int getThreadMessageCount(Long threadId);

    /**
     * 检查用户是否是线索参与者
     */
    @Select("SELECT COUNT(*) > 0 FROM thread_participants WHERE thread_id = #{threadId} AND user_id = #{userId}")
    boolean isParticipant(@Param("threadId") Long threadId, @Param("userId") Long userId);

    /**
     * 添加线索参与者
     */
    @Insert("INSERT INTO thread_participants (thread_id, user_id, joined_at) " +
            "VALUES (#{threadId}, #{userId}, NOW())")
    int addParticipant(@Param("threadId") Long threadId, @Param("userId") Long userId);

    /**
     * 移除线索参与者
     */
    @Delete("DELETE FROM thread_participants WHERE thread_id = #{threadId} AND user_id = #{userId}")
    int removeParticipant(@Param("threadId") Long threadId, @Param("userId") Long userId);

    /**
     * 获取用户参与的线索列表
     */
    @Select("SELECT t.* FROM message_threads t " +
            "INNER JOIN thread_participants tp ON t.id = tp.thread_id " +
            "WHERE tp.user_id = #{userId} " +
            "ORDER BY t.last_reply_at DESC, t.created_at DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<MessageThread> getUserThreads(@Param("userId") Long userId,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize);

    /**
     * 获取用户参与的线索总数
     */
    @Select("SELECT COUNT(*) FROM thread_participants WHERE user_id = #{userId}")
    int getUserThreadCount(Long userId);

    /**
     * 获取活跃线索列表
     */
    @Select("SELECT * FROM message_threads " +
            "WHERE status = 'active' " +
            "ORDER BY is_pinned DESC, last_reply_at DESC, created_at DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<MessageThread> getActiveThreads(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 获取活跃线索总数
     */
    @Select("SELECT COUNT(*) FROM message_threads WHERE status = 'active'")
    int getActiveThreadCount();

    /**
     * 获取用户创建的线索列表
     */
    @Select("SELECT * FROM message_threads " +
            "WHERE created_by = #{userId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<MessageThread> getUserCreatedThreads(@Param("userId") Long userId,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    /**
     * 获取用户创建的线索总数
     */
    @Select("SELECT COUNT(*) FROM message_threads WHERE created_by = #{userId}")
    int getUserCreatedThreadCount(Long userId);

    /**
     * 搜索线索
     */
    @Select("SELECT * FROM message_threads " +
            "WHERE (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY is_pinned DESC, last_reply_at DESC, created_at DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<MessageThread> searchThreads(@Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    /**
     * 搜索线索总数
     */
    @Select("SELECT COUNT(*) FROM message_threads " +
            "WHERE title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')")
    int searchThreadCount(String keyword);

    /**
     * 获取线索中的最后一条消息
     */
    @Select("SELECT * FROM messages " +
            "WHERE thread_id = #{threadId} " +
            "ORDER BY created_at DESC " +
            "LIMIT 1")
    Message getLastThreadMessage(Long threadId);

    /**
     * 根据根消息ID查找线索
     */
    @Select("SELECT * FROM message_threads WHERE root_message_id = #{rootMessageId}")
    MessageThread findByRootMessageId(Long rootMessageId);

    /**
     * 获取线索的参与者列表
     */
    @Select("SELECT u.* FROM users u " +
            "INNER JOIN thread_participants tp ON u.id = tp.user_id " +
            "WHERE tp.thread_id = #{threadId} " +
            "ORDER BY tp.joined_at ASC")
    List<User> getThreadParticipants(Long threadId);

    /**
     * 批量更新线索状态
     */
    @Update("UPDATE message_threads SET status = #{status}, updated_at = NOW() WHERE id IN #{threadIds}")
    int batchUpdateStatus(@Param("threadIds") List<Long> threadIds, @Param("status") String status);

    /**
     * 删除线索（软删除）
     */
    @Update("UPDATE message_threads SET status = 'deleted', updated_at = NOW() WHERE id = #{threadId}")
    int deleteById(Long threadId);

    /**
     * 获取线索的回复统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_replies, " +
            "COUNT(CASE WHEN DATE(created_at) = CURDATE() THEN 1 END) as today_replies, " +
            "COUNT(CASE WHEN DATE(created_at) = CURDATE() - INTERVAL 1 DAY THEN 1 END) as yesterday_replies, " +
            "COUNT(CASE WHEN DATE(created_at) >= CURDATE() - INTERVAL 7 DAY THEN 1 END) as week_replies " +
            "FROM messages WHERE thread_id = #{threadId}")
    Map<String, Object> getThreadReplyStats(Long threadId);

    /**
     * 获取热门线索（按回复数排序）
     */
    @Select("SELECT t.*, COUNT(m.id) as reply_count " +
            "FROM message_threads t " +
            "LEFT JOIN messages m ON t.id = m.thread_id " +
            "WHERE t.status = 'active' " +
            "GROUP BY t.id " +
            "ORDER BY reply_count DESC, t.last_reply_at DESC " +
            "LIMIT #{limit}")
    List<MessageThread> getPopularThreads(@Param("limit") int limit);

    /**
     * 获取用户在特定时间范围内的线索活动
     */
    @Select("SELECT t.*, COUNT(m.id) as user_reply_count " +
            "FROM message_threads t " +
            "INNER JOIN messages m ON t.id = m.thread_id " +
            "WHERE m.user_id = #{userId} " +
            "AND m.created_at BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY t.id " +
            "ORDER BY user_reply_count DESC, m.created_at DESC")
    List<MessageThread> getUserThreadActivity(@Param("userId") Long userId,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate);
}