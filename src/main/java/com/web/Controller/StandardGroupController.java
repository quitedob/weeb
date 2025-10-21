package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.Group;
import com.web.model.GroupMember;
import com.web.service.GroupService;
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 标准化群组管理控制器
 * 遵循RESTful API设计规范
 */
@Slf4j
@RestController
@RequestMapping("/api/groups")
public class StandardGroupController {

    @Autowired
    private GroupService groupService;

    /**
     * 创建群组
     * POST /api/groups
     */
    @PostMapping
    @PreAuthorize("hasPermission(null, 'GROUP_CREATE_OWN')")
    public ResponseEntity<ApiResponse<Group>> createGroup(
            @RequestBody @Valid GroupCreateVo createVo,
            @Userid Long userId) {
        try {
            Group group = groupService.createGroup(createVo, userId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("群组创建成功", group));
        } catch (Exception e) {
            log.error("创建群组失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("创建群组失败: " + e.getMessage()));
        }
    }

    /**
     * 获取群组详情
     * GET /api/groups/{groupId}
     */
    @GetMapping("/{groupId}")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_READ_OWN')")
    public ResponseEntity<ApiResponse<Group>> getGroup(@PathVariable Long groupId) {
        try {
            Group group = groupService.getGroupById(groupId);
            if (group == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(group));
        } catch (Exception e) {
            log.error("获取群组详情失败: groupId={}", groupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取群组详情失败: " + e.getMessage()));
        }
    }

    /**
     * 更新群组信息
     * PUT /api/groups/{groupId}
     */
    @PutMapping("/{groupId}")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<Group>> updateGroup(
            @PathVariable Long groupId,
            @RequestBody @Valid GroupCreateVo updateVo,
            @Userid Long userId) {
        try {
            Group group = groupService.updateGroup(groupId, updateVo, userId);
            if (group == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success("群组更新成功", group));
        } catch (Exception e) {
            log.error("更新群组失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("更新群组失败: " + e.getMessage()));
        }
    }

    /**
     * 删除群组
     * DELETE /api/groups/{groupId}
     */
    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_DELETE_OWN')")
    public ResponseEntity<ApiResponse<String>> deleteGroup(
            @PathVariable Long groupId,
            @Userid Long userId) {
        try {
            boolean deleted = groupService.deleteGroup(groupId, userId);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("删除群组失败"));
            }
        } catch (Exception e) {
            log.error("删除群组失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("删除群组失败: " + e.getMessage()));
        }
    }

    /**
     * 获取群组成员列表
     * GET /api/groups/{groupId}/members
     */
    @GetMapping("/{groupId}/members")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_READ_OWN')")
    public ResponseEntity<ApiResponse<List<GroupMember>>> getGroupMembers(@PathVariable Long groupId) {
        try {
            List<GroupMember> members = groupService.getGroupMembers(groupId);
            return ResponseEntity.ok(ApiResponse.success(members));
        } catch (Exception e) {
            log.error("获取群组成员失败: groupId={}", groupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取群组成员失败: " + e.getMessage()));
        }
    }

    /**
     * 邀请用户加入群组
     * POST /api/groups/{groupId}/members
     */
    @PostMapping("/{groupId}/members")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_MANAGE_MEMBERS_OWN')")
    public ResponseEntity<ApiResponse<String>> inviteUserToGroup(
            @PathVariable Long groupId,
            @RequestBody @Valid GroupInviteVo inviteVo,
            @Userid Long userId) {
        try {
            boolean invited = groupService.inviteUser(groupId, inviteVo, userId);
            if (invited) {
                return ResponseEntity.status(201)
                        .body(ApiResponse.success("邀请发送成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("邀请发送失败"));
            }
        } catch (Exception e) {
            log.error("邀请用户加入群组失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("邀请失败: " + e.getMessage()));
        }
    }

    /**
     * 退出群组
     * DELETE /api/groups/{groupId}/members/me
     */
    @DeleteMapping("/{groupId}/members/me")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_LEAVE_OWN')")
    public ResponseEntity<ApiResponse<String>> leaveGroup(
            @PathVariable Long groupId,
            @Userid Long userId) {
        try {
            boolean left = groupService.leaveGroup(groupId, userId);
            if (left) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("退出群组失败"));
            }
        } catch (Exception e) {
            log.error("退出群组失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("退出群组失败: " + e.getMessage()));
        }
    }

    /**
     * 移除群组成员
     * DELETE /api/groups/{groupId}/members/{userId}
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_MANAGE_MEMBERS_OWN')")
    public ResponseEntity<ApiResponse<String>> removeGroupMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @Userid Long currentUserId) {
        try {
            boolean removed = groupService.removeMember(groupId, userId, currentUserId);
            if (removed) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("移除成员失败"));
            }
        } catch (Exception e) {
            log.error("移除群组成员失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("移除成员失败: " + e.getMessage()));
        }
    }

    /**
     * 申请加入群组
     * POST /api/groups/{groupId}/applications
     */
    @PostMapping("/{groupId}/applications")
    @PreAuthorize("hasPermission(null, 'GROUP_JOIN_OWN')")
    public ResponseEntity<ApiResponse<String>> applyToJoinGroup(
            @PathVariable Long groupId,
            @RequestBody Map<String, String> application,
            @Userid Long userId) {
        try {
            String message = application.getOrDefault("message", "");
            boolean applied = groupService.applyToJoinGroup(groupId, userId, message);
            if (applied) {
                return ResponseEntity.status(201)
                        .body(ApiResponse.success("申请已提交"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("申请提交失败"));
            }
        } catch (Exception e) {
            log.error("申请加入群组失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("申请失败: " + e.getMessage()));
        }
    }

    /**
     * 获取群组申请列表
     * GET /api/groups/{groupId}/applications
     */
    @GetMapping("/{groupId}/applications")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_MANAGE_MEMBERS_OWN')")
    public ResponseEntity<ApiResponse<Object>> getGroupApplications(@PathVariable Long groupId) {
        try {
            // 这里需要实现获取群组申请的逻辑
            return ResponseEntity.ok(ApiResponse.success("获取申请列表成功"));
        } catch (Exception e) {
            log.error("获取群组申请失败: groupId={}", groupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取申请列表失败: " + e.getMessage()));
        }
    }

    /**
     * 处理群组申请
     * PUT /api/groups/{groupId}/applications/{applicationId}
     */
    @PutMapping("/{groupId}/applications/{applicationId}")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_MANAGE_MEMBERS_OWN')")
    public ResponseEntity<ApiResponse<String>> processGroupApplication(
            @PathVariable Long groupId,
            @PathVariable Long applicationId,
            @RequestBody Map<String, String> decision,
            @Userid Long userId) {
        try {
            String action = decision.get("action");
            String reason = decision.getOrDefault("reason", "");

            if (!"approve".equals(action) && !"reject".equals(action)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无效的操作类型"));
            }

            boolean processed = "approve".equals(action)
                ? groupService.approveApplication(applicationId, userId, reason)
                : groupService.rejectApplication(applicationId, userId, reason);

            if (processed) {
                return ResponseEntity.ok(ApiResponse.success("申请处理成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("申请处理失败"));
            }
        } catch (Exception e) {
            log.error("处理群组申请失败: groupId={}, applicationId={}", groupId, applicationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("处理申请失败: " + e.getMessage()));
        }
    }

    /**
     * 设置群组管理员
     * PUT /api/groups/{groupId}/members/{userId}/role
     */
    @PutMapping("/{groupId}/members/{userId}/role")
    @PreAuthorize("hasPermission(#groupId, 'GROUP_MANAGE_MEMBERS_OWN')")
    public ResponseEntity<ApiResponse<String>> setMemberRole(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> roleRequest,
            @Userid Long currentUserId) {
        try {
            String role = roleRequest.get("role");
            if (role == null || role.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("角色不能为空"));
            }

            boolean updated = groupService.setMemberRole(groupId, userId, role, currentUserId);
            if (updated) {
                return ResponseEntity.ok(ApiResponse.success("角色设置成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("角色设置失败"));
            }
        } catch (Exception e) {
            log.error("设置成员角色失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("设置角色失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索群组
     * GET /api/groups/search?q=keyword&limit=10
     */
    @GetMapping("/search")
    @PreAuthorize("hasPermission(null, 'GROUP_READ_OWN')")
    public ResponseEntity<ApiResponse<List<Group>>> searchGroups(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Group> groups = groupService.searchGroups(keyword, limit);
            return ResponseEntity.ok(ApiResponse.success(groups));
        } catch (Exception e) {
            log.error("搜索群组失败: keyword={}", keyword, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("搜索群组失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户加入的群组列表
     * GET /api/groups/my-groups
     */
    @GetMapping("/my-groups")
    @PreAuthorize("hasPermission(null, 'GROUP_READ_OWN')")
    public ResponseEntity<ApiResponse<List<Group>>> getMyGroups(@Userid Long userId) {
        try {
            List<Group> groups = groupService.getUserGroups(userId);
            return ResponseEntity.ok(ApiResponse.success(groups));
        } catch (Exception e) {
            log.error("获取用户群组失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取用户群组失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户创建的群组列表
     * GET /api/groups/my-created
     */
    @GetMapping("/my-created")
    @PreAuthorize("hasPermission(null, 'GROUP_READ_OWN')")
    public ResponseEntity<ApiResponse<List<Group>>> getMyCreatedGroups(@Userid Long userId) {
        try {
            List<Group> groups = groupService.getUserCreatedGroups(userId);
            return ResponseEntity.ok(ApiResponse.success(groups));
        } catch (Exception e) {
            log.error("获取用户创建的群组失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取创建的群组失败: " + e.getMessage()));
        }
    }
}