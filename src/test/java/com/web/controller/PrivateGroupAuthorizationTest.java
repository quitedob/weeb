package com.web.controller;

import com.web.config.UserInfoArgumentResolver;
import com.web.exception.GlobalExceptionHandler;
import com.web.mapper.*;
import com.web.model.*;
import com.web.service.*;
import com.web.service.impl.GroupServiceImpl;
import com.web.service.impl.UserTypeSecurityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PrivateGroupAuthorizationTest {
    private final GroupMapper groups = mock(GroupMapper.class);
    private final GroupMemberMapper members = mock(GroupMemberMapper.class);
    private final GroupApplicationMapper applications = mock(GroupApplicationMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final AuthMapper accounts = mock(AuthMapper.class);
    private final AuthService auth = mock(AuthService.class);
    private final ChatListMapper chats = mock(ChatListMapper.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final MessageBroadcastService broadcasts = mock(MessageBroadcastService.class);
    private final Map<Long, User> persistedUsers = new HashMap<>();
    private final Map<Long, GroupMember> membership = new HashMap<>();
    private final Group group = new Group();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        group.setId(10L);
        group.setOwnerId(1L);
        group.setIsVisible(0);
        group.setSharedChatId(100L);
        group.setGroupName("Private team");
        group.setGroupDescription("Members only description");
        group.setMemberCount(2);
        for (long id = 1; id <= 4; id++) {
            User user = new User();
            user.setId(id);
            user.setUsername(id == 3 ? "admin_looking_username" : "group-user-" + id);
            user.setStatus(1);
            user.setType(id == 4 ? "ADMIN" : "USER");
            persistedUsers.put(id, user);
        }
        membership.put(1L, new GroupMember(10L, 1L, 1));
        membership.put(2L, new GroupMember(10L, 2L, 3));
        when(groups.selectById(10L)).thenReturn(group);
        when(users.selectById(anyLong())).thenAnswer(call -> persistedUsers.get(call.getArgument(0)));
        when(auth.findByUserID(anyLong())).thenAnswer(call -> persistedUsers.get(call.getArgument(0)));
        when(accounts.findByUsername(anyString())).thenAnswer(call -> persistedUsers.values().stream()
                .filter(user -> user.getUsername().equals(call.getArgument(0))).findFirst().orElse(null));
        when(members.findByGroupAndUser(eq(10L), anyLong())).thenAnswer(call -> membership.get(call.getArgument(1)));
        when(members.findUserIdsByGroupId(10L)).thenReturn(List.of(1L, 2L));

        var service = new GroupServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", groups);
        ReflectionTestUtils.setField(service, "groupMapper", groups);
        ReflectionTestUtils.setField(service, "groupMemberMapper", members);
        ReflectionTestUtils.setField(service, "groupApplicationMapper", applications);
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "authService", auth);
        ReflectionTestUtils.setField(service, "userTypeSecurityService", new UserTypeSecurityServiceImpl(accounts));
        ReflectionTestUtils.setField(service, "chatListMapper", chats);
        ReflectionTestUtils.setField(service, "notificationService", notifications);
        ReflectionTestUtils.setField(service, "messageBroadcastService", broadcasts);
        var controller = new GroupController();
        ReflectionTestUtils.setField(controller, "groupService", service);
        mvc = standaloneSetup(controller).setCustomArgumentResolvers(new UserInfoArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"OUTSIDER", "PENDING", "LEFT", "KICKED", "INVALID_ROLE", "FAKE_OWNER", "DISABLED_ACCOUNT"})
    void privateDetailsAndMembersRejectRevokedOrInvalidMembershipBeforeAnyRepair(String state) throws Exception {
        switch (state) {
            case "OUTSIDER" -> membership.remove(2L);
            case "PENDING" -> membership.get(2L).setJoinStatus("PENDING");
            case "LEFT" -> membership.get(2L).setJoinStatus("LEFT");
            case "KICKED" -> membership.get(2L).setKickedAt(new Date());
            case "INVALID_ROLE" -> membership.get(2L).setRole(0);
            case "FAKE_OWNER" -> membership.get(2L).setRole(1);
            case "DISABLED_ACCOUNT" -> persistedUsers.get(2L).setStatus(0);
        }
        group.setSharedChatId(null);
        mvc.perform(get("/api/groups/10").param("userId", "1").requestAttr("userinfo", actor(2)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/10/members").param("userId", "1").requestAttr("userinfo", actor(2)))
                .andExpect(status().isForbidden());
        verify(members, never()).findUserIdsByGroupId(anyLong());
        verifyNoInteractions(chats, applications, notifications, broadcasts);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 4})
    void validOwnerMemberAndPersistedGlobalAdministratorCanReadPrivateGroup(long userId) throws Exception {
        mvc.perform(get("/api/groups/10").requestAttr("userinfo", actor(userId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.groupDescription").value("Members only description"));
        mvc.perform(get("/api/groups/10/members").requestAttr("userinfo", actor(userId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void missingActorOrAdministratorLookingUsernameCannotReadPrivateGroup() throws Exception {
        mvc.perform(get("/api/groups/10")).andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/10/members")).andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/10").requestAttr("userinfo", actor(3))).andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/10/members").requestAttr("userinfo", actor(3))).andExpect(status().isForbidden());
    }

    @Test
    void privateGroupRejectsGuessedIdApplicationWithoutWritingOrNotifying() throws Exception {
        mvc.perform(post("/api/groups/10/applications").requestAttr("userinfo", actor(3))
                .contentType("application/json").content("{\"message\":\"Please join\",\"userId\":\"1\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(applications, notifications, broadcasts);
    }

    @Test
    void publicGroupStillAllowsNonmemberPreviewAndApplication() throws Exception {
        group.setIsVisible(1);
        when(applications.insert(any(GroupApplication.class))).thenReturn(1);
        mvc.perform(get("/api/groups/10").requestAttr("userinfo", actor(3)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.currentUserRole").value("NON_MEMBER"));
        mvc.perform(get("/api/groups/10/members").requestAttr("userinfo", actor(3)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/groups/10/applications").requestAttr("userinfo", actor(3))
                .contentType("application/json").content("{\"message\":\"Please join\",\"userId\":\"1\"}"))
                .andExpect(status().isCreated());
        verify(applications).insert(argThat((GroupApplication application) -> application.getUserId().equals(3L)
                && application.getGroupId().equals(10L)));
    }

    @Test
    void privateOwnerCanStillInviteThroughExistingMemberAdmission() throws Exception {
        when(groups.incrementMemberCountIfCapacity(10L)).thenReturn(1);
        when(members.insert(any(GroupMember.class))).thenReturn(1);
        when(chats.insertChatList(any())).thenReturn(1);
        mvc.perform(post("/api/groups/10/members").requestAttr("userinfo", actor(1))
                .contentType("application/json").content("{\"groupId\":10,\"memberIds\":[3]}"))
                .andExpect(status().isCreated());
        verify(members).insert(argThat((GroupMember member) -> member.getGroupId().equals(10L)
                && member.getUserId().equals(3L) && "ACCEPTED".equals(member.getJoinStatus())));
        verifyNoInteractions(applications);
    }

    private Map<String, Object> actor(long userId) {
        return Map.of("userId", userId);
    }
}
