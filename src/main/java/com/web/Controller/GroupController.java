package com.web.Controller;

import com.web.annotation.Userid;
import com.web.model.Group;
import com.web.service.GroupService;
import com.web.util.ResultUtil;
import com.web.util.SecurityUtil;
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import com.web.vo.group.GroupKickVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * 群组管理控制器
 * 简化注释：群组控制器
 */
@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    /**
     * 创建群组
     * @param createVo 群组创建信息
     * @param userId 创建者ID
     * @return 创建的群组信息
     */
    @PostMapping("/create")
    public Object createGroup(@RequestBody @Valid GroupCreateVo createVo, @Userid Long userId) {
        Group group = groupService.createGroup(createVo, userId);
        return ResultUtil.Succeed(group);
    }

    /**
     * 解散群组
     * @param groupId 群组ID
     * @param userId 操作者ID
     * @return 操作结果
     */
    @DeleteMapping("/{groupId}")
    public Object dissolveGroup(@PathVariable Long groupId, @Userid Long userId) {
        groupService.dissolveGroup(groupId, userId);
        return ResultUtil.Succeed("群组已解散");
    }

    /**
     * 退出群组
     * @param groupId 群组ID
     * @param userId 退出者ID
     * @return 操作结果
     */
    @DeleteMapping("/quit/{groupId}")
    public Object quitGroup(@PathVariable Long groupId, @Userid Long userId) {
        groupService.quitGroup(groupId, userId);
        return ResultUtil.Succeed("已退出群组");
    }

    /**
     * 邀请用户加入群组
     * @param groupId 群组ID
     * @param inviteVo 邀请信息
     * @param userId 邀请者ID
     * @return 操作结果
     */
    @PostMapping("/{groupId}/invite")
    public Object inviteUser(@PathVariable Long groupId, 
                           @RequestBody @Valid GroupInviteVo inviteVo, 
                           @Userid Long userId) {
        groupService.inviteUser(groupId, inviteVo, userId);
        return ResultUtil.Succeed("邀请发送成功");
    }

    /**
     * 踢出群成员
     * @param groupId 群组ID
     * @param kickVo 踢出信息
     * @param userId 操作者ID
     * @return 操作结果
     */
    @PostMapping("/{groupId}/kick")
    public Object kickUser(@PathVariable Long groupId, 
                          @RequestBody @Valid GroupKickVo kickVo, 
                          @Userid Long userId) {
        groupService.kickUser(groupId, kickVo, userId);
        return ResultUtil.Succeed("成员已踢出");
    }

    /**
     * 获取用户加入的群组列表
     * @param userId 用户ID
     * @return 用户加入的群组列表
     */
    @GetMapping("/my-list")
    public Object getUserJoinedGroups(@Userid Long userId) {
        return ResultUtil.Succeed(groupService.getUserJoinedGroups(userId));
    }

    /**
     * 获取群组详情
     * @param groupId 群组ID
     * @return 群组详情
     */
    @GetMapping("/{groupId}")
    public Object getGroupDetails(@PathVariable Long groupId) {
        return ResultUtil.Succeed(groupService.getGroupDetails(groupId));
    }

    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @return 群组成员列表
     */
    @GetMapping("/members/{groupId}")
    public Object getGroupMembers(@PathVariable Long groupId) {
        return ResultUtil.Succeed(groupService.getGroupMembers(groupId));
    }

    /**
     * 更新群组信息
     * @param groupId 群组ID
     * @param groupData 群组更新数据
     * @param userId 操作者ID
     * @return 操作结果
     */
    @PutMapping("/{groupId}")
    public Object updateGroup(@PathVariable Long groupId, 
                            @RequestBody Group groupData, 
                            @Userid Long userId) {
        groupService.updateGroup(groupId, groupData, userId);
        return ResultUtil.Succeed("群组信息已更新");
    }
}
