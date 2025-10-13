package com.web.integration;

import com.web.service.ArticleCommentService;
import com.web.service.FileManagementService;
import com.web.service.UserFollowService;
import com.web.service.GroupService;
import com.web.service.ContactService;
import com.web.service.SearchService;
import com.web.service.NotificationService;
import com.web.vo.comment.CommentCreateVo;
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupApplyVo;
import com.web.constant.ContactStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基本功能测试
 * 测试新实现的功能是否正常工作
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BasicFunctionalityTest {

    @Autowired(required = false)
    private ArticleCommentService articleCommentService;

    @Autowired(required = false)
    private FileManagementService fileManagementService;

    @Autowired(required = false)
    private UserFollowService userFollowService;

    @Autowired(required = false)
    private GroupService groupService;

    @Autowired(required = false)
    private ContactService contactService;

    @Autowired(required = false)
    private SearchService searchService;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Test
    void testServicesAreAvailable() {
        // 测试所有服务都能正常注入
        assertNotNull(groupService, "GroupService should be available");
        assertNotNull(contactService, "ContactService should be available");
        assertNotNull(searchService, "SearchService should be available");
        assertNotNull(notificationService, "NotificationService should be available");
        
        // 新增的服务
        assertNotNull(articleCommentService, "ArticleCommentService should be available");
        assertNotNull(fileManagementService, "FileManagementService should be available");
        assertNotNull(userFollowService, "UserFollowService should be available");
    }

    @Test
    void testGroupFunctionality() {
        if (groupService == null) return;
        
        // 测试群组创建
        assertDoesNotThrow(() -> {
            GroupCreateVo createVo = new GroupCreateVo();
            createVo.setGroupName("测试群组");
            createVo.setGroupDescription("测试描述");
            
            // 使用管理员ID（通常为1）
            groupService.createGroup(createVo, 1L);
        });
    }

    @Test
    void testContactFunctionality() {
        if (contactService == null) return;
        
        // 测试联系人功能
        assertDoesNotThrow(() -> {
            contactService.getContacts(1L, ContactStatus.ACCEPTED);
        });
    }

    @Test
    void testSearchFunctionality() {
        if (searchService == null) return;
        
        // 测试搜索功能
        assertDoesNotThrow(() -> {
            Map<String, Object> userResult = searchService.searchUsers("admin", 0, 10);
            assertNotNull(userResult);
            assertTrue(userResult.containsKey("list"));
        });

        assertDoesNotThrow(() -> {
            Map<String, Object> groupResult = searchService.searchGroups("测试", 0, 10);
            assertNotNull(groupResult);
            assertTrue(groupResult.containsKey("list"));
        });
    }

    @Test
    void testNotificationFunctionality() {
        if (notificationService == null) return;
        
        // 测试通知功能
        assertDoesNotThrow(() -> {
            notificationService.createAndPublishNotification(1L, 1L, "TEST", "USER", 1L);
        });

        assertDoesNotThrow(() -> {
            Map<String, Object> notifications = notificationService.getNotificationsForUser(1L, 1, 10);
            assertNotNull(notifications);
        });

        assertDoesNotThrow(() -> {
            int unreadCount = notificationService.getUnreadCount(1L);
            assertTrue(unreadCount >= 0);
        });
    }

    @Test
    void testUserFollowFunctionality() {
        if (userFollowService == null) return;
        
        // 测试关注功能
        assertDoesNotThrow(() -> {
            Map<String, Object> stats = userFollowService.getFollowStats(1L);
            assertNotNull(stats);
            assertTrue(stats.containsKey("followingCount"));
            assertTrue(stats.containsKey("followersCount"));
        });

        assertDoesNotThrow(() -> {
            Map<String, Object> followingList = userFollowService.getFollowingList(1L, 1, 10);
            assertNotNull(followingList);
            assertTrue(followingList.containsKey("following"));
        });

        assertDoesNotThrow(() -> {
            Map<String, Object> followersList = userFollowService.getFollowersList(1L, 1, 10);
            assertNotNull(followersList);
            assertTrue(followersList.containsKey("followers"));
        });
    }

    @Test
    void testFileManagementFunctionality() {
        if (fileManagementService == null) return;
        
        // 测试文件管理功能
        assertDoesNotThrow(() -> {
            Map<String, Object> userFiles = fileManagementService.getUserFiles(1L, 1, 10);
            assertNotNull(userFiles);
            assertTrue(userFiles.containsKey("files"));
            assertTrue(userFiles.containsKey("total"));
        });

        assertDoesNotThrow(() -> {
            String fileUrl = fileManagementService.generateFileUrl(1L);
            assertNotNull(fileUrl);
            assertTrue(fileUrl.contains("/api/files/download/"));
        });
    }

    @Test
    void testArticleCommentFunctionality() {
        if (articleCommentService == null) return;
        
        // 测试评论功能
        assertDoesNotThrow(() -> {
            List<com.web.model.ArticleComment> comments = articleCommentService.getCommentsByArticleId(1L);
            assertNotNull(comments);
        });
    }
}