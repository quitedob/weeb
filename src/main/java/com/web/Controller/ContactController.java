package com.web.Controller;

import com.web.annotation.Userid;
import com.web.constant.ContactStatus; // Required for query param
import com.web.dto.UserDto;
import com.web.service.ContactService;
import com.web.util.ResultUtil;
import com.web.vo.contact.ContactApplyVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
// Using jakarta.validation.Valid
import jakarta.validation.Valid;
import java.util.List; // For return type of getContacts

/**
 * 联系人（好友）管理控制器
 * 简化注释：联系人控制器
 */
@RestController
@RequestMapping("/api/contact") // As per plan
public class ContactController {

    @Autowired
    private ContactService contactService;

    /**
     * 发送好友申请
     * @param applyVo 申请信息 (friendId, remarks)
     * @param userId 申请人ID (from @Userid)
     * @return 操作结果
     */
    @PostMapping("/apply")
    public Object apply(@RequestBody @Valid ContactApplyVo applyVo, @Userid Long userId) {
        contactService.apply(applyVo, userId);
        return ResultUtil.Succeed("申请发送成功");
    }

    /**
     * 同意好友申请
     * @param contactId 申请记录的ID (Contact entity ID)
     * @param userId 当前用户ID (被申请人, from @Userid)
     * @return 操作结果
     */
    @PostMapping("/accept/{contactId}")
    public Object accept(@PathVariable("contactId") Long contactId, @Userid Long userId) {
        contactService.accept(contactId, userId);
        return ResultUtil.Succeed("申请已同意");
    }

    /**
     * 拒绝好友申请
     * @param contactId 申请记录的ID (Contact entity ID)
     * @param userId 当前用户ID (被申请人, from @Userid)
     * @return 操作结果
     */
    @PostMapping("/decline/{contactId}")
    public Object decline(@PathVariable("contactId") Long contactId, @Userid Long userId) {
        contactService.declineOrBlock(contactId, userId, ContactStatus.REJECTED);
        return ResultUtil.Succeed("申请已拒绝");
    }

    /**
     * 拉黑联系人 (可以将现有关系标记为拉黑，或将某个申请直接拉黑)
     * @param contactId 关系记录的ID (Contact entity ID)
     * @param userId 当前用户ID (操作人, from @Userid)
     * @return 操作结果
     */
    @PostMapping("/block/{contactId}")
    public Object block(@PathVariable("contactId") Long contactId, @Userid Long userId) {
        contactService.declineOrBlock(contactId, userId, ContactStatus.BLOCKED);
        return ResultUtil.Succeed("联系人已拉黑");
    }

    /**
     * 获取联系人列表 (例如：好友列表, 待处理申请列表)
     * @param userId 当前用户ID (from @Userid)
     * @param status 要查询的关系状态 (e.g., ACCEPTED, PENDING)
     * @return 联系人列表 (List<UserDto>)
     */
    @GetMapping("/list")
    public Object getContacts(@Userid Long userId, @RequestParam("status") ContactStatus status) {
        List<UserDto> contacts = contactService.getContacts(userId, status);
        return ResultUtil.Succeed(contacts);
    }

    /**
     * 获取待处理的好友申请列表 (方便前端调用)
     * @param userId 当前用户ID (from @Userid)
     * @return 待处理的申请列表 (List<UserDto> representing the applicants)
     */
    @GetMapping("/list/pending")
    public Object getPendingApplications(@Userid Long userId) {
        // This service method might need to return more info than just UserDto,
        // perhaps a list of Contact objects or a custom DTO that includes remarks and contactId.
        // For now, assuming getContacts can handle fetching users who sent PENDING requests to current user.
        // The service method getContacts(userId, ContactStatus.PENDING) should be implemented
        // to fetch users who sent requests TO 'userId'.
        List<UserDto> pendingRequests = contactService.getContacts(userId, ContactStatus.PENDING);
        return ResultUtil.Succeed(pendingRequests);
    }
}
