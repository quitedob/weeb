package com.web.controller;

import com.web.config.UserInfoArgumentResolver;
import com.web.constant.GroupRoleConstants;
import com.web.exception.GlobalExceptionHandler;
import com.web.mapper.*;
import com.web.model.*;
import com.web.service.*;
import com.web.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GroupApplicationForbiddenRegressionTest {
    private final GroupMapper groups = mock(GroupMapper.class);
    private final GroupMemberMapper members = mock(GroupMemberMapper.class);
    private final GroupApplicationMapper applications = mock(GroupApplicationMapper.class);
    private final AuthService auth = mock(AuthService.class);
    private final UserTypeSecurityService roles = mock(UserTypeSecurityService.class);
    private final ChatListMapper chats = mock(ChatListMapper.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final MessageBroadcastService broadcasts = mock(MessageBroadcastService.class);
    private final Group group = new Group();
    private final GroupApplication persisted = new GroupApplication(10L, 2L, "Please approve");
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        group.setId(10L);
        group.setOwnerId(1L);
        group.setMemberCount(1);
        group.setSharedChatId(100L);
        persisted.setId(20L);
        when(groups.selectById(10L)).thenReturn(group);
        when(applications.selectById(20L)).thenAnswer(call -> {
            GroupApplication snapshot = new GroupApplication(persisted.getGroupId(), persisted.getUserId(), persisted.getMessage());
            snapshot.setId(persisted.getId());
            snapshot.setStatus(persisted.getStatus());
            return snapshot;
        });
        when(applications.update(any(GroupApplication.class), any())).thenAnswer(call -> {
            GroupApplication decision = call.getArgument(0);
            persisted.setStatus(decision.getStatus());
            persisted.setReviewerId(decision.getReviewerId());
            persisted.setReviewedAt(decision.getReviewedAt());
            return 1;
        });
        for (long userId : new long[] {1L, 3L}) {
            User user = new User();
            user.setId(userId);
            user.setUsername("user" + userId);
            user.setType("USER");
            user.setStatus(1);
            when(auth.findByUserID(userId)).thenReturn(user);
        }
        GroupMember owner = new GroupMember();
        owner.setGroupId(10L);
        owner.setUserId(1L);
        owner.setRole(GroupRoleConstants.ROLE_OWNER);
        owner.setJoinStatus("ACCEPTED");
        when(members.findByGroupAndUser(10L, 1L)).thenReturn(owner);
        when(chats.insertChatList(any())).thenReturn(1);
        when(members.insert(any(GroupMember.class))).thenReturn(1);
        when(groups.incrementMemberCountIfCapacity(10L)).thenAnswer(call -> {
            group.setMemberCount(group.getMemberCount() + 1);
            return 1;
        });

        var service = new GroupServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", groups);
        ReflectionTestUtils.setField(service, "groupMapper", groups);
        ReflectionTestUtils.setField(service, "groupMemberMapper", members);
        ReflectionTestUtils.setField(service, "groupApplicationMapper", applications);
        ReflectionTestUtils.setField(service, "authService", auth);
        ReflectionTestUtils.setField(service, "userTypeSecurityService", roles);
        ReflectionTestUtils.setField(service, "chatListMapper", chats);
        ReflectionTestUtils.setField(service, "notificationService", notifications);
        ReflectionTestUtils.setField(service, "messageBroadcastService", broadcasts);
        var controller = new GroupController();
        ReflectionTestUtils.setField(controller, "groupService", service);
        mvc = standaloneSetup(controller).setCustomArgumentResolvers(new UserInfoArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void outsiderDecisionReturnsForbiddenWithoutChangingPendingRequestOrMembership(String action) throws Exception {
        decide(action, 3L).andExpect(status().isForbidden());
        assertPendingUnchanged();
        verify(auth).findByUserID(3L);
        verifyNoInteractions(applications);
        verifyNoMutation();
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void ownerCannotDecideAnotherGroupsApplication(String action) throws Exception {
        persisted.setGroupId(99L);
        decide(action, 1L).andExpect(status().isForbidden());
        assertPendingUnchanged();
        assertEquals(99L, persisted.getGroupId());
        verify(applications, never()).update(any(GroupApplication.class), any());
        verifyNoMutation();
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void ownerCanDecideApplicationInTheirOwnGroup(String action) throws Exception {
        decide(action, 1L).andExpect(status().isOk());
        assertEquals("approve".equals(action) ? "APPROVED" : "REJECTED", persisted.getStatus());
        assertEquals(1L, persisted.getReviewerId());
        assertNotNull(persisted.getReviewedAt());
        verify(applications).update(any(GroupApplication.class), any());
        verify(applications, never()).updateById(any(GroupApplication.class));
        if ("approve".equals(action)) {
            verify(members).insert(argThat((GroupMember member) -> member.getGroupId().equals(10L)
                    && member.getUserId().equals(2L) && "ACCEPTED".equals(member.getJoinStatus())));
            assertEquals(2, group.getMemberCount());
            verify(groups).incrementMemberCountIfCapacity(10L);
            verify(groups, never()).updateById(any(Group.class));
        } else {
            verify(members, never()).insert(any(GroupMember.class));
            assertEquals(1, group.getMemberCount());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void losingConcurrentDecisionCannotAddMemberOrSendNotification(String action) throws Exception {
        doReturn(0).when(applications).update(any(GroupApplication.class), any());
        decide(action, 1L).andExpect(status().isBadRequest());
        assertPendingUnchanged();
        verifyNoMutation();
    }

    private ResultActions decide(String action, Long actor) throws Exception {
        return mvc.perform(put("/api/groups/10/applications/20")
                .requestAttr("userinfo", Map.of("userId", actor)).param("userId", "1")
                .contentType("application/json")
                .content("{\"action\":\"" + action + "\",\"reason\":\"contract verification\"}"));
    }

    private void assertPendingUnchanged() {
        assertEquals("PENDING", persisted.getStatus());
        assertNull(persisted.getReviewerId());
        assertNull(persisted.getReviewedAt());
        assertEquals(1, group.getMemberCount());
    }

    private void verifyNoMutation() {
        verify(members, never()).insert(any(GroupMember.class));
        verify(groups, never()).updateById(any(Group.class));
        verify(groups, never()).incrementMemberCountIfCapacity(anyLong());
        verifyNoInteractions(chats, notifications, broadcasts);
    }
}
