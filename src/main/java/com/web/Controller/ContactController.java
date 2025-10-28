package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.constant.ContactStatus; // Required for query param
import com.web.dto.UserDto;
import com.web.service.ContactService;
import com.web.vo.contact.ContactApplyVo;
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
        contactService.apply(applyVo, userId);
        return ResponseEntity.ok(ApiResponse.success("申请发送成功"));
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
        String username = requestBody.get("username");
        String message = requestBody.getOrDefault("message", "");
        
        contactService.applyByUsername(username, message, userId);
        return ResponseEntity.ok(ApiResponse.success("好友申请已发送"));
    }

    /**
     * 同意好友申请
     * @param contactId 申请记录的ID (Contact entity ID)
     * @param userId 当前用户ID (被申请人, from @Userid)
     * @return 操作结果
     */
    @PostMapping("/accept/{contactId}")
    public ResponseEntity<ApiResponse<String>> accept(@PathVariable("contactId") Long contactId, @Userid Long userId) {
        contactService.accept(contactId, userId);
        return ResponseEntity.ok(ApiResponse.success("申请已同意"));
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
        contactService.declineOrBlock(contactId, userId, ContactStatus.REJECTED);
        return ResponseEntity.ok(ApiResponse.success("申请已拒绝"));
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
     * @return 联系人列表 (List<UserDto>)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getContacts(@Userid Long userId, @RequestParam("status") ContactStatus status) {
        List<UserDto> contacts = contactService.getContacts(userId, status);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    /**
     * 获取待处理的好友申请列表 (方便前端调用)
     * @param userId 当前用户ID (from @Userid)
     * @return 待处理的申请列表 (List<UserDto> representing the applicants)
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<UserDto>>> getPendingApplications(@Userid Long userId) {
        // This service method might need to return more info than just UserDto,
        // perhaps a list of Contact objects or a custom DTO that includes remarks and contactId.
        // For now, assuming getContacts can handle fetching users who sent PENDING requests to current user.
        // The service method getContacts(userId, ContactStatus.PENDING) should be implemented
        // to fetch users who sent requests TO 'userId'.
        List<UserDto> pendingRequests = contactService.getContacts(userId, ContactStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.success(pendingRequests));
    }
}
