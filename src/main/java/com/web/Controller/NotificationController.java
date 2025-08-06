package com.web.Controller;

import cn.hutool.json.JSONObject;
import com.web.service.NotificationService;
import com.web.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public JSONObject createTestNotification() {
        try {
            // 获取当前登录用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            // 创建测试通知
            notificationService.createAndPublishNotification(
                userId, // 接收者
                userId, // 触发者（这里用同一个用户）
                "TEST_NOTIFICATION", // 通知类型
                "test", // 实体类型
                1L // 实体ID
            );
            
            return ResultUtil.Succeed("测试通知已创建");
        } catch (Exception e) {
            return ResultUtil.Fail("创建测试通知失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户的通知列表（分页）
     * @param page 页码（默认1）
     * @param size 每页大小（默认10）
     * @return 通知列表和分页信息
     */
    @GetMapping
    public JSONObject getNotifications(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        try {
            // 获取当前登录用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            // 参数验证
            if (page < 1) page = 1;
            if (size < 1 || size > 50) size = 10;
            
            Map<String, Object> result = notificationService.getNotificationsForUser(userId, page, size);
            
            return ResultUtil.Succeed(result);
        } catch (Exception e) {
            return ResultUtil.Fail("获取通知列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户的未读通知数量
     * @return 未读通知数量
     */
    @GetMapping("/unread-count")
    public JSONObject getUnreadCount() {
        try {
            // 获取当前登录用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            int unreadCount = notificationService.getUnreadCount(userId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("unreadCount", unreadCount);
            
            return ResultUtil.Succeed(data);
        } catch (Exception e) {
            return ResultUtil.Fail("获取未读通知数量失败: " + e.getMessage());
        }
    }

    /**
     * 将当前用户的所有通知标记为已读
     * @return 操作结果
     */
    @PostMapping("/read-all")
    public JSONObject markAllAsRead() {
        try {
            // 获取当前登录用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            boolean success = notificationService.markAllAsRead(userId);
            
            if (success) {
                return ResultUtil.Succeed("所有通知已标记为已读");
            } else {
                return ResultUtil.Fail("标记已读失败");
            }
        } catch (Exception e) {
            return ResultUtil.Fail("标记已读失败: " + e.getMessage());
        }
    }

    /**
     * 将指定通知标记为已读
     * @param id 通知ID
     * @return 操作结果
     */
    @PostMapping("/{id}/read")
    public JSONObject markAsRead(@PathVariable Long id) {
        try {
            // 获取当前登录用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            boolean success = notificationService.markAsRead(id, userId);
            
            if (success) {
                return ResultUtil.Succeed("通知已标记为已读");
            } else {
                return ResultUtil.Fail("标记已读失败，通知不存在或无权限");
            }
        } catch (Exception e) {
            return ResultUtil.Fail("标记已读失败: " + e.getMessage());
        }
    }

    /**
     * 删除当前用户的所有已读通知
     * @return 删除的通知数量
     */
    @DeleteMapping("/read")
    public JSONObject deleteReadNotifications() {
        try {
            // 获取当前登录用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            int deletedCount = notificationService.deleteReadNotifications(userId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("deletedCount", deletedCount);
            
            return ResultUtil.Succeed(data);
        } catch (Exception e) {
            return ResultUtil.Fail("删除已读通知失败: " + e.getMessage());
        }
    }
} 