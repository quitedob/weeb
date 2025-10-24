package com.web.service;

import com.web.model.Message;

import java.time.LocalDate;
import java.util.List;

/**
 * 消息服务接口，定义消息相关的操作
 */
public interface MessageService {

    /**
     * 发送消息
     *
     * @param userId      当前用户ID，类型为 Long
     * @param messageBody 消息对象，包含消息内容、目标等信息
     * @return 发送后的消息对象
     */
    Message send(Long userId, Message messageBody);

    /**
     * 获取聊天记录
     *
     * @param userId   当前用户ID，类型为 Long
     * @param targetId 目标聊天对象ID，类型为 Long
     * @param index    起始索引
     * @param num      查询条数
     * @return 聊天记录列表
     */
    List<Message> record(Long userId, Long targetId, int index, int num);

    /**
     * 撤回消息
     *
     * @param userId 当前用户ID，类型为 Long
     * @param msgId  要撤回的消息ID，类型为 Long
     * @return 撤回后的消息对象
     */
    Message recall(Long userId, Long msgId);

    /**
     * 删除过期消息
     *
     * @param expirationDate 过期日期
     */
    void deleteExpiredMessages(LocalDate expirationDate);

    /**
     * 发送群组消息
     *
     * @param userId      当前用户ID，类型为 Long
     * @param messageBody 消息对象，包含消息内容、目标等信息
     * @return 发送后的消息对象
     */
    Message sendMessageToGroup(Long userId, Message messageBody);

    /**
     * 发送私聊消息
     *
     * @param userId      当前用户ID，类型为 Long
     * @param messageBody 消息对象，包含消息内容、目标等信息
     * @return 发送后的消息对象
     */
    Message sendMessageToUser(Long userId, Message messageBody);

  /**
   * 处理用户对消息的反应（添加/取消）
   * @param reactionVo 反应视图
   * @param userId 用户ID
   * 简化注释：处理反应
   */
  void handleReaction(com.web.vo.message.ReactionVo reactionVo, Long userId); // Changed userId to Long

  /**
   * 保存WebSocket消息到数据库
   *
   * @param messageData 消息数据Map
   * @param userId 发送者ID
   * @return 保存后的消息对象
   */
  Message saveWebSocketMessage(java.util.Map<String, Object> messageData, Long userId);

  // ==================== 消息线程相关方法 ====================

  /**
   * 创建消息线程
   *
   * @param userId 用户ID
   * @param parentMessageId 父消息ID
   * @param content 线程消息内容
   * @return 创建的线程消息
   */
  Message createThread(Long userId, Long parentMessageId, String content);

  /**
   * 获取消息线程列表
   *
   * @param parentMessageId 父消息ID
   * @param page 页码
   * @param pageSize 每页大小
   * @return 线程消息列表和分页信息
   */
  java.util.Map<String, Object> getThreadMessages(Long parentMessageId, int page, int pageSize);

  /**
   * 获取消息的所有线程摘要
   *
   * @param parentMessageId 父消息ID
   * @return 线程摘要信息（消息数量、参与人数、最后回复时间等）
   */
  java.util.Map<String, Object> getThreadSummary(Long parentMessageId);

  /**
   * 在线程中回复消息
   *
   * @param userId 用户ID
   * @param threadId 线程ID
   * @param content 回复内容
   * @return 回复的消息对象
   */
  Message replyToThread(Long userId, Long threadId, String content);

  /**
   * 删除消息线程
   *
   * @param threadId 线程ID
   * @param userId 操作用户ID
   * @return 删除是否成功
   */
  boolean deleteThread(Long threadId, Long userId);

  /**
   * 获取用户相关的所有线程
   *
   * @param userId 用户ID
   * @param page 页码
   * @param pageSize 每页大小
   * @return 用户参与的线程列表
   */
  java.util.Map<String, Object> getUserThreads(Long userId, int page, int pageSize);
}
