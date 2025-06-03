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
}
