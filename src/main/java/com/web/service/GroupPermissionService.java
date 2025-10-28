package com.web.service;

/**
 * 群组权限服务接口
 * 统一管理群组相关的权限检查逻辑
 * 提供缓存支持以提高性能
 */
public interface GroupPermissionService {

    /**
     * 检查用户是否为群主
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否为群主
     */
    boolean isGroupOwner(Long groupId, Long userId);

    /**
     * 检查用户是否为群组管理员（包括群主）
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否为管理员
     */
    boolean isGroupAdmin(Long groupId, Long userId);

    /**
     * 检查用户是否为群组成员
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否为成员
     */
    boolean isGroupMember(Long groupId, Long userId);

    /**
     * 检查用户是否可以管理群组（修改群组信息）
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否可以管理
     */
    boolean canManageGroup(Long groupId, Long userId);

    /**
     * 检查用户是否可以邀请成员
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否可以邀请
     */
    boolean canInviteMembers(Long groupId, Long userId);

    /**
     * 检查用户是否可以踢出成员
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @return 是否可以踢出
     */
    boolean canKickMember(Long groupId, Long userId, Long targetUserId);

    /**
     * 检查用户是否可以解散群组
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否可以解散
     */
    boolean canDissolveGroup(Long groupId, Long userId);

    /**
     * 检查用户是否可以转让群组
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否可以转让
     */
    boolean canTransferGroup(Long groupId, Long userId);

    /**
     * 检查用户是否可以发送消息
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否可以发送消息
     */
    boolean canSendMessage(Long groupId, Long userId);

    /**
     * 检查用户是否可以设置成员角色
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @param newRole 新角色
     * @return 是否可以设置
     */
    boolean canSetMemberRole(Long groupId, Long userId, Long targetUserId, int newRole);

    /**
     * 获取用户在群组中的角色
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 角色值（0=普通成员, 1=管理员, 2=群主, -1=非成员）
     */
    int getUserRoleInGroup(Long groupId, Long userId);

    /**
     * 清除用户的群组权限缓存
     * @param groupId 群组ID
     * @param userId 用户ID
     */
    void clearPermissionCache(Long groupId, Long userId);

    /**
     * 清除群组的所有权限缓存
     * @param groupId 群组ID
     */
    void clearGroupPermissionCache(Long groupId);
}
