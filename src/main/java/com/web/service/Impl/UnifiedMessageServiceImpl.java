package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.*;
import com.web.model.*;
import com.web.service.*;
import com.web.service.UserTypeSecurityService;
import com.web.vo.message.SendMessageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

/**
 * 统一消息服务实现类
 * 整合群组消息和私聊消息的处理逻辑
 * 提供统一的消息发送、接收、查询等功能
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class UnifiedMessageServiceImpl implements UnifiedMessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ChatListMapper chatListMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserTypeSecurityService userTypeSecurityService;

    @Autowired
    private com.web.service.MessageCacheService messageCacheService;

    @Autowired
    private com.web.service.MessageRetryService messageRetryService;

    // 消息类型常量
    private static final String MESSAGE_TYPE_PRIVATE = "PRIVATE";
    private static final String MESSAGE_TYPE_GROUP = "GROUP";

    // 消息状态常量
    private static final int MESSAGE_STATUS_NORMAL = 0;
    private static final int MESSAGE_STATUS_DELETED = 1;
    private static final int MESSAGE_STATUS_RECALLED = 2;

    @Override
    public Message sendMessage(SendMessageVo sendMessageVo, Long userId) {
        try {
            // 统一消息格式验证
            com.web.util.MessageValidator.validateSendMessageVo(sendMessageVo);
            com.web.util.MessageValidator.validateUserId(userId);

            // 清理消息内容
            String sanitizedContent = com.web.util.MessageValidator.sanitizeContent(sendMessageVo.getContent());
            sendMessageVo.setContent(sanitizedContent);

            // 根据目标类型选择发送方式
            Message message;
            switch (sendMessageVo.getTargetType().toUpperCase()) {
                case MESSAGE_TYPE_PRIVATE:
                    message = sendPrivateMessage(sendMessageVo.getTargetId(), sendMessageVo.getContent(), userId);
                    break;
                case MESSAGE_TYPE_GROUP:
                    message = sendGroupMessage(sendMessageVo.getTargetId(), sendMessageVo.getContent(), userId);
                    break;
                default:
                    throw new WeebException("不支持的消息类型: " + sendMessageVo.getTargetType());
            }

            // 缓存消息
            if (message != null) {
                messageCacheService.cacheMessage(message);
                // 清除会话消息列表缓存，强制重新加载
                messageCacheService.evictMessageList(message.getChatId());
            }

            return message;

        } catch (WeebException e) {
            // 业务异常直接抛出，不重试
            throw e;
        } catch (Exception e) {
            log.error("发送消息失败: userId={}, targetType={}, targetId={}",
                userId, sendMessageVo.getTargetType(), sendMessageVo.getTargetId(), e);

            // 记录失败消息，稍后重试
            messageRetryService.recordFailedMessage(sendMessageVo, userId, e.getMessage());

            throw new WeebException("发送消息失败: " + e.getMessage());
        }
    }

    @Override
    public Message sendPrivateMessage(Long targetUserId, String content, Long senderId) {
        try {
            // 验证目标用户是否存在
            User targetUser = chatService.findUserById(targetUserId);
            if (targetUser == null) {
                throw new WeebException("目标用户不存在");
            }

            // 创建或获取私聊会话
            ChatList chatList = chatService.createChat(senderId, targetUserId);

            // 创建消息对象
            Message message = new Message();
            message.setChatId(chatList.getId());
            message.setSenderId(senderId);
            message.setReceiverId(targetUserId);
            message.setContent(content);
            message.setType(MESSAGE_TYPE_PRIVATE);
            message.setStatus(MESSAGE_STATUS_NORMAL);
            message.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            message.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            // 保存消息
            messageMapper.insertMessage(message);

            // 更新聊天列表
            chatListMapper.updateLastMessageAndUnreadCount(chatList.getId(), content);

            log.info("私聊消息发送成功: senderId={}, receiverId={}, messageId={}",
                senderId, targetUserId, message.getId());

            return message;

        } catch (Exception e) {
            log.error("发送私聊消息失败: senderId={}, receiverId={}", senderId, targetUserId, e);
            throw new WeebException("发送私聊消息失败: " + e.getMessage());
        }
    }

    @Override
    public Message sendGroupMessage(Long groupId, String content, Long senderId) {
        try {
            // 验证群组是否存在
            Group group = groupMapper.selectById(groupId);
            if (group == null) {
                throw new WeebException("群组不存在");
            }

            // 检查发送者是否为群组成员
            GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, senderId);
            if (member == null) {
                throw new WeebException("您不是该群组成员");
            }

            // 创建消息对象
            Message message = new Message();
            message.setChatId(groupId); // 群聊使用groupId作为chatId
            message.setSenderId(senderId);
            message.setReceiverId(groupId); // 群聊消息接收者为群组
            message.setContent(content);
            message.setType(MESSAGE_TYPE_GROUP);
            message.setStatus(MESSAGE_STATUS_NORMAL);
            message.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            message.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            // 保存消息
            messageMapper.insertMessage(message);

            // 更新群组成员的未读消息数（这里简化处理）
            updateGroupUnreadCounts(groupId, senderId);

            log.info("群聊消息发送成功: groupId={}, senderId={}, messageId={}",
                groupId, senderId, message.getId());

            return message;

        } catch (Exception e) {
            log.error("发送群聊消息失败: groupId={}, senderId={}", groupId, senderId, e);
            throw new WeebException("发送群聊消息失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUnifiedMessageList(Long userId, int page, int size) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 获取私聊消息
            List<Message> privateMessages = messageService.getUserMessages(userId, page, size);

            // 获取群聊消息
            List<Long> userGroupIds = groupMemberMapper.findGroupIdsByUserId(userId);
            List<Message> groupMessages = new ArrayList<>();
            for (Long groupId : userGroupIds) {
                List<Message> groupMsgs = messageMapper.selectMessagesByChatId(groupId, 0, size);
                groupMessages.addAll(groupMsgs);
            }

            // 合并并按时间排序
            List<Message> allMessages = new ArrayList<>();
            allMessages.addAll(privateMessages);
            allMessages.addAll(groupMessages);
            allMessages.sort((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()));

            // 分页处理
            int total = allMessages.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, total);

            List<Message> pageMessages = startIndex < total ?
                allMessages.subList(startIndex, endIndex) : new ArrayList<>();

            result.put("messages", pageMessages);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (int) Math.ceil((double) total / size));

            return result;

        } catch (Exception e) {
            log.error("获取统一消息列表失败: userId={}", userId, e);
            throw new WeebException("获取消息列表失败: " + e.getMessage());
        }
    }

    @Override
    public List<Message> getPrivateMessageHistory(Long userId, Long targetUserId, int page, int size) {
        try {
            // 验证参数
            com.web.util.MessageValidator.validateUserId(userId);
            com.web.util.MessageValidator.validateUserId(targetUserId);
            com.web.util.MessageValidator.validatePagination(page, size);

            // 检查私聊会话是否存在
            ChatList chatList = chatListMapper.selectChatListByUserAndTarget(userId, targetUserId);
            if (chatList == null) {
                return new ArrayList<>();
            }

            // 尝试从缓存获取
            List<Message> cachedMessages = messageCacheService.getCachedMessageList(chatList.getId(), page, size);
            if (cachedMessages != null) {
                log.debug("从缓存获取私聊消息: chatId={}, page={}, size={}", chatList.getId(), page, size);
                return cachedMessages;
            }

            // 从数据库查询
            int offset = (page - 1) * size;
            List<Message> messages = messageMapper.selectPrivateMessagesByUsers(userId, targetUserId, offset, size);

            // 缓存查询结果
            if (!messages.isEmpty()) {
                messageCacheService.cacheMessageList(chatList.getId(), messages);
            }

            // 预加载下一页
            if (messages.size() == size) {
                messageCacheService.preloadMessages(chatList.getId(), page + 1, size);
            }

            return messages;

        } catch (Exception e) {
            log.error("获取私聊消息历史失败: userId={}, targetUserId={}", userId, targetUserId, e);
            throw new WeebException("获取私聊消息历史失败: " + e.getMessage());
        }
    }

    @Override
    public List<Message> getGroupMessageHistory(Long groupId, Long userId, int page, int size) {
        try {
            // 验证参数
            com.web.util.MessageValidator.validateUserId(userId);
            com.web.util.MessageValidator.validatePagination(page, size);

            // 检查用户是否有权限查看群聊消息
            if (!hasGroupMessagePermission(groupId, userId)) {
                throw new WeebException("无权限查看该群聊消息");
            }

            // 尝试从缓存获取
            List<Message> cachedMessages = messageCacheService.getCachedMessageList(groupId, page, size);
            if (cachedMessages != null) {
                log.debug("从缓存获取群聊消息: groupId={}, page={}, size={}", groupId, page, size);
                return cachedMessages;
            }

            // 从数据库查询
            int offset = (page - 1) * size;
            List<Message> messages = messageMapper.selectMessagesByChatId(groupId, offset, size);

            // 缓存查询结果
            if (!messages.isEmpty()) {
                messageCacheService.cacheMessageList(groupId, messages);
            }

            // 预加载下一页
            if (messages.size() == size) {
                messageCacheService.preloadMessages(groupId, page + 1, size);
            }

            return messages;

        } catch (Exception e) {
            log.error("获取群聊消息历史失败: groupId={}, userId={}", groupId, userId, e);
            throw new WeebException("获取群聊消息历史失败: " + e.getMessage());
        }
    }

    @Override
    public boolean markMessageAsRead(Long messageId, Long userId) {
        try {
            Message message = messageMapper.selectById(messageId);
            if (message == null) {
                return false;
            }

            // 检查权限
            if (!hasMessagePermission(messageId, userId)) {
                return false;
            }

            // 根据消息类型标记已读
            if (MESSAGE_TYPE_PRIVATE.equals(message.getType())) {
                return markPrivateChatAsRead(message.getSenderId().equals(userId) ?
                    message.getReceiverId() : message.getSenderId(), userId);
            } else if (MESSAGE_TYPE_GROUP.equals(message.getType())) {
                return markGroupChatAsRead(message.getChatId(), userId);
            }

            return false;

        } catch (Exception e) {
            log.error("标记消息已读失败: messageId={}, userId={}", messageId, userId, e);
            return false;
        }
    }

    @Override
    public boolean markPrivateChatAsRead(Long targetUserId, Long userId) {
        try {
            ChatList chatList = chatListMapper.selectChatListByUserAndTarget(userId, targetUserId);
            if (chatList != null) {
                return chatListMapper.resetUnreadCountByChatId(chatList.getId()) > 0;
            }
            return false;
        } catch (Exception e) {
            log.error("标记私聊已读失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return false;
        }
    }

    @Override
    public boolean markGroupChatAsRead(Long groupId, Long userId) {
        try {
            // 这里应该实现群聊已读标记逻辑
            // 由于当前表结构限制，暂时返回true
            log.info("标记群聊已读: groupId={}, userId={}", groupId, userId);
            return true;
        } catch (Exception e) {
            log.error("标记群聊已读失败: groupId={}, userId={}", groupId, userId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getUnreadMessageStats(Long userId) {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 获取私聊未读数
            List<ChatList> chatLists = chatListMapper.selectChatListByUserId(userId);
            int privateUnread = chatLists.stream()
                .mapToInt(chat -> chat.getUnreadCount() != null ? chat.getUnreadCount() : 0)
                .sum();

            // 获取群聊未读数（简化处理）
            List<Long> groupIds = groupMemberMapper.findGroupIdsByUserId(userId);
            int groupUnread = 0; // 这里应该根据实际的群聊未读逻辑计算

            stats.put("privateUnread", privateUnread);
            stats.put("groupUnread", groupUnread);
            stats.put("totalUnread", privateUnread + groupUnread);

            return stats;

        } catch (Exception e) {
            log.error("获取未读消息统计失败: userId={}", userId, e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean deleteMessage(Long messageId, Long userId) {
        try {
            com.web.util.MessageValidator.validateMessageId(messageId);
            com.web.util.MessageValidator.validateUserId(userId);

            Message message = messageMapper.selectById(messageId);
            if (message == null) {
                return false;
            }

            // 检查权限（发送者或管理员可以删除）
            if (!canDeleteMessage(message, userId)) {
                return false;
            }

            // 软删除消息
            message.setStatus(MESSAGE_STATUS_DELETED);
            message.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            boolean result = messageMapper.updateById(message) > 0;

            // 清除缓存
            if (result) {
                messageCacheService.evictMessage(messageId);
                messageCacheService.evictMessageList(message.getChatId());
            }

            return result;

        } catch (Exception e) {
            log.error("删除消息失败: messageId={}, userId={}", messageId, userId, e);
            return false;
        }
    }

    @Override
    public boolean recallMessage(Long messageId, Long userId) {
        try {
            Message message = messageMapper.selectById(messageId);
            if (message == null) {
                return false;
            }

            // 只有发送者可以撤回消息
            if (!message.getSenderId().equals(userId)) {
                return false;
            }

            // 检查撤回时间限制（例如5分钟内）
            long recallTimeLimit = 5 * 60 * 1000; // 5分钟
            long timeDiff = System.currentTimeMillis() - message.getCreatedAt().getTime();
            if (timeDiff > recallTimeLimit) {
                return false;
            }

            // 撤回消息
            message.setStatus(MESSAGE_STATUS_RECALLED);
            message.setContent("[消息已撤回]");
            message.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            return messageMapper.updateById(message) > 0;

        } catch (Exception e) {
            log.error("撤回消息失败: messageId={}, userId={}", messageId, userId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> searchMessages(Long userId, String keyword, int page, int size) {
        try {
            // 验证参数
            com.web.util.MessageValidator.validateUserId(userId);
            com.web.util.MessageValidator.validateSearchKeyword(keyword);
            com.web.util.MessageValidator.validatePagination(page, size);

            Map<String, Object> result = new HashMap<>();

            // 清理搜索关键词
            String sanitizedKeyword = com.web.util.MessageValidator.sanitizeContent(keyword);

            // 搜索私聊消息
            List<Message> privateMessages = messageMapper.searchPrivateMessages(userId, sanitizedKeyword, page, size);

            // 搜索群聊消息
            List<Long> groupIds = groupMemberMapper.findGroupIdsByUserId(userId);
            List<Message> groupMessages = new ArrayList<>();
            for (Long groupId : groupIds) {
                List<Message> groupMsgs = messageMapper.searchGroupMessages(groupId, sanitizedKeyword, page, size);
                groupMessages.addAll(groupMsgs);
            }

            // 合并结果
            List<Message> allMessages = new ArrayList<>();
            allMessages.addAll(privateMessages);
            allMessages.addAll(groupMessages);
            allMessages.sort((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()));

            result.put("messages", allMessages);
            result.put("total", allMessages.size());
            result.put("keyword", sanitizedKeyword);
            result.put("page", page);
            result.put("size", size);

            return result;

        } catch (Exception e) {
            log.error("搜索消息失败: userId={}, keyword={}", userId, keyword, e);
            throw new WeebException("搜索消息失败: " + e.getMessage());
        }
    }

    @Override
    public Message getMessageById(Long messageId, Long userId) {
        try {
            Message message = messageMapper.selectById(messageId);
            if (message == null || !hasMessagePermission(messageId, userId)) {
                return null;
            }
            return message;
        } catch (Exception e) {
            log.error("获取消息详情失败: messageId={}, userId={}", messageId, userId, e);
            return null;
        }
    }

    @Override
    public boolean hasMessagePermission(Long messageId, Long userId) {
        try {
            Message message = messageMapper.selectById(messageId);
            if (message == null) {
                return false;
            }

            // 发送者和接收者都有权限查看消息
            if (message.getSenderId().equals(userId) || message.getReceiverId().equals(userId)) {
                return true;
            }

            // 管理员有所有权限
            User user = chatService.findUserById(userId);
            if (user != null && userTypeSecurityService.isAdmin(user.getUsername())) {
                return true;
            }

            // 群聊消息需要检查群组成员身份
            if (MESSAGE_TYPE_GROUP.equals(message.getType())) {
                return hasGroupMessagePermission(message.getChatId(), userId);
            }

            return false;

        } catch (Exception e) {
            log.error("检查消息权限失败: messageId={}, userId={}", messageId, userId, e);
            return false;
        }
    }

    @Override
    public List<ChatList> getUnifiedChatList(Long userId) {
        try {
            // 获取私聊列表
            List<ChatList> privateChats = chatService.getChatList(userId);

            // 获取群聊列表（需要创建对应的ChatList记录）
            List<Long> groupIds = groupMemberMapper.findGroupIdsByUserId(userId);
            List<ChatList> groupChats = new ArrayList<>();

            for (Long groupId : groupIds) {
                Group group = groupMapper.selectById(groupId);
                if (group != null) {
                    ChatList groupChat = new ChatList();
                    groupChat.setId(groupId.toString());
                    groupChat.setUserId(userId);
                    groupChat.setTargetId(groupId);
                    groupChat.setType(MESSAGE_TYPE_GROUP);
                    groupChat.setTargetInfo(group.getGroupName());
                    groupChat.setUnreadCount(0); // 这里需要实际计算

                    // 获取最后一条消息
                    Message lastMessage = messageMapper.selectLastMessageByChatId(groupId);
                    groupChat.setLastMessage(lastMessage != null ? lastMessage.getContent().toString() : "");

                    groupChats.add(groupChat);
                }
            }

            // 合并并按时间排序
            List<ChatList> allChats = new ArrayList<>();
            allChats.addAll(privateChats);
            allChats.addAll(groupChats);

            // 按最后消息时间排序（这里简化处理）
            allChats.sort((c1, c2) -> {
                if (c1.getLastMessage() == null && c2.getLastMessage() == null) return 0;
                if (c1.getLastMessage() == null) return 1;
                if (c2.getLastMessage() == null) return -1;
                return c2.getLastMessage().compareTo(c1.getLastMessage());
            });

            return allChats;

        } catch (Exception e) {
            log.error("获取统一聊天列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查用户是否有群聊消息权限
     */
    private boolean hasGroupMessagePermission(Long groupId, Long userId) {
        try {
            // 管理员有所有权限
            User user = chatService.findUserById(userId);
            if (user != null && userTypeSecurityService.isAdmin(user.getUsername())) {
                return true;
            }

            // 检查是否为群组成员
            GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, userId);
            return member != null;
        } catch (Exception e) {
            log.error("检查群聊消息权限失败: groupId={}, userId={}", groupId, userId, e);
            return false;
        }
    }

    /**
     * 检查用户是否可以删除消息
     */
    private boolean canDeleteMessage(Message message, Long userId) {
        try {
            // 发送者可以删除自己的消息
            if (message.getSenderId().equals(userId)) {
                return true;
            }

            // 管理员可以删除任何消息
            User user = chatService.findUserById(userId);
            if (user != null && userTypeSecurityService.isAdmin(user.getUsername())) {
                return true;
            }

            // 群聊中，群主和管理员可以删除群聊消息
            if (MESSAGE_TYPE_GROUP.equals(message.getType())) {
                GroupMember member = groupMemberMapper.findByGroupAndUser(message.getChatId(), userId);
                return member != null && member.getRole() >= 1; // 群主或管理员
            }

            return false;

        } catch (Exception e) {
            log.error("检查删除消息权限失败: messageId={}, userId={}", message.getId(), userId, e);
            return false;
        }
    }

    /**
     * 更新群组成员的未读消息数
     */
    private void updateGroupUnreadCounts(Long groupId, Long excludeUserId) {
        try {
            List<GroupMember> members = groupMemberMapper.findMembersByGroupId(groupId);
            for (GroupMember member : members) {
                if (!member.getUserId().equals(excludeUserId)) {
                    // 这里应该实现群聊未读数更新逻辑
                    log.debug("更新群聊未读数: groupId={}, memberId={}", groupId, member.getUserId());
                }
            }
        } catch (Exception e) {
            log.error("更新群聊未读数失败: groupId={}", groupId, e);
        }
    }
}