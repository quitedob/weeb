package com.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.web.exception.WeebException;
import com.web.mapper.ChatListMapper;
import com.web.mapper.GroupMapper;
import com.web.mapper.GroupMemberMapper;
import com.web.mapper.MessageMapper;
import com.web.model.ChatList;
import com.web.model.Group;
import com.web.model.GroupMember;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.*;
import com.web.util.MessageValidator;
import com.web.vo.message.SendMessageVo;
import com.web.vo.message.TextMessageContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Alternate message and retry entry points use the same persisted chat membership as HTTP/STOMP. */
@Service
@Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class UnifiedMessageServiceImpl implements UnifiedMessageService {
    @Autowired private MessageMapper messageMapper;
    @Autowired private ChatListMapper chatListMapper;
    @Autowired private GroupMapper groupMapper;
    @Autowired private GroupMemberMapper groupMemberMapper;
    @Autowired private ChatService chatService;
    @Autowired private ChatAccessService chatAccessService;
    @Autowired private UserService userService;
    @Autowired private UserPreferencesService userPreferencesService;
    @Autowired private MessageCacheService messageCacheService;
    @Autowired private MessageRetryService messageRetryService;

    @Override
    public Message sendMessage(SendMessageVo request, Long userId) {
        MessageValidator.validateSendMessageVo(request);
        requireActiveSender(userId);
        if (request.getClientMessageId() == null) request.setClientMessageId(UUID.randomUUID().toString());
        String content = MessageValidator.sanitizeContent(request.getContent().toString());
        try {
            Message message = "GROUP".equalsIgnoreCase(request.getTargetType())
                    ? sendGroupMessage(request.getTargetId(), content, userId, request.getClientMessageId())
                    : sendPrivateMessage(request.getTargetId(), content, userId, request.getClientMessageId());
            messageCacheService.cacheMessage(message);
            messageCacheService.evictMessageList(message.getChatId());
            return message;
        } catch (AccessDeniedException | WeebException | IllegalArgumentException e) {
            // A denied request must never be persisted as a retry that can bypass current membership/privacy.
            throw e;
        } catch (RuntimeException e) {
            if (!request.isRetryAttempt()) messageRetryService.recordFailedMessage(request, userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public Message sendPrivateMessage(Long targetUserId, String content, Long senderId) {
        return sendPrivateMessage(targetUserId, content, senderId, null);
    }

    public Message sendPrivateMessage(Long targetUserId, String content, Long senderId, String clientMessageId) {
        requireActiveSender(senderId);
        MessageValidator.validateUserId(targetUserId);
        MessageValidator.validateMessageContent(content);
        if (!userPreferencesService.canReceiveMessages(targetUserId)) {
            throw new AccessDeniedException("Recipient does not accept private messages");
        }
        ChatList chat = chatService.createChat(senderId, targetUserId);
        chatAccessService.requireAccess(senderId, chat.getSharedChatId());
        return chatService.sendMessage(senderId, chat.getId(), newMessage(content, senderId, clientMessageId));
    }

    @Override
    public Message sendGroupMessage(Long groupId, String content, Long senderId) {
        return sendGroupMessage(groupId, content, senderId, null);
    }

    public Message sendGroupMessage(Long groupId, String content, Long senderId, String clientMessageId) {
        requireActiveSender(senderId);
        MessageValidator.validateMessageContent(content);
        Long sharedChatId = requireGroupChat(groupId, senderId);
        return chatService.sendMessageBySharedChatId(senderId, sharedChatId,
                newMessage(content, senderId, clientMessageId));
    }

    private Message newMessage(String text, Long senderId, String clientId) {
        TextMessageContent content = new TextMessageContent();
        content.setContent(text);
        content.setContentType(1);
        Message message = new Message();
        message.setSenderId(senderId);
        message.setClientMessageId(clientId);
        message.setContent(content);
        message.setMessageType(1);
        message.setStatus(Message.STATUS_SENT);
        message.setIsRecalled(0);
        return message;
    }

    @Override
    public Map<String, Object> getUnifiedMessageList(Long userId, int page, int size) {
        return queryMessages(userId, null, page, size);
    }

    @Override
    public Map<String, Object> searchMessages(Long userId, String keyword, int page, int size) {
        MessageValidator.validateSearchKeyword(keyword);
        return queryMessages(userId, MessageValidator.sanitizeContent(keyword), page, size);
    }

    private Map<String, Object> queryMessages(Long userId, String keyword, int page, int size) {
        MessageValidator.validateUserId(userId);
        MessageValidator.validatePagination(page, size);
        Set<Long> chatIds = readableChatIds(userId);
        long total = 0;
        List<Message> messages = List.of();
        if (!chatIds.isEmpty()) {
            QueryWrapper<Message> query = new QueryWrapper<Message>().in("chat_id", chatIds);
            if (keyword != null) {
                query.apply("JSON_UNQUOTE(JSON_EXTRACT(content, '$.content')) LIKE {0}", "%" + keyword + "%");
            }
            total = messageMapper.selectCount(query);
            query.orderByDesc("created_at").orderByDesc("id").last("LIMIT " + (long) (page - 1) * size + ", " + size);
            messages = messageMapper.selectList(query);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("messages", messages);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (total + size - 1) / size);
        if (keyword != null) result.put("keyword", keyword);
        return result;
    }

    private Set<Long> readableChatIds(Long userId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ChatList chat : chatService.getChatList(userId)) {
            if (canRead(userId, chat.getSharedChatId())) ids.add(chat.getSharedChatId());
        }
        List<GroupMember> groups = groupMemberMapper.selectList(new QueryWrapper<GroupMember>()
                .eq("user_id", userId).eq("join_status", "ACCEPTED").isNull("kicked_at"));
        for (GroupMember member : groups) {
            Group group = groupMapper.selectById(member.getGroupId());
            if (group != null && canRead(userId, group.getSharedChatId())) ids.add(group.getSharedChatId());
        }
        return ids;
    }

    @Override
    public List<Message> getPrivateMessageHistory(Long userId, Long targetUserId, int page, int size) {
        MessageValidator.validateUserId(userId);
        MessageValidator.validateUserId(targetUserId);
        ChatList chat = chatListMapper.selectChatListByUserAndTarget(userId, targetUserId);
        if (chat == null) return List.of();
        chatAccessService.requireAccess(userId, chat.getSharedChatId());
        return history(chat.getSharedChatId(), page, size);
    }

    @Override
    public List<Message> getGroupMessageHistory(Long groupId, Long userId, int page, int size) {
        return history(requireGroupChat(groupId, userId), page, size);
    }

    private List<Message> history(Long sharedChatId, int page, int size) {
        MessageValidator.validatePagination(page, size);
        List<Message> cached = messageCacheService.getCachedMessageList(sharedChatId, page, size);
        if (cached != null && cached.stream().allMatch(m -> sharedChatId.equals(m.getChatId()))) return cached;
        List<Message> messages = messageMapper.selectMessagesBySharedChatId(sharedChatId, (page - 1) * size, size);
        if (page == 1 && !messages.isEmpty()) messageCacheService.cacheMessageList(sharedChatId, messages);
        return messages;
    }

    @Override
    public boolean markMessageAsRead(Long messageId, Long userId) {
        Message message = requireMessage(messageId, userId);
        chatService.markAsReadBySharedChatId(userId, message.getChatId(), messageId);
        return true;
    }

    @Override
    public boolean markPrivateChatAsRead(Long targetUserId, Long userId) {
        ChatList chat = chatListMapper.selectChatListByUserAndTarget(userId, targetUserId);
        if (chat == null) return false;
        chatAccessService.requireAccess(userId, chat.getSharedChatId());
        return chatService.markAsReadBySharedChatId(userId, chat.getSharedChatId());
    }

    @Override
    public boolean markGroupChatAsRead(Long groupId, Long userId) {
        return chatService.markAsReadBySharedChatId(userId, requireGroupChat(groupId, userId));
    }

    @Override
    public Map<String, Object> getUnreadMessageStats(Long userId) {
        int privateUnread = 0;
        int groupUnread = 0;
        for (ChatList chat : getUnifiedChatList(userId)) {
            int count = chat.getUnreadCount() == null ? 0 : chat.getUnreadCount();
            if ("GROUP".equals(chat.getType())) groupUnread += count;
            else privateUnread += count;
        }
        return Map.of("privateUnread", privateUnread, "groupUnread", groupUnread, "totalUnread", privateUnread + groupUnread);
    }

    @Override
    public boolean deleteMessage(Long messageId, Long userId) {
        Message message = requireMessage(messageId, userId);
        if (!userId.equals(message.getSenderId())) return false;
        // Keep the historical soft-delete representation; permission requires both sender and current membership.
        message.setIsRead(1);
        message.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        boolean result = messageMapper.updateById(message) > 0;
        if (result) evict(message);
        return result;
    }

    @Override
    public boolean recallMessage(Long messageId, Long userId) {
        Message message = requireMessage(messageId, userId);
        if (!userId.equals(message.getSenderId())) return false;
        boolean result = chatService.recallMessage(userId, messageId);
        if (result) evict(message);
        return result;
    }

    private void evict(Message message) {
        messageCacheService.evictMessage(message.getId());
        messageCacheService.evictMessageList(message.getChatId());
    }

    @Override
    public Message getMessageById(Long messageId, Long userId) {
        return requireMessage(messageId, userId);
    }

    @Override
    public boolean hasMessagePermission(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        return message != null && canRead(userId, message.getChatId());
    }

    @Override
    public List<ChatList> getUnifiedChatList(Long userId) {
        MessageValidator.validateUserId(userId);
        return chatService.getChatList(userId).stream()
                .filter(chat -> canRead(userId, chat.getSharedChatId())).toList();
    }

    private Message requireMessage(Long messageId, Long userId) {
        MessageValidator.validateMessageId(messageId);
        Message message = messageMapper.selectById(messageId);
        if (message == null) throw new WeebException("Message not found");
        chatAccessService.requireAccess(userId, message.getChatId());
        return message;
    }

    private Long requireGroupChat(Long groupId, Long userId) {
        Group group = groupId == null ? null : groupMapper.selectById(groupId);
        if (group == null) throw new AccessDeniedException("Group conversation unavailable");
        chatAccessService.requireAccess(userId, group.getSharedChatId());
        return group.getSharedChatId();
    }

    private boolean canRead(Long userId, Long chatId) {
        try {
            chatAccessService.requireAccess(userId, chatId);
            return true;
        } catch (AccessDeniedException e) {
            return false;
        }
    }

    private void requireActiveSender(Long userId) {
        MessageValidator.validateUserId(userId);
        User user = userService.getUserBasicInfo(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new AccessDeniedException("Sender account unavailable");
        }
    }
}
