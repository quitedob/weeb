package com.web.service;

import com.web.mapper.ChatListMapper;
import com.web.mapper.MessageMapper;
import com.web.model.ChatList;
import com.web.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatListIdentityAndUnreadTest {
    @Test
    void groupsWithoutTargetIdsRemainVisibleAndUseCanonicalUnreadCounts() {
        var chats = mock(ChatListMapper.class);
        var unread = mock(ChatUnreadCountService.class);
        var service = new ChatServiceImpl();
        ReflectionTestUtils.setField(service, "chatListMapper", chats);
        ReflectionTestUtils.setField(service, "messageMapper", mock(MessageMapper.class));
        ReflectionTestUtils.setField(service, "chatAccessService", new ChatAccessService(chats));
        ReflectionTestUtils.setField(service, "chatUnreadCountService", unread);
        ChatList group = chat("group-current", 50L, null, "GROUP");
        group.setGroupId(2L);
        ChatList duplicate = chat("group-old", 50L, null, "GROUP");
        duplicate.setGroupId(2L);
        duplicate.setUpdateTime(group.getUpdateTime().minusDays(1));
        ChatList direct = chat("private", 60L, 2L, "PRIVATE");
        ChatList kicked = chat("kicked", 70L, null, "GROUP");
        when(chats.selectChatListByUserId(1L)).thenReturn(List.of(duplicate, group, direct, kicked));
        when(chats.canUserAccessSharedChat(1L, 50L)).thenReturn(true);
        when(chats.canUserAccessSharedChat(1L, 60L)).thenReturn(true);
        when(unread.getUnreadCount(1L, 50L)).thenReturn(7);
        when(unread.getUnreadCount(1L, 60L)).thenReturn(3);
        var list = service.getChatList(1L);
        assertEquals(List.of(group, direct), list);
        assertEquals(7, group.getUnreadCount());
        assertEquals(3, direct.getUnreadCount());
        verify(unread, times(1)).getUnreadCount(1L, 50L);
        verify(unread, never()).getUnreadCount(1L, 70L);
    }

    private ChatList chat(String id, long shared, Long target, String type) {
        var chat = new ChatList();
        chat.setId(id);
        chat.setUserId(1L);
        chat.setSharedChatId(shared);
        chat.setTargetId(target);
        chat.setType(type);
        chat.setUnreadCount(0);
        chat.setUpdateTime(LocalDateTime.now());
        return chat;
    }
}
