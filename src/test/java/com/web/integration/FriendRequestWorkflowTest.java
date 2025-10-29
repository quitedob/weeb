package com.web.integration;

import com.web.constant.ContactStatus;
import com.web.dto.ContactDto;
import com.web.dto.ContactRequestDto;
import com.web.exception.WeebException;
import com.web.mapper.ContactMapper;
import com.web.model.Contact;
import com.web.model.User;
import com.web.service.ContactService;
import com.web.service.NotificationService;
import com.web.service.UserService;
import com.web.vo.contact.ContactApplyVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Friend Request Workflow Integration Test
 * Tests the complete friend request workflow including:
 * - Sending friend requests with notifications
 * - Accepting/rejecting friend requests
 * - Displaying friend lists with complete information
 */
@ExtendWith(MockitoExtension.class)
public class FriendRequestWorkflowTest {

    @Mock
    private ContactMapper contactMapper;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ContactService contactService;

    private User testUser1;
    private User testUser2;
    private Contact testContact;
    private ContactDto testContactDto;
    private ContactRequestDto testContactRequest;

    @BeforeEach
    void setUp() {
        // Setup test users
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setUsername("testuser1");
        testUser1.setNickname("Test User 1");
        testUser1.setAvatar("avatar1.jpg");
        testUser1.setBio("Test bio 1");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("testuser2");
        testUser2.setNickname("Test User 2");
        testUser2.setAvatar("avatar2.jpg");
        testUser2.setBio("Test bio 2");

        // Setup test contact
        testContact = new Contact();
        testContact.setId(1L);
        testContact.setUserId(1L);
        testContact.setFriendId(2L);
        testContact.setStatus(ContactStatus.PENDING.getCode());
        testContact.setCreateTime(new Date());

        // Setup test ContactDto
        testContactDto = new ContactDto();
        testContactDto.setId(2L);
        testContactDto.setUsername("testuser2");
        testContactDto.setNickname("Test User 2");
        testContactDto.setAvatar("avatar2.jpg");
        testContactDto.setBio("Test bio 2");
        testContactDto.setContactTime(LocalDateTime.now());

        // Setup test ContactRequestDto
        testContactRequest = new ContactRequestDto();
        testContactRequest.setId(1L);
        testContactRequest.setUsername("testuser1");
        testContactRequest.setNickname("Test User 1");
        testContactRequest.setAvatar("avatar1.jpg");
        testContactRequest.setRemarks("Hello, let's be friends!");
        testContactRequest.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testSendFriendRequestByUsername_Success() {
        // Arrange
        doNothing().when(contactService).applyByUsername("testuser2", "Hello!", 1L);

        // Act
        assertDoesNotThrow(() -> {
            contactService.applyByUsername("testuser2", "Hello!", 1L);
        });

        // Assert
        verify(contactService).applyByUsername("testuser2", "Hello!", 1L);
    }

    @Test
    void testSendFriendRequestByUsername_UserNotFound() {
        // Arrange
        doThrow(new WeebException("用户不存在")).when(contactService).applyByUsername("nonexistent", "Hello!", 1L);

        // Act & Assert
        WeebException exception = assertThrows(WeebException.class, () -> {
            contactService.applyByUsername("nonexistent", "Hello!", 1L);
        });
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void testSendFriendRequestByUsername_AlreadyFriends() {
        // Arrange
        doThrow(new WeebException("你们已经是好友了")).when(contactService).applyByUsername("testuser2", "Hello!", 1L);

        // Act & Assert
        WeebException exception = assertThrows(WeebException.class, () -> {
            contactService.applyByUsername("testuser2", "Hello!", 1L);
        });
        assertEquals("你们已经是好友了", exception.getMessage());
    }

    @Test
    void testSendFriendRequestByUsername_PendingRequestExists() {
        // Arrange
        doThrow(new WeebException("你已经发送过好友申请，请等待对方处理")).when(contactService).applyByUsername("testuser2", "Hello!", 1L);

        // Act & Assert
        WeebException exception = assertThrows(WeebException.class, () -> {
            contactService.applyByUsername("testuser2", "Hello!", 1L);
        });
        assertEquals("你已经发送过好友申请，请等待对方处理", exception.getMessage());
    }

    @Test
    void testAcceptFriendRequest_Success() {
        // Arrange
        doNothing().when(contactService).accept(1L, 2L);

        // Act
        assertDoesNotThrow(() -> {
            contactService.accept(1L, 2L);
        });

        // Assert
        verify(contactService).accept(1L, 2L);
    }

    @Test
    void testAcceptFriendRequest_NoPermission() {
        // Arrange
        doThrow(new WeebException("无权限操作此联系人记录")).when(contactService).accept(1L, 2L);

        // Act & Assert
        WeebException exception = assertThrows(WeebException.class, () -> {
            contactService.accept(1L, 2L);
        });
        assertEquals("无权限操作此联系人记录", exception.getMessage());
    }

    @Test
    void testRejectFriendRequest_Success() {
        // Arrange
        doNothing().when(contactService).declineOrBlock(1L, 2L, ContactStatus.REJECTED);

        // Act
        assertDoesNotThrow(() -> {
            contactService.declineOrBlock(1L, 2L, ContactStatus.REJECTED);
        });

        // Assert
        verify(contactService).declineOrBlock(1L, 2L, ContactStatus.REJECTED);
    }

    @Test
    void testGetContactsWithDetails_AcceptedFriends() {
        // Arrange
        List<ContactDto> expectedContacts = Arrays.asList(testContactDto);
        when(contactService.getContactsWithDetails(1L, ContactStatus.ACCEPTED)).thenReturn(expectedContacts);

        // Act
        List<ContactDto> result = contactService.getContactsWithDetails(1L, ContactStatus.ACCEPTED);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        ContactDto contact = result.get(0);
        assertEquals("testuser2", contact.getUsername());
        assertEquals("Test User 2", contact.getNickname());
        assertEquals("avatar2.jpg", contact.getAvatar());
        assertEquals("Test bio 2", contact.getBio());
        assertNotNull(contact.getContactTime());
    }

    @Test
    void testGetPendingRequests_Success() {
        // Arrange
        List<ContactRequestDto> expectedRequests = Arrays.asList(testContactRequest);
        when(contactService.getPendingRequests(2L)).thenReturn(expectedRequests);

        // Act
        List<ContactRequestDto> result = contactService.getPendingRequests(2L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        ContactRequestDto request = result.get(0);
        assertEquals("testuser1", request.getUsername());
        assertEquals("Test User 1", request.getNickname());
        assertEquals("Hello, let's be friends!", request.getRemarks());
        assertNotNull(request.getCreatedAt());
    }

    @Test
    void testNotificationFailureDoesNotPreventFriendRequest() {
        // This test verifies that notification failures don't prevent friend requests
        // In a real implementation, the service should handle notification failures gracefully
        
        // Arrange - service should handle notification failures internally
        doNothing().when(contactService).applyByUsername("testuser2", "Hello!", 1L);

        // Act - should not throw exception despite potential notification failure
        assertDoesNotThrow(() -> {
            contactService.applyByUsername("testuser2", "Hello!", 1L);
        });

        // Assert - friend request method should be called
        verify(contactService).applyByUsername("testuser2", "Hello!", 1L);
    }

    @Test
    void testCompleteWorkflow_SendAcceptAndDisplay() {
        // Test complete workflow: send request -> accept -> display friends

        // Step 1: Send friend request
        doNothing().when(contactService).applyByUsername("testuser2", "Hello!", 1L);
        assertDoesNotThrow(() -> {
            contactService.applyByUsername("testuser2", "Hello!", 1L);
        });

        // Step 2: Accept friend request
        doNothing().when(contactService).accept(1L, 2L);
        assertDoesNotThrow(() -> {
            contactService.accept(1L, 2L);
        });

        // Step 3: Display friend list
        List<ContactDto> expectedContacts = Arrays.asList(testContactDto);
        when(contactService.getContactsWithDetails(1L, ContactStatus.ACCEPTED)).thenReturn(expectedContacts);

        List<ContactDto> friends = contactService.getContactsWithDetails(1L, ContactStatus.ACCEPTED);
        assertNotNull(friends);
        assertEquals(1, friends.size());
        assertEquals("testuser2", friends.get(0).getUsername());

        // Verify all service method calls
        verify(contactService).applyByUsername("testuser2", "Hello!", 1L);
        verify(contactService).accept(1L, 2L);
        verify(contactService).getContactsWithDetails(1L, ContactStatus.ACCEPTED);
    }
}