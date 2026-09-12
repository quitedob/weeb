package com.web.controller;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.web.config.UserInfoArgumentResolver;
import com.web.exception.GlobalExceptionHandler;
import com.web.mapper.*;
import com.web.model.*;
import com.web.service.*;
import com.web.service.impl.GroupServiceImpl;
import com.web.vo.group.GroupInviteVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GroupRoleAuthorizationRegressionTest {
    private final GroupMapper groups = mock(GroupMapper.class);
    private final GroupMemberMapper members = mock(GroupMemberMapper.class);
    private final AuthService auth = mock(AuthService.class);
    private final UserTypeSecurityService roles = mock(UserTypeSecurityService.class);
    private final MessageBroadcastService broadcasts = mock(MessageBroadcastService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final ChatListMapper chats = mock(ChatListMapper.class);
    private final Map<Long, GroupMember> membership = new HashMap<>();
    private final Map<Long, User> users = new HashMap<>();
    private final Group group = new Group();
    private final GroupServiceImpl service = new GroupServiceImpl();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        group.setId(10L);
        group.setOwnerId(1L);
        group.setGroupName("Before");
        group.setGroupDescription("Original description");
        group.setMemberCount(4);
        for (long id = 1; id <= 4; id++) {
            GroupMember member = new GroupMember(10L, id, id == 1 ? 1 : id == 2 ? 2 : 3);
            member.setId(100L + id);
            membership.put(id, member);
            User user = new User();
            user.setId(id);
            user.setUsername("group-role-user-" + id);
            user.setStatus(1);
            user.setType("USER");
            users.put(id, user);
        }
        when(groups.selectById(10L)).thenReturn(group);
        when(auth.findByUserID(anyLong())).thenAnswer(call -> users.get(call.getArgument(0)));
        when(members.findByGroupAndUser(eq(10L), anyLong())).thenAnswer(call -> membership.get(call.getArgument(1)));
        when(members.update(isNull(), any())).thenReturn(1);
        when(members.delete(any())).thenReturn(1);
        when(groups.update(isNull(), any())).thenReturn(1);
        when(groups.decrementMemberCount(10L)).thenReturn(1);
        ReflectionTestUtils.setField(service, "baseMapper", groups);
        ReflectionTestUtils.setField(service, "groupMapper", groups);
        ReflectionTestUtils.setField(service, "groupMemberMapper", members);
        ReflectionTestUtils.setField(service, "authService", auth);
        ReflectionTestUtils.setField(service, "userTypeSecurityService", roles);
        ReflectionTestUtils.setField(service, "messageBroadcastService", broadcasts);
        ReflectionTestUtils.setField(service, "notificationService", notifications);
        ReflectionTestUtils.setField(service, "chatListMapper", chats);
        var controller = new GroupController();
        ReflectionTestUtils.setField(controller, "groupService", service);
        mvc = standaloneSetup(controller).setCustomArgumentResolvers(new UserInfoArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "BLOCKED", "LEFT", "KICKED", "DISABLED", "ABSENT"})
    void inactiveOwnersCannotEditPromoteOrRemove(String state) throws Exception {
        invalidate(1L, state);
        mvc.perform(put("/api/groups/10").requestAttr("userinfo", actor(1L))
                .contentType("application/json").content("{\"groupName\":\"Changed\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/groups/10/members/3/role").requestAttr("userinfo", actor(1L))
                .contentType("application/json").content("{\"role\":\"admin\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/groups/10/members/3").requestAttr("userinfo", actor(1L)))
                .andExpect(status().isForbidden());
        verifyNoMutation();
    }

    @Test
    void ordinaryMemberCannotEditGroupOrGrantAdministratorOrRemoveAnotherMember() throws Exception {
        mvc.perform(put("/api/groups/10").requestAttr("userinfo", actor(3L))
                .contentType("application/json").content("{\"groupName\":\"Changed\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/groups/10/members/4/role").requestAttr("userinfo", actor(3L))
                .contentType("application/json").content("{\"role\":\"admin\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/groups/10/members/4").requestAttr("userinfo", actor(3L)))
                .andExpect(status().isForbidden());
        verifyNoMutation();
    }

    @ParameterizedTest
    @CsvSource({"1,3", "2,3", "1,2"})
    void authorizedRemovalDeletesOnlyOnceAndUsesAtomicCount(long actor, long target) throws Exception {
        mvc.perform(delete("/api/groups/10/members/" + target).requestAttr("userinfo", actor(actor)))
                .andExpect(status().isNoContent());
        verify(members).delete(any());
        verify(groups).decrementMemberCount(10L);
        verify(groups, never()).updateById(any(Group.class));
        assertEquals(4, group.getMemberCount(), "The loaded group snapshot must not be written back");
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,1", "3,1", "2,2"})
    void ownerAndAdministratorTargetsAreProtected(long actor, long target) throws Exception {
        mvc.perform(delete("/api/groups/10/members/" + target).requestAttr("userinfo", actor(actor)))
                .andExpect(status().isForbidden());
        verifyNoMutation();
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "member"})
    void ownerCannotBeDemotedAndAdministratorCannotAssignRoles(String role) throws Exception {
        mvc.perform(put("/api/groups/10/members/1/role").requestAttr("userinfo", actor(1L))
                .contentType("application/json").content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/groups/10/members/3/role").requestAttr("userinfo", actor(2L))
                .contentType("application/json").content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isForbidden());
        verifyNoMutation();
    }

    @ParameterizedTest
    @CsvSource({"admin,3,2", "member,2,3"})
    void ownerCanPromoteAndDemoteUsingCanonicalRoleValues(String role, long target, int expectedRole) throws Exception {
        mvc.perform(put("/api/groups/10/members/" + target + "/role").requestAttr("userinfo", actor(1L))
                .contentType("application/json").content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk());
        verify(members).update(isNull(), argThat(wrapper -> {
            UpdateWrapper<?> update = (UpdateWrapper<?>) wrapper;
            return update.getSqlSet().startsWith("role=")
                    && update.getParamNameValuePairs().containsValue(expectedRole);
        }));
        verify(members, never()).updateById(any(GroupMember.class));
    }

    @Test
    void unsupportedRoleCannotCreateAnotherOwner() throws Exception {
        mvc.perform(put("/api/groups/10/members/3/role").requestAttr("userinfo", actor(1L))
                .contentType("application/json").content("{\"role\":\"owner\"}"))
                .andExpect(status().isBadRequest());
        verifyNoMutation();
    }

    @Test
    void administratorCanEditNameAndDescriptionWithoutWritingCounterOrOwnerSnapshot() throws Exception {
        mvc.perform(put("/api/groups/10").requestAttr("userinfo", actor(2L))
                .contentType("application/json").content("{\"groupName\":\"Changed\",\"groupDescription\":\"New description\"}"))
                .andExpect(status().isOk());
        verify(groups).update(isNull(), argThat(wrapper -> {
            String fields = ((UpdateWrapper<?>) wrapper).getSqlSet();
            return fields.contains("group_name=") && fields.contains("group_description=")
                    && !fields.contains("member_count") && !fields.contains("owner_id");
        }));
        verify(groups, never()).updateById(any(Group.class));
    }

    @Test
    void lostRemovalRaceCannotDecrementCountOrBroadcast() throws Exception {
        when(members.delete(any())).thenReturn(0);
        mvc.perform(delete("/api/groups/10/members/3").requestAttr("userinfo", actor(1L)))
                .andExpect(status().isBadRequest());
        verify(groups, never()).decrementMemberCount(anyLong());
        verifyNoInteractions(broadcasts);
    }

    @Test
    void batchInvitationKeepsOnlyTheAcceptedFirstMemberWhenCapacityRunsOut() {
        group.setMemberCount(1);
        group.setMaxMembers(2);
        membership.remove(3L);
        membership.remove(4L);
        AtomicInteger persistedCount = new AtomicInteger(1);
        when(groups.incrementMemberCountIfCapacity(10L)).thenAnswer(call ->
                persistedCount.compareAndSet(1, 2) ? 1 : 0);
        when(members.insert(any(GroupMember.class))).thenAnswer(call -> {
            GroupMember member = call.getArgument(0);
            membership.put(member.getUserId(), member);
            return 1;
        });
        when(chats.insertChatList(any())).thenReturn(1);
        GroupInviteVo request = new GroupInviteVo();
        request.setGroupId(10L);
        request.setMemberIds(List.of(3L, 4L));

        assertTrue(service.inviteMembers(request, 1L));

        assertEquals(2, persistedCount.get());
        assertEquals("ACCEPTED", membership.get(3L).getJoinStatus());
        assertFalse(membership.containsKey(4L));
        verify(members, times(1)).insert(any(GroupMember.class));
        verify(notifications).createAndPublishNotification(3L, 1L, "GROUP_INVITE", "GROUP", 10L);
        verify(broadcasts).broadcastGroupMemberChange(10L, "MEMBER_ADDED", 3L, 1L, null);
        verifyNoMoreInteractions(notifications, broadcasts);
        verify(groups, never()).updateById(any(Group.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "LEFT", "KICKED", "DISABLED", "ABSENT"})
    void ownershipCannotTransferToInactiveTarget(String state) {
        invalidate(3L, state);
        assertThrows(AccessDeniedException.class, () -> service.transferGroup(10L, 3L, 1L));
        verifyNoMutation();
    }

    @Test
    void validOwnerTransferUpdatesRolesAndOnlyOwnershipFields() {
        assertTrue(service.transferGroup(10L, 3L, 1L));
        verify(groups).update(isNull(), argThat(wrapper -> {
            String fields = ((UpdateWrapper<?>) wrapper).getSqlSet();
            return fields.contains("owner_id=") && fields.contains("transfer_count")
                    && !fields.contains("member_count");
        }));
        verify(members, times(2)).update(isNull(), any());
        verify(groups, never()).updateById(any(Group.class));
    }

    private Map<String, Object> actor(long userId) {
        return Map.of("userId", userId);
    }

    private void invalidate(Long userId, String state) {
        if ("ABSENT".equals(state)) membership.remove(userId);
        else if ("DISABLED".equals(state)) users.get(userId).setStatus(0);
        else if ("KICKED".equals(state)) membership.get(userId).setKickedAt(new Date());
        else membership.get(userId).setJoinStatus(state);
    }

    private void verifyNoMutation() {
        verify(members, never()).update(isNull(), any());
        verify(members, never()).delete(any());
        verify(groups, never()).update(isNull(), any());
        verify(groups, never()).updateById(any(Group.class));
        verify(groups, never()).decrementMemberCount(anyLong());
        verifyNoInteractions(broadcasts);
    }
}
