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

    boolean inviteMembers(GroupInviteVo inviteVo, Long userId); // userId as Long

    void kickMember(GroupKickVo kickVo, Long userId); // userId as Long

    boolean leaveGroup(Long groupId, Long userId); // groupId and userId as Long

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
    boolean inviteUser(Long groupId, GroupInviteVo inviteVo, Long userId);

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
    boolean applyToJoinGroup(GroupApplyVo applyVo, Long userId);

    /**
     * 根据群组ID获取群组信息
     * @param groupId 群组ID
     * @return 群组信息
     */
    Group getGroupById(Long groupId);

    /**
     * 解散群组
     * @param groupId 群组ID
     * @param userId 操作者ID
     */
    boolean deleteGroup(Long groupId, Long userId);

  
    /**
     * 同意加入群组申请
     * @param groupId 群组ID
     * @param applicationId 申请ID
     * @param userId 操作者ID
     */
    boolean approveApplication(Long groupId, Long applicationId, Long userId, String reason);

    /**
     * 拒绝加入群组申请
     * @param groupId 群组ID
     * @param applicationId 申请ID
     * @param userId 操作者ID
     * @param reason 原因
     */
    boolean rejectApplication(Long groupId, Long applicationId, Long userId, String reason);

    /**
     * 设置成员角色
     * @param groupId 群组ID
     * @param userId 成员ID
     * @param role 角色名称
     */
    boolean setMemberRole(Long groupId, Long userId, String role, Long operatorId);

    /**
     * 移除成员
     * @param groupId 群组ID
     * @param userId 成员ID
     * @param operatorId 操作者ID
     */
    boolean removeMember(Long groupId, Long userId, Long operatorId);

    /**
     * 搜索群组
     * @param keyword 关键词
     * @param type 群组类型
     * @return 搜索结果
     */
    List<Group> searchGroups(String keyword, int limit);

    /**
     * 获取用户加入的群组
     * @param userId 用户ID
     * @return 用户加入的群组列表
     */
    List<Group> getUserGroups(Long userId);

    /**
     * 获取用户创建的群组
     * @param userId 用户ID
     * @return 用户创建的群组列表
     */
    List<Group> getUserCreatedGroups(Long userId);

    /**
     * 转让群组
     * @param groupId 群组ID
     * @param newOwnerId 新群主ID
     * @param currentOwnerId 当前群主ID
     * @return 是否成功
     */
    boolean transferGroup(Long groupId, Long newOwnerId, Long currentOwnerId);
}
