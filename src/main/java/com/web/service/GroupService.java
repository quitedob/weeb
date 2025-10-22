package com.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.web.model.Group; // Assuming Group model will be created
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import com.web.vo.group.GroupKickVo;
import com.web.vo.group.GroupApplyVo;
import java.util.List;
import java.util.Map;

/**
 * 群组服务接口
 * 简化注释：群组服务
 */
public interface GroupService extends IService<Group> {
    Group createGroup(GroupCreateVo createVo, Long userId); // Return type Group, userId as Long

    void inviteMembers(GroupInviteVo inviteVo, Long userId); // userId as Long

    void kickMember(GroupKickVo kickVo, Long userId); // userId as Long

    void leaveGroup(Long groupId, Long userId); // groupId and userId as Long

    /**
     * 解散群组
     * @param groupId 群组ID
     * @param userId 操作者ID
     */
    void dissolveGroup(Long groupId, Long userId);

    /**
     * 退出群组
     * @param groupId 群组ID
     * @param userId 退出者ID
     */
    void quitGroup(Long groupId, Long userId);

    /**
     * 邀请用户加入群组
     * @param groupId 群组ID
     * @param inviteVo 邀请信息
     * @param userId 邀请者ID
     */
    void inviteUser(Long groupId, GroupInviteVo inviteVo, Long userId);

    /**
     * 踢出群成员
     * @param groupId 群组ID
     * @param kickVo 踢出信息
     * @param userId 操作者ID
     */
    void kickUser(Long groupId, GroupKickVo kickVo, Long userId);

    /**
     * 根据用户ID获取该用户加入的所有群组
     * @param userId 用户ID
     * @return 群组列表
     */
    List<Group> getGroupsByUserId(Long userId);

    /**
     * 获取用户加入的群组列表
     * @param userId 用户ID
     * @return 用户加入的群组列表
     */
    List<Group> getUserJoinedGroups(Long userId);

    /**
     * 获取用户拥有的群组列表
     * @param userId 用户ID
     * @return 用户拥有的群组列表
     */
    List<Group> getUserOwnedGroups(Long userId);

    /**
     * 获取群组详情
     * @param groupId 群组ID
     * @return 群组详情
     */
    Group getGroupDetails(Long groupId);

    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @return 群组成员列表（包含用户详细信息）
     */
    List<Map<String, Object>> getGroupMembers(Long groupId);

    /**
     * 更新群组信息
     * @param groupId 群组ID
     * @param groupData 群组更新数据
     * @param userId 操作者ID
     */
    void updateGroup(Long groupId, Group groupData, Long userId);

    /**
     * 申请加入群组
     * @param applyVo 申请信息
     * @param userId 申请者ID
     */
    void applyToJoinGroup(GroupApplyVo applyVo, Long userId);
}
