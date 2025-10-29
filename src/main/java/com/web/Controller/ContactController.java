package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.constant.ContactStatus; // Required for query param
import com.web.dto.UserDto;
import com.web.dto.ContactDto;
import com.web.exception.WeebException;
import com.web.service.ContactService;
import com.web.vo.contact.ContactApplyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Using jakarta.validation.Valid
import jakarta.validation.Valid;
import java.util.List; // For return type of getContacts

/**
 * 联系人（好友）管理控制器
 * 简化注释：联系人控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/contacts") // As per plan
public class ContactController {

    @Autowired
    private ContactService contactService;

    /**
     * 发送好友申请（通过用户ID）
     * @param applyVo 申请信息 (friendId, remarks)
     * @param userId 申请人ID (from @Userid)
     * @return 操作结果
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<String>> apply(@RequestBody @Valid ContactApplyVo applyVo, @Userid Long userId) {
        try {
            contactService.apply(applyVo, userId);
            return ResponseEntity.ok(ApiResponse.success("申请发送成功"));
        } catch (WeebException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("发送好友申请失败: userId={}, friendId={}", userId, applyVo != null ? applyVo.getFriendId() : null, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("发送好友申请失败，请稍后重试"));
        }
    }

    /**
     * 发送好友申请（兼容前端 /request 路径）
     * @param requestBody 包含 targetUserId 和 message
     * @param userId 申请人ID (from @Userid)
     * @return 操作结果
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<String>> sendRequest(
            @RequestBody java.util.Map<String, Object> requestBody, 
            @Userid Long userId) {
        Long targetUserId = Long.valueOf(requestBody.get("targetUserId").toString());
        String message = requestBody.getOrDefault("message", "").toString();
        
        ContactApplyVo applyVo = new ContactApplyVo();
        applyVo.setFriendId(targetUserId);
        applyVo.setRemarks(message);
        
        contactService.apply(applyVo, userId);
        return ResponseEntity.ok(ApiResponse.success("好友申请已发送"));
    }

    /**
     * 通过用户名发送好友申请
     * @param requestBody 包含 username 和 message
     * @param userId 申请人ID (from @Userid)
     * @return 操作结果
     */
    @PostMapping("/request/by-username")
    public ResponseEntity<ApiResponse<String>> sendRequestByUsername(
            @RequestBody java.util.Map<String, String> requestBody, 
            @Userid Long userId) {
        try {
            if (requestBody == null || !requestBody.containsKey("username")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名不能为空"));
            }
            
            String username = requestBody.get("username");
            String message = requestBody.getOrDefault("message", "");
            
            contactService.applyByUsername(username, message, userId);
            return ResponseEntity.ok(ApiResponse.success("好友申请已发送"));
        } catch (WeebException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("通过用户名发送好友申请失败: userId={}, username={}", userId, 
                     requestBody != null ? requestBody.get("username") : null, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("发送好友申请失败，请稍后重试"));
        }
    }

    /**
     * 同意好友申请
     * @param contactId 申请记录的ID (Contact entity ID)
     * @param userId 当前用户ID (被申请人, from @Userid)
     * @return 操作结果
     */
    @PostMapping("/accept/{contactId}")
    public ResponseEntity<ApiResponse<String>> accept(@PathVariable("contactId") Long contactId, @Userid Long userId) {
        try {
            contactService.accept(contactId, userId);
            return ResponseEntity.ok(ApiResponse.success("申请已同意"));
        } catch (WeebException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("接受好友申请失败: userId={}, contactId={}", userId, contactId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("接受好友申请失败，请稍后重试"));
        }
    }

    /**
     * 同意好友申请（兼容前端 /request/{id}/accept 路径）
     */
    @PostMapping("/request/{requestId}/accept")
    public ResponseEntity<ApiResponse<String>> acceptRequest(@PathVariable("requestId") Long requestId, @Userid Long userId) {
        contactService.accept(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success("已接受好友申请"));
    }

    /**
     * 拒绝好友申请
     * @param contactId 申请记录的ID (Contact entity ID)
     * @param userId 当前用户ID (被申请人, from @Userid)
     * @return 操作结果
     */
    @PostMapping("/decline/{contactId}")
    public ResponseEntity<ApiResponse<String>> decline(@PathVariable("contactId") Long contactId, @Userid Long userId) {
        try {
            contactService.declineOrBlock(contactId, userId, ContactStatus.REJECTED);
            return ResponseEntity.ok(ApiResponse.success("申请已拒绝"));
        } catch (WeebException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("拒绝好友申请失败: userId={}, contactId={}", userId, contactId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("拒绝好友申请失败，请稍后重试"));
        }
    }

    /**
     * 拒绝好友申请（兼容前端 /request/{id}/reject 路径）
     */
    @PostMapping("/request/{requestId}/reject")
    public ResponseEntity<ApiResponse<String>> rejectRequest(@PathVariable("requestId") Long requestId, @Userid Long userId) {
        contactService.declineOrBlock(requestId, userId, ContactStatus.REJECTED);
        return ResponseEntity.ok(ApiResponse.success("已拒绝好友申请"));
    }

    /**
     * 拉黑联系人 (可以将现有关系标记为拉黑，或将某个申请直接拉黑)
     * @param contactId 关系记录的ID (Contact entity ID)
     * @param userId 当前用户ID (操作人, from @Userid)
     * @return 操作结果
     */
    @PostMapping("/block/{contactId}")
    public ResponseEntity<ApiResponse<String>> block(@PathVariable("contactId") Long contactId, @Userid Long userId) {
        contactService.declineOrBlock(contactId, userId, ContactStatus.BLOCKED);
        return ResponseEntity.ok(ApiResponse.success("联系人已拉黑"));
    }

    /**
     * 获取联系人列表 (例如：好友列表, 待处理申请列表)
     * @param userId 当前用户ID (from @Userid)
     * @param status 要查询的关系状态 (e.g., ACCEPTED, PENDING)
     * @return 联系人详细信息列表 (List<ContactDto>)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactDto>>> getContacts(@Userid Long userId, @RequestParam("status") ContactStatus status) {
        try {
            List<ContactDto> contacts = contactService.getContactsWithDetails(userId, status);
            return ResponseEntity.ok(ApiResponse.success(contacts));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取联系人列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取待处理的好友申请列表 (方便前端调用)
     * @param userId 当前用户ID (from @Userid)
     * @return 待处理的申请列表（包含详细信息）
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<com.web.dto.ContactRequestDto>>> getPendingApplications(@Userid Long userId) {
        List<com.web.dto.ContactRequestDto> pendingRequests = contactService.getPendingRequests(userId);
        return ResponseEntity.ok(ApiResponse.success(pendingRequests));
    }

    // ==================== 联系人分组管理接口 ====================

    /**
     * 创建联系人分组
     */
    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<Long>> createGroup(
            @RequestBody java.util.Map<String, Object> requestBody,
            @Userid Long userId) {
        String groupName = requestBody.get("groupName").toString();
        Integer groupOrder = requestBody.containsKey("groupOrder") 
            ? Integer.valueOf(requestBody.get("groupOrder").toString()) 
            : 0;
        
        Long groupId = contactService.createContactGroup(userId, groupName, groupOrder);
        return ResponseEntity.ok(ApiResponse.success(groupId));
    }

    /**
     * 获取用户的所有联系人分组
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<com.web.model.ContactGroup>>> getUserGroups(@Userid Long userId) {
        List<com.web.model.ContactGroup> groups = contactService.getUserContactGroups(userId);
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    /**
     * 更新分组名称
     */
    @PutMapping("/groups/{groupId}/name")
    public ResponseEntity<ApiResponse<String>> updateGroupName(
            @PathVariable Long groupId,
            @RequestBody java.util.Map<String, String> requestBody,
            @Userid Long userId) {
        String newName = requestBody.get("groupName");
        boolean success = contactService.updateContactGroupName(groupId, userId, newName);
        return ResponseEntity.ok(ApiResponse.success(success ? "分组名称已更新" : "更新失败"));
    }

    /**
     * 更新分组排序
     */
    @PutMapping("/groups/{groupId}/order")
    public ResponseEntity<ApiResponse<String>> updateGroupOrder(
            @PathVariable Long groupId,
            @RequestBody java.util.Map<String, Integer> requestBody,
            @Userid Long userId) {
        Integer newOrder = requestBody.get("groupOrder");
        boolean success = contactService.updateContactGroupOrder(groupId, userId, newOrder);
        return ResponseEntity.ok(ApiResponse.success(success ? "分组排序已更新" : "更新失败"));
    }

    /**
     * 删除分组
     */
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<String>> deleteGroup(
            @PathVariable Long groupId,
            @Userid Long userId) {
        boolean success = contactService.deleteContactGroup(groupId, userId);
        return ResponseEntity.ok(ApiResponse.success(success ? "分组已删除" : "删除失败"));
    }

    /**
     * 将联系人添加到分组
     */
    @PostMapping("/groups/{groupId}/contacts/{contactId}")
    public ResponseEntity<ApiResponse<String>> addContactToGroup(
            @PathVariable Long groupId,
            @PathVariable Long contactId,
            @Userid Long userId) {
        boolean success = contactService.addContactToGroup(contactId, groupId, userId);
        return ResponseEntity.ok(ApiResponse.success(success ? "已添加到分组" : "添加失败"));
    }

    /**
     * 从分组中移除联系人（移到默认分组）
     */
    @DeleteMapping("/groups/contacts/{contactId}")
    public ResponseEntity<ApiResponse<String>> removeContactFromGroup(
            @PathVariable Long contactId,
            @Userid Long userId) {
        boolean success = contactService.removeContactFromGroup(contactId, userId);
        return ResponseEntity.ok(ApiResponse.success(success ? "已移到默认分组" : "移除失败"));
    }

    /**
     * 获取指定分组的联系人列表
     */
    @GetMapping("/groups/{groupId}/contacts")
    public ResponseEntity<ApiResponse<List<UserDto>>> getContactsByGroup(
            @PathVariable Long groupId,
            @Userid Long userId) {
        List<UserDto> contacts = contactService.getContactsByGroup(groupId, userId);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }
}
