package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.MessageMapper;
import com.web.mapper.MessageReactionMapper;
import com.web.model.Message;
import com.web.model.MessageReaction;
import com.web.service.MessageService;
import com.web.vo.message.ReactionVo;
import com.web.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 消息服务实现类
 * 处理消息发送、接收、撤回等业务逻辑
 */
@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageReactionMapper messageReactionMapper;

    @Override
    public Message send(Long userId, Message messageBody) {
        // 设置发送者ID
        messageBody.setSenderId(userId);
        // 设置发送时间
        messageBody.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        // 设置消息状态为正常
        messageBody.setIsRecalled(0);
        
        // 根据消息类型选择不同的发送方式
        if (messageBody.getMessageType() != null && messageBody.getMessageType() == 1) {
            // 群组消息
            return sendMessageToGroup(userId, messageBody);
        } else {
            // 私聊消息
            return sendMessageToUser(userId, messageBody);
        }
    }

    @Override
    public List<Message> record(Long userId, Long targetId, int index, int num) {
        return messageMapper.record(userId, targetId, index, num);
    }

    @Override
    public Message recall(Long userId, Long msgId) {
        // 先查询消息是否存在且属于当前用户
        Message message = messageMapper.selectMessageById(msgId);
        if (message == null || !message.getSenderId().equals(userId)) {
            throw new WeebException("消息不存在或无权限撤回");
        }
        
        // 检查消息是否已经被撤回
        if (message.getIsRecalled() != null && message.getIsRecalled() == 1) {
            throw new WeebException("消息已经被撤回");
        }
        
        // 检查撤回时间限制（例如：只能撤回2分钟内的消息）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sendTime = message.getCreatedAt().toLocalDateTime();
        if (sendTime.plusMinutes(2).isBefore(now)) {
            throw new WeebException("超过撤回时间限制");
        }
        
        // 标记消息为已撤回
        int result = messageMapper.markMessageAsRecalled(msgId);
        if (result > 0) {
            message.setIsRecalled(1);
            return message;
        }
        
        throw new WeebException("撤回消息失败");
    }

    @Override
    public void deleteExpiredMessages(LocalDate expirationDate) {
        messageMapper.deleteExpiredMessages(expirationDate);
    }

    @Override
    public Message sendMessageToGroup(Long userId, Message messageBody) {
        // 设置消息类型为群组消息
        messageBody.setMessageType(1);
        
        // 插入群组消息
        int result = messageMapper.insertGroupMessage(messageBody);
        if (result > 0) {
            return messageBody;
        }
        
        throw new WeebException("发送群组消息失败");
    }

    @Override
    public Message sendMessageToUser(Long userId, Message messageBody) {
        // 设置消息类型为私聊消息
        messageBody.setMessageType(0);
        
        // 插入私聊消息
        int result = messageMapper.insertMessage(messageBody);
        if (result > 0) {
            return messageBody;
        }
        
        throw new WeebException("发送私聊消息失败");
    }

    @Override
    public void handleReaction(ReactionVo reactionVo, Long userId) {
        // 检查消息是否存在
        Message message = messageMapper.selectMessageById(reactionVo.getMessageId());
        if (message == null) {
            throw new WeebException("消息不存在");
        }
        
        // 检查用户是否已经对该消息有反应
        MessageReaction existingReaction = messageReactionMapper.selectByMessageIdAndUserId(
            reactionVo.getMessageId(), userId);
        
        if ("add".equals(reactionVo.getAction())) {
            // 添加反应
            if (existingReaction != null) {
                // 如果已存在反应，更新反应类型
                existingReaction.setReactionType(reactionVo.getReactionType());
                messageReactionMapper.updateById(existingReaction);
            } else {
                // 创建新的反应
                MessageReaction newReaction = new MessageReaction();
                newReaction.setMessageId(reactionVo.getMessageId());
                newReaction.setUserId(userId);
                newReaction.setReactionType(reactionVo.getReactionType());
                newReaction.setCreateTime(new Date());
                messageReactionMapper.insert(newReaction);
            }
        } else if ("remove".equals(reactionVo.getAction())) {
            // 移除反应
            if (existingReaction != null) {
                messageReactionMapper.deleteByMessageIdAndUserId(reactionVo.getMessageId(), userId);
            }
        } else {
            throw new WeebException("不支持的反应操作");
        }
    }
}