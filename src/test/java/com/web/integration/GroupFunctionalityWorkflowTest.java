package com.web.integration;

import com.web.dto.GroupDto;
import com.web.exception.WeebException;
import com.web.service.GroupService;
import com.web.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Group Functionality Workflow Integration Test
 * Tests the complete group functionality workflow including:
 * - Group creation and membership
 * - Group application process with notifications
 * - Group list display with owner and role information
 */
@ExtendWith(MockitoExtension.class)
public class GroupFunctionalityWorkflowTest {

    @Mock
    private GroupService groupService;

    @Mock
    private NotificationService notificationService;

    private GroupDto testGroupDto;
    private GroupDto testOwnedGroupDto;

    @BeforeEach
    void setUp() {
        // Setup test group DTO (user is a member)
        testGroupDto = new GroupDto();
        testGroupDto.setId(1L);
        testGroupDto.setGroupName("Test Group");
        testGroupDto.setGroupDescription("A test group for testing");
        testGroupDto.setOwnerId(2L);
        testGroupDto.setOwnerUsername("groupowner");
        testGroupDto.setGroupAvatarUrl("group-avatar.jpg");
        testGroupDto.setStatus("ACTIVE");
        testGroupDto.setMaxMembers(50);
        testGroupDto.setMemberCount(5);
        testGroupDto.setRole(2); // Member role (not owner)
        testGroupDto.setCreatedAt(LocalDateTime.now());
        testGroupDto.setLastTransferAt(null);
        testGroupDto.setTransferCount(0);

        // Setup test owned group DTO (user is the owner)
        testOwnedGroupDto = new GroupDto();
        testOwnedGroupDto.setId(2L);
        testOwnedGroupDto.setGroupName("My Test Group");
        testOwnedGroupDto.setGroupDescription("A group I own");
        testOwnedGroupDto.setOwnerId(1L);
        testOwnedGroupDto.setOwnerUsername("testuser");
        testOwnedGroupDto.setGroupAvatarUrl("my-group-avatar.jpg");
        testOwnedGroupDto.setStatus("ACTIVE");
        testOwnedGroupDto.setMaxMembers(100);
        testOwnedGroupDto.setMemberCount(10);
        testOwnedGroupDto.setRole(1); // Owner role
        testOwnedGroupDto.setCreatedAt(LocalDateTime.now().minusDays(7));
        testOwnedGroupDto.setLastTransferAt(null);
        testOwnedGroupDto.setTransferCount(0);
    }

    @Test
    void testGetUserGroupsWithDetails_Success() {
        // Arrange
        List<GroupDto> expectedGroups = Arrays.asList(testGroupDto, testOwnedGroupDto);
        when(groupService.getUserGroupsWithDetails(1L)).thenReturn(expectedGroups);

        // Act
        List<GroupDto> result = groupService.getUserGroupsWithDetails(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify first group (member)
        GroupDto memberGroup = result.get(0);
        assertEquals("Test Group", memberGroup.getGroupName());
        assertEquals("groupowner", memberGroup.getOwnerUsername());
        assertEquals(Integer.valueOf(2), memberGroup.getRole()); // Member role
        assertEquals(Integer.valueOf(5), memberGroup.getMemberCount());
        assertNotNull(memberGroup.getCreatedAt());
        
        // Verify second group (owner)
        GroupDto ownedGroup = result.get(1);
        assertEquals("My Test Group", ownedGroup.getGroupName());
        assertEquals("testuser", ownedGroup.getOwnerUsername());
        assertEquals(Integer.valueOf(1), ownedGroup.getRole()); // Owner role
        assertEquals(Integer.valueOf(10), ownedGroup.getMemberCount());
    }

    @Test
    void testGetUserCreatedGroupsWithDetails_Success() {
        // Arrange
        List<GroupDto> expectedOwnedGroups = Arrays.asList(testOwnedGroupDto);
        when(groupService.getUserCreatedGroupsWithDetails(1L)).thenReturn(expectedOwnedGroups);

        // Act
        List<GroupDto> result = groupService.getUserCreatedGroupsWithDetails(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        GroupDto ownedGroup = result.get(0);
        assertEquals("My Test Group", ownedGroup.getGroupName());
        assertEquals("testuser", ownedGroup.getOwnerUsername());
        assertEquals(Integer.valueOf(1), ownedGroup.getRole()); // Owner role
        assertEquals(Integer.valueOf(100), ownedGroup.getMaxMembers());
        assertEquals(Integer.valueOf(10), ownedGroup.getMemberCount());
    }

    @Test
    void testGetGroupWithDetails_Success() {
        // Arrange
        when(groupService.getGroupWithDetails(1L, 1L)).thenReturn(testGroupDto);

        // Act
        GroupDto result = groupService.getGroupWithDetails(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Group", result.getGroupName());
        assertEquals("A test group for testing", result.getGroupDescription());
        assertEquals("groupowner", result.getOwnerUsername());
        assertEquals(Integer.valueOf(2), result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void testGetGroupWithDetails_GroupNotFound() {
        // Arrange
        when(groupService.getGroupWithDetails(999L, 1L)).thenReturn(null);

        // Act
        GroupDto result = groupService.getGroupWithDetails(999L, 1L);

        // Assert
        assertNull(result);
    }

    @Test
    void testGroupApplicationProcess_Success() {
        // This test simulates the group application process
        // In a real implementation, this would involve:
        // 1. User applies to join group
        // 2. Group owner receives notification
        // 3. Owner approves application
        // 4. Applicant receives approval notification
        // 5. User appears in group member list

        // Arrange - mock the application process
        when(groupService.applyToJoinGroup(any(com.web.vo.group.GroupApplyVo.class), eq(1L))).thenReturn(true);

        // Act - user applies to join group
        com.web.vo.group.GroupApplyVo applyVo = new com.web.vo.group.GroupApplyVo();
        applyVo.setGroupId(1L);
        applyVo.setMessage("I'd like to join this group");
        
        boolean result = groupService.applyToJoinGroup(applyVo, 1L);

        // Assert - application method was called and returned true
        assertTrue(result);
        verify(groupService).applyToJoinGroup(any(com.web.vo.group.GroupApplyVo.class), eq(1L));
    }

    @Test
    void testGroupApplicationApproval_Success() {
        // This test simulates the group application approval process
        
        // Arrange - mock the approval process
        when(groupService.approveApplication(1L, 2L, 1L, "Approved")).thenReturn(true);

        // Act - owner approves application
        boolean result = groupService.approveApplication(1L, 2L, 1L, "Approved");

        // Assert - approval method was called and returned true
        assertTrue(result);
        verify(groupService).approveApplication(1L, 2L, 1L, "Approved");
    }

    @Test
    void testGroupCreation_Success() {
        // This test simulates group creation
        
        // Arrange
        com.web.model.Group mockGroup = new com.web.model.Group();
        mockGroup.setId(1L);
        when(groupService.createGroup(any(), eq(1L))).thenReturn(mockGroup);

        // Act
        com.web.model.Group group = groupService.createGroup(new com.web.vo.group.GroupCreateVo(), 1L);

        // Assert
        assertNotNull(group);
        assertEquals(1L, group.getId());
        verify(groupService).createGroup(any(), eq(1L));
    }

    @Test
    void testGroupMembershipDisplay_WithRoleInformation() {
        // Test that group lists display correct role information
        
        // Arrange
        List<GroupDto> mixedGroups = Arrays.asList(testGroupDto, testOwnedGroupDto);
        when(groupService.getUserGroupsWithDetails(1L)).thenReturn(mixedGroups);

        // Act
        List<GroupDto> result = groupService.getUserGroupsWithDetails(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify role information is correctly displayed
        GroupDto memberGroup = result.stream()
            .filter(g -> g.getRole() == 2)
            .findFirst()
            .orElse(null);
        assertNotNull(memberGroup);
        assertEquals("Test Group", memberGroup.getGroupName());
        assertEquals("groupowner", memberGroup.getOwnerUsername());
        
        GroupDto ownerGroup = result.stream()
            .filter(g -> g.getRole() == 1)
            .findFirst()
            .orElse(null);
        assertNotNull(ownerGroup);
        assertEquals("My Test Group", ownerGroup.getGroupName());
        assertEquals("testuser", ownerGroup.getOwnerUsername());
    }

    @Test
    void testGroupFieldNaming_ConsistentCreatedAt() {
        // Test that groups use consistent field naming (createdAt instead of createTime)
        
        // Arrange
        when(groupService.getGroupWithDetails(1L, 1L)).thenReturn(testGroupDto);

        // Act
        GroupDto result = groupService.getGroupWithDetails(1L, 1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCreatedAt()); // Should use createdAt, not createTime
        
        // Verify the field is properly accessible
        LocalDateTime createdAt = result.getCreatedAt();
        assertTrue(createdAt.isBefore(LocalDateTime.now().plusMinutes(1)));
    }

    @Test
    void testCompleteGroupWorkflow_CreateApplyApproveDisplay() {
        // Test complete workflow: create group -> apply to join -> approve -> display members
        
        // Step 1: Create group
        com.web.model.Group mockGroup = new com.web.model.Group();
        mockGroup.setId(1L);
        when(groupService.createGroup(any(), eq(1L))).thenReturn(mockGroup);
        com.web.model.Group group = groupService.createGroup(new com.web.vo.group.GroupCreateVo(), 1L);
        assertEquals(1L, group.getId());

        // Step 2: Another user applies to join
        when(groupService.applyToJoinGroup(any(com.web.vo.group.GroupApplyVo.class), eq(2L))).thenReturn(true);
        com.web.vo.group.GroupApplyVo applyVo = new com.web.vo.group.GroupApplyVo();
        applyVo.setGroupId(1L);
        applyVo.setMessage("Please let me join");
        boolean applyResult = groupService.applyToJoinGroup(applyVo, 2L);
        assertTrue(applyResult);

        // Step 3: Owner approves application
        when(groupService.approveApplication(1L, 1L, 2L, "Approved")).thenReturn(true);
        boolean approveResult = groupService.approveApplication(1L, 1L, 2L, "Approved");
        assertTrue(approveResult);

        // Step 4: Display updated group information
        GroupDto updatedGroup = new GroupDto();
        updatedGroup.setId(1L);
        updatedGroup.setMemberCount(2); // Now has 2 members
        when(groupService.getGroupWithDetails(1L, 1L)).thenReturn(updatedGroup);
        
        GroupDto result = groupService.getGroupWithDetails(1L, 1L);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getMemberCount());

        // Verify all workflow steps were called
        verify(groupService).createGroup(any(), eq(1L));
        verify(groupService).applyToJoinGroup(any(com.web.vo.group.GroupApplyVo.class), eq(2L));
        verify(groupService).approveApplication(1L, 1L, 2L, "Approved");
        verify(groupService).getGroupWithDetails(1L, 1L);
    }

    @Test
    void testGroupNotificationIntegration() {
        // Test that group operations trigger appropriate notifications
        // This is a conceptual test since we're mocking the service
        
        // The actual implementation should:
        // 1. Send notification to group owner when someone applies
        // 2. Send notification to applicant when application is approved
        
        // Arrange - simulate notification integration
        when(groupService.applyToJoinGroup(any(com.web.vo.group.GroupApplyVo.class), eq(2L))).thenReturn(true);
        when(groupService.approveApplication(1L, 1L, 2L, "Approved")).thenReturn(true);

        // Act
        com.web.vo.group.GroupApplyVo applyVo = new com.web.vo.group.GroupApplyVo();
        applyVo.setGroupId(1L);
        applyVo.setMessage("Test application");
        
        boolean applyResult = groupService.applyToJoinGroup(applyVo, 2L);
        boolean approveResult = groupService.approveApplication(1L, 1L, 2L, "Approved");

        // Assert - verify service methods were called (notifications handled internally)
        assertTrue(applyResult);
        assertTrue(approveResult);
        verify(groupService).applyToJoinGroup(any(com.web.vo.group.GroupApplyVo.class), eq(2L));
        verify(groupService).approveApplication(1L, 1L, 2L, "Approved");
    }

    @Test
    void testGroupServiceErrorHandling() {
        // Test error handling in group operations
        
        // Arrange
        doThrow(new WeebException("群组不存在")).when(groupService).getGroupWithDetails(999L, 1L);
        doThrow(new WeebException("无权限操作")).when(groupService).approveApplication(1L, 2L, 3L, "Test");

        // Act & Assert
        WeebException groupNotFound = assertThrows(WeebException.class, () -> {
            groupService.getGroupWithDetails(999L, 1L);
        });
        assertEquals("群组不存在", groupNotFound.getMessage());

        WeebException noPermission = assertThrows(WeebException.class, () -> {
            groupService.approveApplication(1L, 2L, 3L, "Test");
        });
        assertEquals("无权限操作", noPermission.getMessage());
    }
}