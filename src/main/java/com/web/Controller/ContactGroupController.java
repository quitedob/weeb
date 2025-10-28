package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.dto.UserDto;
import com.web.model.ContactGroup;
import com.web.service.ContactService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 联系人分组管理控制器
 * 提供分组的CRUD操作
 */
@Slf4j
@RestController
@RequestMapping("/api/contact-groups")
public class ContactGroupController {

    @Autowired
    private ContactService contactService;

    /**
     * 创建联系人分组
     * POST /api/contact-groups
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createGroup(
            @RequestBody Map<String, Object> request,
            @Userid Long userId) {
        try {
            String groupName = (String) request.get("groupName");
            Integer groupOrder = request.get("groupOrder") != null 
                ? Integer.valueOf(request.get("groupOrder").toString()) 
                : 0;

            Long groupId = contactService.createContactGroup(userId, groupName, groupOrder);
            return ResponseEntity.status(201)
                .body(ApiResponse.success("分组创建成功", groupId));
        } catch (Exception e) {
            log.error("创建分组失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("创建分组失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的所有分组
     * GET /api/contact-groups
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactGroup>>> getUserGroups(@Userid Long userId) {
        try {
            List<ContactGroup> groups = contactService.getUserContactGroups(userId);
            return ResponseEntity.ok(ApiResponse.success(groups));
        } catch (Exception e) {
            log.error("获取分组列表失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取分组列表失败"));
        }
    }

    /**
     * 更新分组名称
     * PUT /api/contact-groups/{groupId}/name
     */
    @PutMapping("/{groupId}/name")
    public ResponseEntity<ApiResponse<String>> updateGroupName(
            @PathVariable Long groupId,
            @RequestBody Map<String, String> request,
            @Userid Long userId) {
        try {
            String newName = request.get("groupName");
            boolean success = contactService.updateContactGroupName(groupId, userId, newName);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("分组名称更新成功"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("分组名称更新失败"));
            }
        } catch (Exception e) {
            log.error("更新分组名称失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("更新分组名称失败: " + e.getMessage()));
        }
    }

    /**
     * 更新分组排序
     * PUT /api/contact-groups/{groupId}/order
     */
    @PutMapping("/{groupId}/order")
    public ResponseEntity<ApiResponse<String>> updateGroupOrder(
            @PathVariable Long groupId,
            @RequestBody Map<String, Integer> request,
            @Userid Long userId) {
        try {
            Integer newOrder = request.get("groupOrder");
            boolean success = contactService.updateContactGroupOrder(groupId, userId, newOrder);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("分组排序更新成功"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("分组排序更新失败"));
            }
        } catch (Exception e) {
            log.error("更新分组排序失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("更新分组排序失败: " + e.getMessage()));
        }
    }

    /**
     * 删除分组
     * DELETE /api/contact-groups/{groupId}
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<String>> deleteGroup(
            @PathVariable Long groupId,
            @Userid Long userId) {
        try {
            boolean success = contactService.deleteContactGroup(groupId, userId);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("分组删除成功"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("分组删除失败"));
            }
        } catch (Exception e) {
            log.error("删除分组失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("删除分组失败: " + e.getMessage()));
        }
    }

    /**
     * 将联系人添加到分组
     * POST /api/contact-groups/{groupId}/contacts/{contactId}
     */
    @PostMapping("/{groupId}/contacts/{contactId}")
    public ResponseEntity<ApiResponse<String>> addContactToGroup(
            @PathVariable Long groupId,
            @PathVariable Long contactId,
            @Userid Long userId) {
        try {
            boolean success = contactService.addContactToGroup(contactId, groupId, userId);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("联系人已添加到分组"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("添加联系人到分组失败"));
            }
        } catch (Exception e) {
            log.error("添加联系人到分组失败: groupId={}, contactId={}, userId={}", 
                groupId, contactId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("添加联系人到分组失败: " + e.getMessage()));
        }
    }

    /**
     * 将联系人从分组移除
     * DELETE /api/contact-groups/contacts/{contactId}
     */
    @DeleteMapping("/contacts/{contactId}")
    public ResponseEntity<ApiResponse<String>> removeContactFromGroup(
            @PathVariable Long contactId,
            @Userid Long userId) {
        try {
            boolean success = contactService.removeContactFromGroup(contactId, userId);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("联系人已移出分组"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("移出联系人失败"));
            }
        } catch (Exception e) {
            log.error("移出联系人失败: contactId={}, userId={}", contactId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("移出联系人失败: " + e.getMessage()));
        }
    }

    /**
     * 获取分组下的联系人列表
     * GET /api/contact-groups/{groupId}/contacts
     */
    @GetMapping("/{groupId}/contacts")
    public ResponseEntity<ApiResponse<List<UserDto>>> getContactsByGroup(
            @PathVariable Long groupId,
            @Userid Long userId) {
        try {
            List<UserDto> contacts = contactService.getContactsByGroup(groupId, userId);
            return ResponseEntity.ok(ApiResponse.success(contacts));
        } catch (Exception e) {
            log.error("获取分组联系人失败: groupId={}, userId={}", groupId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取分组联系人失败"));
        }
    }

    /**
     * 获取默认分组
     * GET /api/contact-groups/default
     */
    @GetMapping("/default")
    public ResponseEntity<ApiResponse<ContactGroup>> getDefaultGroup(@Userid Long userId) {
        try {
            ContactGroup defaultGroup = contactService.getDefaultContactGroup(userId);
            return ResponseEntity.ok(ApiResponse.success(defaultGroup));
        } catch (Exception e) {
            log.error("获取默认分组失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取默认分组失败"));
        }
    }
}
