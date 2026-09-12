package com.web.service;

import com.web.mapper.ChatListMapper;
import com.web.model.ChatList;
import com.web.model.Group;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Resolves client chat identifiers only after checking persisted membership. */
@Service
public class ChatAccessService {
    private final ChatListMapper chatListMapper;

    public ChatAccessService(ChatListMapper chatListMapper) {
        this.chatListMapper = chatListMapper;
    }

    public void requireAccess(Long userId, Long sharedChatId) {
        if (userId == null || sharedChatId == null || userId <= 0 || sharedChatId <= 0
                || !chatListMapper.canUserAccessSharedChat(userId, sharedChatId)) {
            throw new AccessDeniedException("无权访问该聊天");
        }
    }

    public ChatList requireOwnedChat(Long userId, String chatId) {
        ChatList chat = chatId == null ? null : chatListMapper.selectChatListByIdString(chatId);
        if (chat == null || userId == null || !userId.equals(chat.getUserId())) {
            throw new AccessDeniedException("无权访问该聊天");
        }
        requireAccess(userId, chat.getSharedChatId());
        return chat;
    }

    /** group_ identifies a group, private_ identifies a shared private chat. */
    public Long resolveRoom(Long userId, String roomId) {
        if (roomId == null || roomId.isBlank() || roomId.contains("/") || roomId.contains("*")) {
            throw new AccessDeniedException("无效的聊天室");
        }
        Long sharedChatId;
        try {
            if (roomId.startsWith("group_")) {
                Group group = chatListMapper.selectGroupById(Long.valueOf(roomId.substring(6)));
                sharedChatId = group == null ? null : group.getSharedChatId();
            } else if (roomId.startsWith("private_")) {
                sharedChatId = Long.valueOf(roomId.substring(8));
                if (chatListMapper.selectGroupBySharedChatId(sharedChatId) != null) {
                    throw new AccessDeniedException("无效的私聊聊天室");
                }
            } else if (roomId.matches("[0-9]+")) {
                sharedChatId = Long.valueOf(roomId);
            } else {
                return requireOwnedChat(userId, roomId).getSharedChatId();
            }
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("无效的聊天室");
        }
        requireAccess(userId, sharedChatId);
        return sharedChatId;
    }
}
