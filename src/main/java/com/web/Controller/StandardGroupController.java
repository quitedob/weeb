package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.Group;
import com.web.model.GroupMember;
import com.web.service.GroupService;
import com.web.util.ApiResponseUtil;
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import com.web.vo.group.GroupApplyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<Group>> createGroup(
            @RequestBody @Valid GroupCreateVo createVo,
            @Userid Long userId) {
        try {
            Group group = groupService.createGroup(createVo, userId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("群组创建成功", group));
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionGroup(e, "创建群组", userId);
        }
    }

    /**
     * 获取群组详情
     * GET /api/groups/{groupId}
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Group>> getGroup(@PathVariable Long groupId) {
        try {
            Group group = groupService.getGroupById(groupId);
            if (group == null) {
                return ApiResponseUtil.badRequestGroup("群组不存在");
            }
            return ApiResponseUtil.successGroup(group);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionGroup(e, "获取群组详情", groupId);
        }
    }

    /**
     * 更新群组信息
     * PUT /api/groups/{groupId}
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Group>> updateGroup(
            @PathVariable Long groupId,
            @RequestBody @Valid GroupCreateVo updateVo,
            @Userid Long userId) {
        try {
            // 创建 Group 对象用于更新
            Group groupData = new Group();
            groupData.setGroupName(updateVo.getGroupName());
            groupData.setGroupDescription(updateVo.getGroupDescription());

            groupService.updateGroup(groupId, groupData, userId);

            // 获取更新后的群组信息
            Group updatedGroup = groupService.getGroupById(groupId);
            return ApiResponseUtil.successGroup(updatedGroup, "群组更新成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionGroup(e, "更新群组", groupId, userId);
        }
    }

    /**
     * 删除群组
     * DELETE /api/groups/{groupId}
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<String>> deleteGroup(
            @PathVariable Long groupId,
            @Userid Long userId) {
        try {
            boolean deleted = groupService.deleteGroup(groupId, userId);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ApiResponseUtil.badRequestString("删除群组失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "删除群组", groupId, userId);
        }
    }

    /**
     * 获取群组成员列表
     * GET /api/groups/{groupId}/members
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getGroupMembers(@PathVariable Long groupId) {
        try {
            java.util.List<java.util.Map<String, Object>> members = groupService.getGroupMembers(groupId);
            return ApiResponseUtil.successMapList(members);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMapList(e, "获取群组成员", groupId);
        }
    }

    /**
     * 邀请用户加入群组
     * POST /api/groups/{groupId}/members
     */
    @PostMapping("/{groupId}/members")
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
                return ApiResponseUtil.badRequestString("邀请发送失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "邀请用户加入群组", groupId, userId);
        }
    }

    /**
     * 退出群组
     * DELETE /api/groups/{groupId}/members/me
     */
    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<ApiResponse<String>> leaveGroup(
            @PathVariable Long groupId,
            @Userid Long userId) {
        try {
            boolean left = groupService.leaveGroup(groupId, userId);
            if (left) {
                return ResponseEntity.noContent().build();
            } else {
                return ApiResponseUtil.badRequestString("退出群组失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "退出群组", groupId, userId);
        }
    }

    /**
     * 移除群组成员
     * DELETE /api/groups/{groupId}/members/{userId}
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse<String>> removeGroupMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @Userid Long currentUserId) {
        try {
            boolean removed = groupService.removeMember(groupId, userId, currentUserId);
            if (removed) {
                return ResponseEntity.noContent().build();
            } else {
                return ApiResponseUtil.badRequestString("移除成员失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "移除群组成员", groupId, userId);
        }
    }

    /**
     * 申请加入群组
     * POST /api/groups/{groupId}/applications
     */
    @PostMapping("/{groupId}/applications")
    public ResponseEntity<ApiResponse<String>> applyToJoinGroup(
            @PathVariable Long groupId,
            @RequestBody Map<String, String> application,
            @Userid Long userId) {
        try {
            String message = application.getOrDefault("message", "");
            GroupApplyVo applyVo = new GroupApplyVo();
            applyVo.setGroupId(groupId);
            applyVo.setUserId(userId);
            applyVo.setMessage(message);

            boolean applied = groupService.applyToJoinGroup(applyVo, userId);
            if (applied) {
                return ResponseEntity.status(201)
                        .body(ApiResponse.success("申请已提交"));
            } else {
                return ApiResponseUtil.badRequestString("申请提交失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "申请加入群组", groupId, userId);
        }
    }

    /**
     * 获取群组申请列表
     * GET /api/groups/{groupId}/applications
     */
    @GetMapping("/{groupId}/applications")
    public ResponseEntity<ApiResponse<Object>> getGroupApplications(@PathVariable Long groupId) {
        try {
            // 这里需要实现获取群组申请的逻辑
            return ApiResponseUtil.success("获取申请列表成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceException(e, "获取群组申请", groupId);
        }
    }

    /**
     * 处理群组申请
     * PUT /api/groups/{groupId}/applications/{applicationId}
     */
    @PutMapping("/{groupId}/applications/{applicationId}")
    public ResponseEntity<ApiResponse<String>> processGroupApplication(
            @PathVariable Long groupId,
            @PathVariable Long applicationId,
            @RequestBody Map<String, String> decision,
            @Userid Long userId) {
        try {
            String action = decision.get("action");
            String reason = decision.getOrDefault("reason", "");

            if (!"approve".equals(action) && !"reject".equals(action)) {
                return ApiResponseUtil.badRequestString("无效的操作类型");
            }

            boolean processed = "approve".equals(action)
                ? groupService.approveApplication(groupId, applicationId, userId, reason)
                : groupService.rejectApplication(groupId, applicationId, userId, reason);

            if (processed) {
                return ApiResponseUtil.successString("申请处理成功");
            } else {
                return ApiResponseUtil.badRequestString("申请处理失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "处理群组申请", groupId, applicationId);
        }
    }

    /**
     * 设置群组管理员
     * PUT /api/groups/{groupId}/members/{userId}/role
     */
    @PutMapping("/{groupId}/members/{userId}/role")
    public ResponseEntity<ApiResponse<String>> setMemberRole(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> roleRequest,
            @Userid Long currentUserId) {
        try {
            String role = roleRequest.get("role");
            if (role == null || role.trim().isEmpty()) {
                return ApiResponseUtil.badRequestString("角色不能为空");
            }

            boolean updated = groupService.setMemberRole(groupId, userId, role, currentUserId);
            if (updated) {
                return ApiResponseUtil.successString("角色设置成功");
            } else {
                return ApiResponseUtil.badRequestString("角色设置失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "设置成员角色", groupId, userId);
        }
    }

    /**
     * 搜索群组
     * GET /api/groups/search?q=keyword&limit=10
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Group>>> searchGroups(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Group> groups = groupService.searchGroups(keyword, limit);
            return ApiResponseUtil.successGroupList(groups);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionGroupList(e, "搜索群组", keyword, limit);
        }
    }

    /**
     * 获取用户加入的群组列表
     * GET /api/groups/my-groups
     */
    @GetMapping("/my-groups")
    public ResponseEntity<ApiResponse<List<Group>>> getMyGroups(@Userid Long userId) {
        try {
            List<Group> groups = groupService.getUserGroups(userId);
            return ApiResponseUtil.successGroupList(groups);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionGroupList(e, "获取用户群组", userId);
        }
    }

    /**
     * 获取用户创建的群组列表
     * GET /api/groups/my-created
     */
    @GetMapping("/my-created")
    public ResponseEntity<ApiResponse<List<Group>>> getMyCreatedGroups(@Userid Long userId) {
        try {
            List<Group> groups = groupService.getUserCreatedGroups(userId);
            return ApiResponseUtil.successGroupList(groups);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionGroupList(e, "获取用户创建的群组", userId);
        }
    }
}