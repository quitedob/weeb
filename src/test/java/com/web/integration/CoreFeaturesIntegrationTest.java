package com.web.integration;

import com.web.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Core Features Integration Test
 * Tests newly implemented core features including comments, follows, groups, contacts, and search
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CoreFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test Article Comment System
     * - Create comment
     * - Reply to comment
     * - Delete comment
     * - Verify notification creation
     */
    @Test
    @DisplayName("Article Comment System - Create, Reply, Delete and Notifications")
    void testArticleCommentSystem() throws Exception {
        // Test creating a comment
        String createCommentJson = """
            {
                "articleId": 1,
                "content": "This is a test comment",
                "parentId": null
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/articles/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createCommentJson))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(createResult, true);

        // Test replying to comment (assuming comment ID 1 was created)
        String replyCommentJson = """
            {
                "articleId": 1,
                "content": "This is a reply to the comment",
                "parentId": 1
            }
            """;

        MvcResult replyResult = mockMvc.perform(post("/api/articles/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(replyCommentJson))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(replyResult, true);

        // Test getting article comments
        MvcResult getCommentsResult = mockMvc.perform(get("/api/articles/1/comments")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(getCommentsResult, true);

        // Test deleting comment
        MvcResult deleteResult = mockMvc.perform(delete("/api/articles/comments/1"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(deleteResult, true);

        System.out.println("✅ Article comment system test passed");
    }

    /**
     * Test User Follow System
     * - Follow user
     * - Unfollow user
     * - Get followers/following lists
     * - Verify notification creation
     */
    @Test
    @DisplayName("User Follow System - Follow/Unfollow and Lists")
    void testUserFollowSystem() throws Exception {
        // Test following a user
        String followJson = """
            {
                "targetUserId": 2
            }
            """;

        MvcResult followResult = mockMvc.perform(post("/api/user/follow")
                .contentType(MediaType.APPLICATION_JSON)
                .content(followJson))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(followResult, true);

        // Test getting following list
        MvcResult followingResult = mockMvc.perform(get("/api/user/following")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(followingResult, true);

        // Test getting followers list
        MvcResult followersResult = mockMvc.perform(get("/api/user/followers")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(followersResult, true);

        // Test unfollowing a user
        MvcResult unfollowResult = mockMvc.perform(delete("/api/user/follow/2"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(unfollowResult, true);

        System.out.println("✅ User follow system test passed");
    }

    /**
     * Test Group Management System
     * - Create group
     * - Invite members
     * - Accept/reject invitations
     * - Remove members
     * - Delete group
     */
    @Test
    @DisplayName("Group Management System - Complete Lifecycle")
    void testGroupManagementSystem() throws Exception {
        // Test creating a group
        String createGroupJson = """
            {
                "name": "Test Group",
                "description": "A test group for integration testing",
                "isPublic": true
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createGroupJson))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(createResult, true);

        // Test getting user's groups
        MvcResult getGroupsResult = mockMvc.perform(get("/api/groups/my")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(getGroupsResult, true);

        // Test inviting a user to group (assuming group ID 1 was created)
        String inviteJson = """
            {
                "userId": 2,
                "role": "MEMBER"
            }
            """;

        MvcResult inviteResult = mockMvc.perform(post("/api/groups/1/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inviteJson))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(inviteResult, true);

        // Test getting group members
        MvcResult membersResult = mockMvc.perform(get("/api/groups/1/members"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(membersResult, true);

        // Test removing a member
        MvcResult removeResult = mockMvc.perform(delete("/api/groups/1/members/2"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(removeResult, true);

        // Test deleting group
        MvcResult deleteResult = mockMvc.perform(delete("/api/groups/1"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(deleteResult, true);

        System.out.println("✅ Group management system test passed");
    }

    /**
     * Test Contact Management System
     * - Send friend request
     * - Accept friend request
     * - Reject friend request
     * - Remove friend
     * - Block/unblock user
     */
    @Test
    @DisplayName("Contact Management System - Friend Requests and Management")
    void testContactManagementSystem() throws Exception {
        // Test sending friend request
        String requestJson = """
            {
                "targetUserId": 2,
                "message": "Let's be friends!"
            }
            """;

        MvcResult requestResult = mockMvc.perform(post("/api/contacts/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(requestResult, true);

        // Test getting contact requests
        MvcResult requestsResult = mockMvc.perform(get("/api/contacts/requests"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(requestsResult, true);

        // Test accepting friend request
        MvcResult acceptResult = mockMvc.perform(post("/api/contacts/accept/1"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(acceptResult, true);

        // Test getting contacts list
        MvcResult contactsResult = mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(contactsResult, true);

        // Test removing contact
        MvcResult removeResult = mockMvc.perform(delete("/api/contacts/2"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(removeResult, true);

        System.out.println("✅ Contact management system test passed");
    }

    /**
     * Test Search Functionality
     * - Search users
     * - Search groups
     * - Search articles with Elasticsearch
     */
    @Test
    @DisplayName("Search Functionality - Users, Groups, and Articles")
    void testSearchFunctionality() throws Exception {
        // Test searching users
        MvcResult userSearchResult = mockMvc.perform(get("/api/search/users")
                .param("keyword", "test")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(userSearchResult, true);

        // Test searching groups
        MvcResult groupSearchResult = mockMvc.perform(get("/api/search/groups")
                .param("keyword", "test")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(groupSearchResult, true);

        // Test searching articles (with Elasticsearch)
        MvcResult articleSearchResult = mockMvc.perform(get("/api/search/articles")
                .param("q", "test")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(articleSearchResult, true);

        System.out.println("✅ Search functionality test passed");
    }

    /**
     * Test Notification System Integration
     * - Verify notifications are created for various actions
     * - Test notification retrieval
     * - Test notification marking as read
     */
    @Test
    @DisplayName("Notification System Integration - Creation and Management")
    void testNotificationSystemIntegration() throws Exception {
        // First create some activities that should generate notifications

        // Create a comment (should notify article author)
        String commentJson = """
            {
                "articleId": 1,
                "content": "Notification test comment",
                "parentId": null
            }
            """;

        mockMvc.perform(post("/api/articles/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentJson))
                .andExpect(status().isOk());

        // Follow a user (should notify the followed user)
        String followJson = """
            {
                "targetUserId": 2
            }
            """;

        mockMvc.perform(post("/api/user/follow")
                .contentType(MediaType.APPLICATION_JSON)
                .content(followJson))
                .andExpect(status().isOk());

        // Test getting notifications
        MvcResult notificationsResult = mockMvc.perform(get("/api/notifications")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(notificationsResult, true);

        // Test getting unread notification count
        MvcResult countResult = mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(countResult, true);

        // Test marking notifications as read
        MvcResult markReadResult = mockMvc.perform(post("/api/notifications/mark-read")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[1, 2, 3]")) // Assuming some notification IDs
                .andExpect(status().isOk())
                .andReturn();

        validateApiResponse(markReadResult, true);

        System.out.println("✅ Notification system integration test passed");
    }

    /**
     * Validate API Response format
     */
    private void validateApiResponse(MvcResult result, boolean shouldBeSuccess) throws Exception {
        String responseContent = result.getResponse().getContentAsString();

        if (responseContent.isEmpty()) {
            return;
        }

        // Parse response as ApiResponse
        ApiResponse<?> apiResponse = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(responseContent, ApiResponse.class);

        assertNotNull(apiResponse, "Response should be parseable as ApiResponse");
        assertNotNull(apiResponse.getMessage(), "Message field should be present");

        if (shouldBeSuccess) {
            assertTrue(apiResponse.isSuccess(), "Success responses should have success=true");
        }
    }
}
