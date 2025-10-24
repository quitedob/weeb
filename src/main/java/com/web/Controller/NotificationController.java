package com.web.Controller;

import cn.hutool.json.JSONObject;
import com.web.common.ApiResponse;
import com.web.security.SecurityUtils;
import com.web.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知控制器
 * 提供通知相关的REST API接口
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 测试创建通知（仅用于开发测试）
     * @return 操作结果
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> createTestNotification() {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        // 创建测试通知
        notificationService.createAndPublishNotification(
            userId, // 接收者
            userId, // 触发者（这里用同一个用户）
            "TEST_NOTIFICATION", // 通知类型
            "test", // 实体类型
            1L // 实体ID
        );

        return ResponseEntity.ok(ApiResponse.success("测试通知已创建"));
    }

    /**
     * 获取当前登录用户的通知列表（分页）
     * @param page 页码（默认1）
     * @param size 每页大小（默认10）
     * @return 通知列表和分页信息
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(@RequestParam(defaultValue = "1") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        // 参数验证
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;

        Map<String, Object> result = notificationService.getNotificationsForUser(userId, page, size);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取当前用户的未读通知数量
     * @return 未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount() {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        int unreadCount = notificationService.getUnreadCount(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("unreadCount", unreadCount);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 将当前用户的所有通知标记为已读
     * @return 操作结果
     */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        boolean success = notificationService.markAllAsRead(userId);

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("所有通知已标记为已读"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "标记已读失败"));
        }
    }

    /**
     * 将指定通知标记为已读
     * @param id 通知ID
     * @return 操作结果
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        boolean success = notificationService.markAsRead(id, userId);

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("通知已标记为已读"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "标记已读失败，通知不存在或无权限"));
        }
    }

    /**
     * 删除当前用户的所有已读通知
     * @return 删除的通知数量
     */
    @DeleteMapping("/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteReadNotifications() {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        int deletedCount = notificationService.deleteReadNotifications(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", deletedCount);

        return ResponseEntity.ok(ApiResponse.success(data));
    }
} 