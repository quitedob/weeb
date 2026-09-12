package com.web.service;

import com.web.mapper.ChatListMapper;
import com.web.mapper.GroupMapper;
import com.web.mapper.GroupMemberMapper;
import com.web.model.Group;
import com.web.service.impl.GroupServiceImpl;
import com.web.vo.group.GroupCreateVo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroupChatIdentityRegressionTest {
    @Test
    void creationUsesPersistedSharedIdInsteadOfInsertRowCount() {
        GroupMapper groups = mock(GroupMapper.class);
        ChatListMapper chats = mock(ChatListMapper.class);
        GroupServiceImpl service = new GroupServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", groups);
        ReflectionTestUtils.setField(service, "groupMemberMapper", mock(GroupMemberMapper.class));
        ReflectionTestUtils.setField(service, "chatListMapper", chats);
        when(groups.insert(any(Group.class))).thenAnswer(call -> {
            Group group = call.getArgument(0);
            group.setId(90L);
            return 1;
        });
        when(chats.findGroupSharedChatId(90L)).thenReturn(null, 105L);
        when(chats.createGroupSharedChat(90L)).thenReturn(1);
        when(chats.insertChatList(any())).thenReturn(1);
        GroupCreateVo request = new GroupCreateVo();
        request.setGroupName("Team");

        Group group = service.createGroup(request, 1L);

        assertEquals(105L, group.getSharedChatId());
        verify(chats).createGroupSharedChat(90L);
        verify(groups).updateById(argThat((Group g) -> g.getSharedChatId().equals(105L)));
        verify(chats).insertChatList(argThat(c -> c.getGroupId().equals(90L) && c.getSharedChatId().equals(105L)));
    }
}
