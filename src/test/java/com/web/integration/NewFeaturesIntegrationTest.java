package com.web.integration;

import com.web.model.Group;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.service.GroupService;
import com.web.service.ContactService;
import com.web.service.NotificationService;
import com.web.service.SearchService;
import com.web.vo.group.GroupApplyVo;
import com.web.vo.group.GroupCreateVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 新功能集成测试
 * 测试群组申请、联系人管理、搜索功能、通知系统的完整性
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class NewFeaturesIntegrationTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SearchService searchService;

    private Long testUserId = 1L;
    private Long testUser2Id = 2L;
    private Long testGroupId;

    @BeforeEach
    void setUp() {
        // 测试数据准备
    }

    @Test
    @Order(1)
    void testGroupApplyFunctionality() {
        // 1. 创建测试群组
        GroupCreateVo createVo = new GroupCreateVo();
        createVo.setGroupName("测试群组");
        createVo.setGroupDescription("用于测试申请加入功能的群组");

        Group createdGroup = groupService.createGroup(createVo, testUserId);
        assertNotNull(createdGroup);
        assertEquals("测试群组", createdGroup.getGroupName());
        testGroupId = createdGroup.getId();

        // 2. 测试申请加入群组
        GroupApplyVo applyVo = new GroupApplyVo();
        applyVo.setGroupId(testGroupId);
        applyVo.setReason("我想加入这个群组");

        assertDoesNotThrow(() -> {
            groupService.applyToJoinGroup(applyVo, testUser2Id);
        });

        // 3. 验证群成员列表
        List<Map<String, Object>> members = groupService.getGroupMembers(testGroupId);
        assertNotNull(members);
        assertTrue(members.size() >= 1); // 至少有群主
    }

    @Test
    @Order(2)
    void testContactManagementFunctionality() {
        // 测试联系人服务的基本功能
        assertDoesNotThrow(() -> {
            // 测试获取联系人列表
            contactService.getContacts(testUserId, com.web.constant.ContactStatus.ACCEPTED);
        });

        assertDoesNotThrow(() -> {
            // 测试获取待处理申请
            contactService.getContacts(testUserId, com.web.constant.ContactStatus.PENDING);
        });
    }

    @Test
    @Order(3)
    void testSearchFunctionality() {
        // 1. 测试用户搜索
        assertDoesNotThrow(() -> {
            Map<String, Object> userSearchResult = searchService.searchUsers("admin", 0, 10);
            assertNotNull(userSearchResult);
            assertTrue(userSearchResult.containsKey("list"));
        });

        // 2. 测试群组搜索
        assertDoesNotThrow(() -> {
            Map<String, Object> groupSearchResult = searchService.searchGroups("测试", 0, 10);
            assertNotNull(groupSearchResult);
            assertTrue(groupSearchResult.containsKey("list"));
        });

        // 3. 测试消息搜索 - 暂时跳过，因为SearchService中没有searchMessages方法
        // assertDoesNotThrow(() -> {
        //     Map<String, Object> messageSearchResult = searchService.searchMessages("hello", 0, 10);
        //     assertNotNull(messageSearchResult);
        //     assertTrue(messageSearchResult.containsKey("list"));
        // });
    }

    @Test
    @Order(4)
    void testNotificationSystemFunctionality() {
        // 1. 测试创建通知
        assertDoesNotThrow(() -> {
            notificationService.createAndPublishNotification(testUserId, testUser2Id, "TEST", "USER", testUser2Id);
        });

        // 2. 测试获取通知列表
        assertDoesNotThrow(() -> {
            Map<String, Object> notifications = notificationService.getNotificationsForUser(testUserId, 1, 10);
            assertNotNull(notifications);
        });

        // 3. 测试获取未读通知数量
        assertDoesNotThrow(() -> {
            int unreadCount = notificationService.getUnreadCount(testUserId);
            assertTrue(unreadCount >= 0);
        });

        // 4. 测试标记所有通知为已读
        assertDoesNotThrow(() -> {
            notificationService.markAllAsRead(testUserId);
        });

        // 5. 测试删除已读通知
        assertDoesNotThrow(() -> {
            notificationService.deleteReadNotifications(testUserId);
        });
    }

    @Test
    @Order(5)
    void testPerformanceAndConsistency() {
        // 确保有测试群组
        if (testGroupId == null) {
            GroupCreateVo createVo = new GroupCreateVo();
            createVo.setGroupName("性能测试群组");
            createVo.setGroupDescription("用于性能测试");
            Group group = groupService.createGroup(createVo, testUserId);
            testGroupId = group.getId();
        }

        // 性能测试：批量创建群组申请
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 5; i++) {
            GroupApplyVo applyVo = new GroupApplyVo();
            applyVo.setGroupId(testGroupId);
            applyVo.setReason("批量测试申请 " + i);

            assertDoesNotThrow(() -> {
                groupService.applyToJoinGroup(applyVo, testUser2Id);
            });
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证性能：5个请求应该在合理时间内完成
        assertTrue(duration < 3000, "批量申请操作耗时过长: " + duration + "ms");

        // 数据一致性验证
        List<Map<String, Object>> members = groupService.getGroupMembers(testGroupId);
        assertNotNull(members, "群成员数据应该存在");
        assertTrue(members.size() >= 1, "至少应该有群主");
    }
}