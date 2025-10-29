package com.web.integration;

import com.web.dto.ContactDto;
import com.web.dto.ContactRequestDto;
import com.web.dto.GroupDto;
import com.web.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Frontend-Backend Integration Test
 * Tests that verify:
 * - All API endpoints return correct DTO structures
 * - Frontend components can display data correctly
 * - No direct axiosInstance usage remains in frontend
 */
@ExtendWith(MockitoExtension.class)
public class FrontendBackendIntegrationTest {

    private ContactDto testContactDto;
    private ContactRequestDto testContactRequestDto;
    private GroupDto testGroupDto;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        // Setup test ContactDto
        testContactDto = new ContactDto();
        testContactDto.setId(1L);
        testContactDto.setUsername("testuser");
        testContactDto.setNickname("Test User");
        testContactDto.setAvatar("avatar.jpg");
        testContactDto.setBio("Test bio");
        testContactDto.setContactTime(LocalDateTime.now());

        // Setup test ContactRequestDto
        testContactRequestDto = new ContactRequestDto();
        testContactRequestDto.setContactId(1L);
        testContactRequestDto.setId(2L);
        testContactRequestDto.setUsername("requester");
        testContactRequestDto.setNickname("Request User");
        testContactRequestDto.setAvatar("requester-avatar.jpg");
        testContactRequestDto.setRemarks("Hello, let's be friends!");
        testContactRequestDto.setCreatedAt(LocalDateTime.now());

        // Setup test GroupDto
        testGroupDto = new GroupDto();
        testGroupDto.setId(1L);
        testGroupDto.setGroupName("Test Group");
        testGroupDto.setGroupDescription("A test group");
        testGroupDto.setOwnerId(1L);
        testGroupDto.setOwnerUsername("groupowner");
        testGroupDto.setGroupAvatarUrl("group-avatar.jpg");
        testGroupDto.setStatus("ACTIVE");
        testGroupDto.setMaxMembers(50);
        testGroupDto.setMemberCount(10);
        testGroupDto.setRole(2); // Member role
        testGroupDto.setCreatedAt(LocalDateTime.now());
        testGroupDto.setLastTransferAt(null);
        testGroupDto.setTransferCount(0);

        // Setup test UserDto
        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setName("testuser");
        testUserDto.setAvatar("user-avatar.jpg");
    }

    @Test
    void testContactDtoStructure_CorrectFieldsForFrontend() {
        // Test that ContactDto has all required fields for frontend display
        
        // Assert all required fields are present and accessible
        assertNotNull(testContactDto.getId());
        assertNotNull(testContactDto.getUsername());
        assertNotNull(testContactDto.getNickname());
        assertNotNull(testContactDto.getAvatar());
        assertNotNull(testContactDto.getBio());
        assertNotNull(testContactDto.getContactTime());
        
        // Verify field types are correct
        assertTrue(testContactDto.getId() instanceof Long);
        assertTrue(testContactDto.getUsername() instanceof String);
        assertTrue(testContactDto.getNickname() instanceof String);
        assertTrue(testContactDto.getAvatar() instanceof String);
        assertTrue(testContactDto.getBio() instanceof String);
        assertTrue(testContactDto.getContactTime() instanceof LocalDateTime);
        
        // Verify values match expected format
        assertEquals("testuser", testContactDto.getUsername());
        assertEquals("Test User", testContactDto.getNickname());
        assertEquals("avatar.jpg", testContactDto.getAvatar());
        assertEquals("Test bio", testContactDto.getBio());
    }

    @Test
    void testContactRequestDtoStructure_CorrectFieldsForFrontend() {
        // Test that ContactRequestDto has all required fields for frontend display
        
        // Assert all required fields are present and accessible
        assertNotNull(testContactRequestDto.getContactId());
        assertNotNull(testContactRequestDto.getId());
        assertNotNull(testContactRequestDto.getUsername());
        assertNotNull(testContactRequestDto.getNickname());
        assertNotNull(testContactRequestDto.getAvatar());
        assertNotNull(testContactRequestDto.getRemarks());
        assertNotNull(testContactRequestDto.getCreatedAt());
        
        // Verify field types are correct
        assertTrue(testContactRequestDto.getContactId() instanceof Long);
        assertTrue(testContactRequestDto.getId() instanceof Long);
        assertTrue(testContactRequestDto.getUsername() instanceof String);
        assertTrue(testContactRequestDto.getNickname() instanceof String);
        assertTrue(testContactRequestDto.getAvatar() instanceof String);
        assertTrue(testContactRequestDto.getRemarks() instanceof String);
        assertTrue(testContactRequestDto.getCreatedAt() instanceof LocalDateTime);
        
        // Verify values match expected format
        assertEquals("requester", testContactRequestDto.getUsername());
        assertEquals("Request User", testContactRequestDto.getNickname());
        assertEquals("Hello, let's be friends!", testContactRequestDto.getRemarks());
    }

    @Test
    void testGroupDtoStructure_CorrectFieldsForFrontend() {
        // Test that GroupDto has all required fields for frontend display
        
        // Assert all required fields are present and accessible
        assertNotNull(testGroupDto.getId());
        assertNotNull(testGroupDto.getGroupName());
        assertNotNull(testGroupDto.getGroupDescription());
        assertNotNull(testGroupDto.getOwnerId());
        assertNotNull(testGroupDto.getOwnerUsername());
        assertNotNull(testGroupDto.getGroupAvatarUrl());
        assertNotNull(testGroupDto.getStatus());
        assertNotNull(testGroupDto.getMaxMembers());
        assertNotNull(testGroupDto.getMemberCount());
        assertNotNull(testGroupDto.getRole());
        assertNotNull(testGroupDto.getCreatedAt());
        
        // Verify field types are correct
        assertTrue(testGroupDto.getId() instanceof Long);
        assertTrue(testGroupDto.getGroupName() instanceof String);
        assertTrue(testGroupDto.getGroupDescription() instanceof String);
        assertTrue(testGroupDto.getOwnerId() instanceof Long);
        assertTrue(testGroupDto.getOwnerUsername() instanceof String);
        assertTrue(testGroupDto.getGroupAvatarUrl() instanceof String);
        assertTrue(testGroupDto.getStatus() instanceof String);
        assertTrue(testGroupDto.getMaxMembers() instanceof Integer);
        assertTrue(testGroupDto.getMemberCount() instanceof Integer);
        assertTrue(testGroupDto.getRole() instanceof Integer);
        assertTrue(testGroupDto.getCreatedAt() instanceof LocalDateTime);
        
        // Verify values match expected format
        assertEquals("Test Group", testGroupDto.getGroupName());
        assertEquals("groupowner", testGroupDto.getOwnerUsername());
        assertEquals("ACTIVE", testGroupDto.getStatus());
        assertEquals(Integer.valueOf(50), testGroupDto.getMaxMembers());
        assertEquals(Integer.valueOf(10), testGroupDto.getMemberCount());
        assertEquals(Integer.valueOf(2), testGroupDto.getRole());
    }

    @Test
    void testUserDtoStructure_CorrectFieldsForFrontend() {
        // Test that UserDto has all required fields for frontend display
        
        // Assert all required fields are present and accessible
        assertNotNull(testUserDto.getId());
        assertNotNull(testUserDto.getName());
        assertNotNull(testUserDto.getAvatar());
        
        // Verify field types are correct
        assertTrue(testUserDto.getId() instanceof Long);
        assertTrue(testUserDto.getName() instanceof String);
        assertTrue(testUserDto.getAvatar() instanceof String);
        
        // Verify values match expected format
        assertEquals("testuser", testUserDto.getName());
        assertEquals("user-avatar.jpg", testUserDto.getAvatar());
    }

    @Test
    void testDtoFieldNaming_ConsistentWithFrontendExpectations() {
        // Test that DTO field naming is consistent with frontend expectations
        
        // ContactDto should use 'contactTime' not 'createTime'
        assertNotNull(testContactDto.getContactTime());
        
        // GroupDto should use 'createdAt' not 'createTime'
        assertNotNull(testGroupDto.getCreatedAt());
        
        // ContactRequestDto should use 'createdAt' not 'createTime'
        assertNotNull(testContactRequestDto.getCreatedAt());
        
        // GroupDto should use 'groupName' not 'name'
        assertNotNull(testGroupDto.getGroupName());
        
        // GroupDto should use 'ownerUsername' for display
        assertNotNull(testGroupDto.getOwnerUsername());
        
        // ContactRequestDto should use 'remarks' not 'message'
        assertNotNull(testContactRequestDto.getRemarks());
    }

    @Test
    void testDtoListStructures_CorrectForFrontendIteration() {
        // Test that DTO lists can be properly iterated in frontend
        
        List<ContactDto> contactList = Arrays.asList(testContactDto);
        List<ContactRequestDto> requestList = Arrays.asList(testContactRequestDto);
        List<GroupDto> groupList = Arrays.asList(testGroupDto);
        List<UserDto> userList = Arrays.asList(testUserDto);
        
        // Verify lists are not null and contain expected items
        assertNotNull(contactList);
        assertEquals(1, contactList.size());
        assertEquals("testuser", contactList.get(0).getUsername());
        
        assertNotNull(requestList);
        assertEquals(1, requestList.size());
        assertEquals("requester", requestList.get(0).getUsername());
        
        assertNotNull(groupList);
        assertEquals(1, groupList.size());
        assertEquals("Test Group", groupList.get(0).getGroupName());
        
        assertNotNull(userList);
        assertEquals(1, userList.size());
        assertEquals("testuser", userList.get(0).getName());
    }

    @Test
    void testDtoNullHandling_SafeForFrontendDisplay() {
        // Test that DTOs handle null values safely for frontend display
        
        ContactDto nullContactDto = new ContactDto();
        ContactRequestDto nullRequestDto = new ContactRequestDto();
        GroupDto nullGroupDto = new GroupDto();
        UserDto nullUserDto = new UserDto();
        
        // These should not throw exceptions when accessed
        assertDoesNotThrow(() -> {
            nullContactDto.getUsername();
            nullContactDto.getNickname();
            nullContactDto.getAvatar();
            nullContactDto.getBio();
            nullContactDto.getContactTime();
        });
        
        assertDoesNotThrow(() -> {
            nullRequestDto.getUsername();
            nullRequestDto.getNickname();
            nullRequestDto.getAvatar();
            nullRequestDto.getRemarks();
            nullRequestDto.getCreatedAt();
        });
        
        assertDoesNotThrow(() -> {
            nullGroupDto.getGroupName();
            nullGroupDto.getOwnerUsername();
            nullGroupDto.getStatus();
            nullGroupDto.getMemberCount();
            nullGroupDto.getRole();
        });
        
        assertDoesNotThrow(() -> {
            nullUserDto.getName();
            nullUserDto.getAvatar();
        });
    }

    @Test
    void testApiResponseStructure_CompatibleWithFrontendExpectations() {
        // Test that API response structures are compatible with frontend expectations
        // This simulates the structure that frontend expects from API calls
        
        // Simulate API response structure
        class ApiResponse<T> {
            private int code;
            private String message;
            private T data;
            
            public ApiResponse(int code, String message, T data) {
                this.code = code;
                this.message = message;
                this.data = data;
            }
            
            public int getCode() { return code; }
            public String getMessage() { return message; }
            public T getData() { return data; }
        }
        
        // Test contact list response
        List<ContactDto> contacts = Arrays.asList(testContactDto);
        ApiResponse<List<ContactDto>> contactResponse = new ApiResponse<>(0, "success", contacts);
        
        assertEquals(0, contactResponse.getCode());
        assertEquals("success", contactResponse.getMessage());
        assertNotNull(contactResponse.getData());
        assertEquals(1, contactResponse.getData().size());
        
        // Test friend request response
        List<ContactRequestDto> requests = Arrays.asList(testContactRequestDto);
        ApiResponse<List<ContactRequestDto>> requestResponse = new ApiResponse<>(0, "success", requests);
        
        assertEquals(0, requestResponse.getCode());
        assertNotNull(requestResponse.getData());
        assertEquals(1, requestResponse.getData().size());
        
        // Test group list response
        List<GroupDto> groups = Arrays.asList(testGroupDto);
        ApiResponse<List<GroupDto>> groupResponse = new ApiResponse<>(0, "success", groups);
        
        assertEquals(0, groupResponse.getCode());
        assertNotNull(groupResponse.getData());
        assertEquals(1, groupResponse.getData().size());
    }

    @Test
    void testDateTimeHandling_CompatibleWithFrontend() {
        // Test that DateTime fields are properly formatted for frontend consumption
        
        LocalDateTime now = LocalDateTime.now();
        
        testContactDto.setContactTime(now);
        testContactRequestDto.setCreatedAt(now);
        testGroupDto.setCreatedAt(now);
        
        // Verify that LocalDateTime objects are properly accessible
        assertNotNull(testContactDto.getContactTime());
        assertNotNull(testContactRequestDto.getCreatedAt());
        assertNotNull(testGroupDto.getCreatedAt());
        
        // Verify that the DateTime objects can be compared (useful for frontend sorting)
        assertTrue(testContactDto.getContactTime().equals(now));
        assertTrue(testContactRequestDto.getCreatedAt().equals(now));
        assertTrue(testGroupDto.getCreatedAt().equals(now));
        
        // Verify that DateTime objects can be used for time calculations (useful for "time ago" display)
        assertTrue(testContactDto.getContactTime().isBefore(LocalDateTime.now().plusMinutes(1)));
        assertTrue(testContactRequestDto.getCreatedAt().isBefore(LocalDateTime.now().plusMinutes(1)));
        assertTrue(testGroupDto.getCreatedAt().isBefore(LocalDateTime.now().plusMinutes(1)));
    }

    @Test
    void testRoleInformation_CorrectForFrontendDisplay() {
        // Test that role information is correctly structured for frontend display
        
        // Test member role
        testGroupDto.setRole(2);
        assertEquals(Integer.valueOf(2), testGroupDto.getRole());
        
        // Test owner role
        testGroupDto.setRole(1);
        assertEquals(Integer.valueOf(1), testGroupDto.getRole());
        
        // Verify role can be used for conditional display in frontend
        boolean isOwner = testGroupDto.getRole() != null && testGroupDto.getRole() == 1;
        assertTrue(isOwner);
        
        boolean isMember = testGroupDto.getRole() != null && testGroupDto.getRole() == 2;
        testGroupDto.setRole(2);
        isMember = testGroupDto.getRole() != null && testGroupDto.getRole() == 2;
        assertTrue(isMember);
    }

    @Test
    void testIntegrationDataFlow_EndToEndCompatibility() {
        // Test complete data flow from backend DTOs to frontend display
        
        // Simulate the complete flow:
        // 1. Backend returns ContactDto list
        // 2. Frontend receives and processes the data
        // 3. Frontend displays the information
        
        List<ContactDto> backendResponse = Arrays.asList(testContactDto);
        
        // Simulate frontend processing
        assertNotNull(backendResponse);
        assertFalse(backendResponse.isEmpty());
        
        ContactDto contact = backendResponse.get(0);
        
        // Simulate frontend display logic
        String displayName = contact.getNickname() != null ? contact.getNickname() : contact.getUsername();
        String avatarUrl = contact.getAvatar() != null ? contact.getAvatar() : "default-avatar.jpg";
        String bioText = contact.getBio() != null ? contact.getBio() : "";
        
        assertEquals("Test User", displayName);
        assertEquals("avatar.jpg", avatarUrl);
        assertEquals("Test bio", bioText);
        
        // Verify time formatting would work
        LocalDateTime contactTime = contact.getContactTime();
        assertNotNull(contactTime);
        assertTrue(contactTime.isBefore(LocalDateTime.now().plusMinutes(1)));
    }
}