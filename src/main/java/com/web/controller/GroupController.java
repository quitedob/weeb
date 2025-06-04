package com.web.controller; // Ensure lowercase 'controller'

import com.web.annotation.Userid;
import com.web.model.Group;
import com.web.service.GroupService;
import com.web.util.ResultUtil;
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import com.web.vo.group.GroupKickVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
// Using jakarta.validation.Valid
import jakarta.validation.Valid;
import com.web.common.ApiResponse; // For ApiResponse
import com.web.util.SecurityUtil;   // For SecurityUtil
import java.util.List;              // For List

/**
 * 群组管理控制器
 * 简化注释：群组控制器
 */
@RestController
@RequestMapping("/api/group") // Using all lowercase path as per user spec example
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping("/create")
    public Object createGroup(@RequestBody @Valid GroupCreateVo createVo, @Userid Long userId) { // userId as Long
        Group newGroup = groupService.createGroup(createVo, userId);
        return ResultUtil.success(newGroup);
    }

    @PostMapping("/invite")
    public Object inviteMembers(@RequestBody @Valid GroupInviteVo inviteVo, @Userid Long userId) { // userId as Long
        groupService.inviteMembers(inviteVo, userId);
        return ResultUtil.success();
    }

    @PostMapping("/kick")
    public Object kickMember(@RequestBody @Valid GroupKickVo kickVo, @Userid Long userId) { // userId as Long
        groupService.kickMember(kickVo, userId);
        return ResultUtil.success();
    }

    @PostMapping("/leave/{groupId}")
    public Object leaveGroup(@PathVariable("groupId") Long groupId, @Userid Long userId) { // groupId and userId as Long
        groupService.leaveGroup(groupId, userId);
        return ResultUtil.success();
    }

    /**
     * 获取当前用户加入的所有群组
     * @return 群组列表
     */
    @GetMapping("/my-list")
    public ApiResponse<List<Group>> getMyGroupList() {
        Integer userId = SecurityUtil.getUserId();
        List<Group> groupList = groupService.getGroupsByUserId(userId);
        return ApiResponse.success(groupList);
    }
}
