package com.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.web.exception.WeebException;
import com.web.mapper.GroupMapper;
import com.web.mapper.GroupMemberMapper;
import com.web.mapper.UserMapper;
import com.web.model.Group;
import com.web.model.GroupMember;
import com.web.model.User;
import com.web.dto.GroupDto;
import com.web.service.GroupService;
import com.web.service.UserTypeSecurityService;
import com.web.service.AuthService;

import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import com.web.vo.group.GroupKickVo;
import com.web.vo.group.GroupApplyVo;
import com.web.constant.GroupRoleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群组服务实现类
 * 处理群组创建、成员管理、群组信息维护等业务逻辑
 * 已修复：统一使用GroupRoleConstants定义角色，避免角色定义不一致问题
 */
@Slf4j
@Service
@Transactional
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTypeSecurityService userTypeSecurityService;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.web.service.NotificationService notificationService;

    @Autowired
    private com.web.mapper.GroupApplicationMapper groupApplicationMapper;

    @Autowired
    private com.web.service.MessageBroadcastService messageBroadcastService;

    @Autowired
    private com.web.mapper.ChatListMapper chatListMapper;

    /**
     * 检查用户在群组中的权限
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param requiredRole 需要的最低角色
     * @return 是否有权限
     */
    private boolean hasGroupPermission(Long groupId, Long userId, int requiredRole) {
        try {
            if (groupId == null || userId == null) return false;
            Group group = getById(groupId);
            if (group == null || !Integer.valueOf(1).equals(group.getStatus())) return false;
            // 系统管理员拥有所有权限
            User currentUser = authService.findByUserID(userId);
            if (currentUser == null || !Integer.valueOf(1).equals(currentUser.getStatus())) return false;
            if (userTypeSecurityService.isAdmin(currentUser.getUsername())) {
                log.debug("系统管理员拥有所有群组权限: userId={}, groupId={}", userId, groupId);
                return true;
            }

            // 检查群组成员身份和角色
            GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, userId);
            if (member == null) {
                log.debug("用户不是群组成员: userId={}, groupId={}", userId, groupId);
                return false;
            }
            
            // 检查成员状态是否为已接受
            if (!isActiveMember(member)) {
                log.debug("用户未被接受为群组成员: userId={}, groupId={}, status={}", 
                    userId, groupId, member.getJoinStatus());
                return false;
            }
            if (GroupRoleConstants.isOwner(member.getRole()) && !userId.equals(group.getOwnerId())) return false;

            // 使用统一的权限检查逻辑（角色值越小，权限越高）
            boolean hasPermission = GroupRoleConstants.hasPermission(member.getRole(), requiredRole);
            log.debug("用户群组权限检查: userId={}, groupId={}, userRole={} ({}), requiredRole={} ({}), hasPermission={}",
                userId, groupId, member.getRole(), GroupRoleConstants.getRoleName(member.getRole()),
                requiredRole, GroupRoleConstants.getRoleName(requiredRole), hasPermission);

            return hasPermission;

        } catch (Exception e) {
            log.error("检查群组权限时发生异常: groupId={}, userId={}", groupId, userId, e);
            return false;
        }
    }

    private boolean isActiveMember(GroupMember member) {
        return member != null && "ACCEPTED".equals(member.getJoinStatus())
                && member.getKickedAt() == null && member.getRole() != null
                && GroupRoleConstants.isValidRole(member.getRole());
    }

    private void decrementActiveMemberCount(Long groupId) {
        if (groupMapper.decrementMemberCount(groupId) != 1) {
            throw new WeebException("群组成员计数更新失败");
        }
    }

    /**
     * 检查用户是否为群主
     */
    private boolean isGroupOwner(Long groupId, Long userId) {
        return hasGroupPermission(groupId, userId, GroupRoleConstants.ROLE_OWNER);
    }

    /**
     * 检查用户是否为群主或管理员
     */
    private boolean isGroupAdmin(Long groupId, Long userId) {
        return hasGroupPermission(groupId, userId, GroupRoleConstants.ROLE_ADMIN);
    }

    /**
     * 检查用户是否为群组成员
     */
    private boolean isGroupMember(Long groupId, Long userId) {
        return hasGroupPermission(groupId, userId, GroupRoleConstants.ROLE_MEMBER);
    }

    private void requireGroupPreviewAccess(Group group, Long userId) {
        if (Integer.valueOf(0).equals(group.getIsVisible()) && !isGroupMember(group.getId(), userId)) {
            throw new AccessDeniedException("无权查看私密群组");
        }
    }

    private Long findOrCreateGroupSharedChat(Long groupId) {
        Long sharedChatId = chatListMapper.findGroupSharedChatId(groupId);
        if (sharedChatId == null) {
            chatListMapper.createGroupSharedChat(groupId);
            sharedChatId = chatListMapper.findGroupSharedChatId(groupId);
        }
        return sharedChatId;
    }

    private void ensureGroupChatList(Group group, Long userId) {
        if (chatListMapper.selectChatListByUserIdAndSharedChatId(userId, group.getSharedChatId()) != null) return;
        com.web.model.ChatList chat = new com.web.model.ChatList();
        chat.setId(java.util.UUID.randomUUID().toString());
        chat.setUserId(userId);
        chat.setSharedChatId(group.getSharedChatId());
        chat.setGroupId(group.getId());
        chat.setTargetId(group.getId());
        chat.setType("GROUP");
        chat.setTargetInfo(group.getGroupName());
        chat.setUnreadCount(0);
        chat.setCreateTime(java.time.LocalDateTime.now());
        chat.setUpdateTime(chat.getCreateTime());
        if (chatListMapper.insertChatList(chat) != 1) throw new WeebException("创建群成员聊天列表失败");
    }

    @Override
    public Group createGroup(GroupCreateVo createVo, Long userId) {
        // 输入验证
        if (createVo == null) {
            throw new WeebException("群组创建信息不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (createVo.getGroupName() == null || createVo.getGroupName().trim().isEmpty()) {
            throw new WeebException("群组名称不能为空");
        }
        if (createVo.getGroupName().length() > 50) {
            throw new WeebException("群组名称不能超过50个字符");
        }
        if (createVo.getGroupDescription() != null && createVo.getGroupDescription().length() > 200) {
            throw new WeebException("群组描述不能超过200个字符");
        }
        
        // ✅ 2人群组类型判断：如果初始成员只有1人（加上创建者共2人）且类型为PRIVATE，建议使用私聊
        if (createVo.getInitialMemberIds() != null && createVo.getInitialMemberIds().size() == 1) {
            if ("PRIVATE".equalsIgnoreCase(createVo.getGroupType())) {
                log.warn("创建2人PRIVATE群组，建议使用私聊功能: userId={}, targetUserId={}", 
                    userId, createVo.getInitialMemberIds().get(0));
                // 注意：这里只是警告，不阻止创建。如果需要强制阻止，可以抛出异常
                // throw new WeebException("2人私密群组建议使用私聊功能，请使用聊天功能直接发送消息");
            }
        }
        
        List<Long> initialMembers = new ArrayList<>();
        if (createVo.getInitialMemberIds() != null) {
            if (createVo.getInitialMemberIds().size() > 50) throw new WeebException("初始成员不能超过50人");
            for (Long memberId : new java.util.LinkedHashSet<>(createVo.getInitialMemberIds())) {
                if (memberId == null || memberId <= 0) throw new WeebException("初始成员ID无效");
                if (memberId.equals(userId)) continue;
                User initialMember = userMapper.selectById(memberId);
                if (initialMember == null || !Integer.valueOf(1).equals(initialMember.getStatus())) {
                    throw new WeebException("初始成员不存在或已停用");
                }
                initialMembers.add(memberId);
            }
        }
        String groupType = createVo.getGroupType();
        if (groupType != null && !groupType.isBlank()
                && !"PUBLIC".equalsIgnoreCase(groupType) && !"PRIVATE".equalsIgnoreCase(groupType)) {
            throw new WeebException("群组类型必须为 PUBLIC 或 PRIVATE");
        }

        // 创建群组对象
        Group group = new Group();
        group.setGroupName(createVo.getGroupName().trim());
        group.setGroupDescription(createVo.getGroupDescription());
        group.setIsVisible("PRIVATE".equalsIgnoreCase(groupType) ? 0 : 1);
        group.setOwnerId(userId);
        // Note: Group model uses createTime (Date) instead of createdAt (LocalDateTime)
        group.setCreateTime(new Date());
        
        // 设置群组类型（如果提供）
        if (groupType != null) log.debug("群组类型: {}", groupType);
        
        // 保存群组
        boolean saved = save(group);
        if (!saved) {
            throw new WeebException("创建群组失败");
        }
        
        // 将创建者添加为群主
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole(GroupRoleConstants.ROLE_OWNER); // 使用常量定义
        ownerMember.setJoinStatus("ACCEPTED"); // 群主直接接受
        
        groupMemberMapper.insert(ownerMember);
        
        // 更新群组成员数
        group.setMemberCount(1);

        // ✅ 关键修复：为群组创建SharedChat记录
        Long sharedChatId = findOrCreateGroupSharedChat(group.getId());
        if (sharedChatId == null) {
            throw new WeebException("创建群组聊天会话失败");
        }

        // 设置群组的sharedChatId
        group.setSharedChatId(sharedChatId);

        // 保存群组（包含sharedChatId）
        updateById(group);

        // ✅ 关键修复：为群主创建ChatList记录
        ensureGroupChatList(group, userId);

        if (!initialMembers.isEmpty()) {
            GroupInviteVo invitation = new GroupInviteVo();
            invitation.setGroupId(group.getId());
            invitation.setMemberIds(initialMembers);
            inviteMembers(invitation, userId);
            group.setMemberCount(1 + initialMembers.size());
        }

        log.info("群组创建成功: groupId={}, groupName={}, ownerId={}, sharedChatId={}",
            group.getId(), group.getGroupName(), userId, sharedChatId);

        return group;
    }

    @Override
    public boolean inviteMembers(GroupInviteVo inviteVo, Long userId) {
        // 输入验证
        if (inviteVo == null) {
            throw new WeebException("邀请信息不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (inviteVo.getGroupId() == null || inviteVo.getGroupId() <= 0) {
            throw new WeebException("群组ID必须为正数");
        }
        if (inviteVo.getMemberIds() == null || inviteVo.getMemberIds().isEmpty()) {
            throw new WeebException("邀请成员列表不能为空");
        }
        
        // 检查用户是否有权限邀请（群主或管理员）
        if (!isGroupAdmin(inviteVo.getGroupId(), userId)) {
            throw new AccessDeniedException("无权限邀请成员");
        }
        
        // 获取群组信息以更新成员数
        Group group = getById(inviteVo.getGroupId());
        if (group == null) {
            throw new WeebException("群组不存在");
        }
        
        // 邀请用户加入群组
        for (Long inviteeId : inviteVo.getMemberIds()) {
            // 检查用户是否已经是群成员
            if (!groupMemberMapper.isMember(inviteVo.getGroupId(), inviteeId)) {
                // 检查群组是否已满
                if (group.getMemberCount() != null && group.getMaxMembers() != null 
                    && group.getMemberCount() >= group.getMaxMembers()) {
                    log.warn("群组已满，无法邀请更多成员: groupId={}, maxMembers={}", 
                        inviteVo.getGroupId(), group.getMaxMembers());
                    break;
                }
                
                GroupMember newMember = new GroupMember();
                newMember.setGroupId(inviteVo.getGroupId());
                newMember.setUserId(inviteeId);
                newMember.setRole(GroupRoleConstants.ROLE_MEMBER); // 使用常量定义
                newMember.setJoinStatus("ACCEPTED"); // 邀请直接接受
                newMember.setInvitedBy(userId); // 记录邀请人
                
                if (groupMapper.incrementMemberCountIfCapacity(group.getId()) != 1) {
                    break;
                }
                if (groupMemberMapper.insert(newMember) != 1) throw new WeebException("添加群成员失败");

                // ✅ 关键修复：为新成员创建ChatList记录
                try {
                    ensureGroupChatList(group, inviteeId);
                } catch (Exception e) {
                    log.error("创建新成员ChatList记录时发生异常: groupId={}, userId={}", group.getId(), inviteeId, e);
                    // 不影响成员邀请成功
                }

                notificationService.createAndPublishNotification(inviteeId, userId, "GROUP_INVITE", "GROUP", group.getId());

                log.info("成员邀请成功: groupId={}, inviteeId={}, inviterId={}",
                    inviteVo.getGroupId(), inviteeId, userId);

                // ✅ 广播群组成员变更事件
                try {
                    messageBroadcastService.broadcastGroupMemberChange(
                        inviteVo.getGroupId(),
                        "MEMBER_ADDED",
                        inviteeId,
                        userId,
                        null
                    );
                } catch (Exception e) {
                    log.error("广播群组成员变更失败: groupId={}, inviteeId={}", 
                        inviteVo.getGroupId(), inviteeId, e);
                }
            }
        }
        
        return true;
    }

    @Override
    public void kickMember(GroupKickVo kickVo, Long userId) {
        // 输入验证
        if (kickVo == null) {
            throw new WeebException("踢出信息不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (kickVo.getGroupId() == null || kickVo.getGroupId() <= 0) {
            throw new WeebException("群组ID必须为正数");
        }
        if (kickVo.getKickedUserId() == null || kickVo.getKickedUserId() <= 0) {
            throw new WeebException("被踢用户ID必须为正数");
        }
        if (userId.equals(kickVo.getKickedUserId())) {
            throw new WeebException("不能踢出自己");
        }
        
        // 检查操作者是否有权限踢人（群主或管理员）
        if (!isGroupAdmin(kickVo.getGroupId(), userId)) {
            throw new AccessDeniedException("无权限踢出成员");
        }

        // 检查被踢用户是否为群成员
        GroupMember targetMember = groupMemberMapper.findByGroupAndUser(kickVo.getGroupId(), kickVo.getKickedUserId());
        if (!isActiveMember(targetMember)) {
            throw new AccessDeniedException("用户不是有效群成员");
        }

        // 不能踢出群主
        if (GroupRoleConstants.isOwner(targetMember.getRole()) || kickVo.getKickedUserId().equals(getById(kickVo.getGroupId()).getOwnerId())) {
            throw new AccessDeniedException("不能踢出群主");
        }

        // 检查操作者权限：只有群主可以踢出管理员
        if (GroupRoleConstants.isAdminOrOwner(targetMember.getRole())) {
            if (!isGroupOwner(kickVo.getGroupId(), userId)) {
                throw new AccessDeniedException("只有群主可以踢出管理员");
            }
        }

        int removed = groupMemberMapper.delete(new QueryWrapper<GroupMember>()
                .eq("id", targetMember.getId()).eq("role", targetMember.getRole())
                .eq("join_status", "ACCEPTED").isNull("kicked_at"));
        if (removed != 1) throw new WeebException("成员状态已变更，请重试");
        decrementActiveMemberCount(kickVo.getGroupId());
        
        log.info("成员被踢出: groupId={}, kickedUserId={}, operatorId={}, reason={}", 
            kickVo.getGroupId(), kickVo.getKickedUserId(), userId, kickVo.getReason());
        
        // ✅ 广播群组成员变更事件
        try {
            java.util.Map<String, Object> additionalData = new java.util.HashMap<>();
            if (kickVo.getReason() != null) {
                additionalData.put("reason", kickVo.getReason());
            }
            messageBroadcastService.broadcastGroupMemberChange(
                kickVo.getGroupId(),
                "MEMBER_REMOVED",
                kickVo.getKickedUserId(),
                userId,
                additionalData
            );
        } catch (Exception e) {
            log.error("广播群组成员变更失败: groupId={}, kickedUserId={}", 
                kickVo.getGroupId(), kickVo.getKickedUserId(), e);
        }
    }

    @Override
    public boolean leaveGroup(Long groupId, Long userId) {
        try {
            // 输入验证
            if (groupId == null || groupId <= 0) {
                throw new WeebException("群组ID必须为正数");
            }
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }
            
            quitGroup(groupId, userId);
            return true;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void dissolveGroup(Long groupId, Long userId) {
        // 输入验证
        if (groupId == null || groupId <= 0) {
            throw new WeebException("群组ID必须为正数");
        }
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        
        // 检查群组是否存在
        Group group = getById(groupId);
        if (group == null) {
            throw new WeebException("群组不存在");
        }

        // 检查用户是否为群主或管理员（管理员有特殊权限）
        if (!isGroupOwner(groupId, userId)) {
            throw new AccessDeniedException("只有群主可以解散群组");
        }
        
        // ✅ 先广播群组解散事件（在删除前，这样成员还能收到通知）
        try {
            messageBroadcastService.broadcastGroupInfoChange(
                groupId,
                "GROUP_DISSOLVED",
                userId,
                null
            );
        } catch (Exception e) {
            log.error("广播群组解散事件失败: groupId={}", groupId, e);
        }
        
        // 删除所有群成员
        List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(groupId);
        for (Long memberId : memberIds) {
            GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, memberId);
            if (member != null) {
                groupMemberMapper.deleteById(member.getId());
            }
        }
        
        // 删除群组
        removeById(groupId);
    }

    @Override
    public void quitGroup(Long groupId, Long userId) {
        // 输入验证
        if (groupId == null || groupId <= 0) {
            throw new WeebException("群组ID必须为正数");
        }
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        
        // 检查用户是否为群成员
        GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, userId);
        if (!isActiveMember(member)) {
            throw new AccessDeniedException("用户不是有效群成员");
        }
        
        // 群主不能直接退出，需要先转让群主或解散群组
        if (GroupRoleConstants.isOwner(member.getRole()) || userId.equals(getById(groupId).getOwnerId())) {
            throw new AccessDeniedException("群主不能退出群组，请先转让群主或解散群组");
        }
        
        // 移除群成员
        int removed = groupMemberMapper.delete(new QueryWrapper<GroupMember>()
                .eq("id", member.getId()).eq("role", member.getRole())
                .eq("join_status", "ACCEPTED").isNull("kicked_at"));
        if (removed != 1) throw new WeebException("成员状态已变更，请重试");
        decrementActiveMemberCount(groupId);
        
        log.info("成员退出群组: groupId={}, userId={}", groupId, userId);
        
        // ✅ 广播群组成员变更事件
        try {
            messageBroadcastService.broadcastGroupMemberChange(
                groupId,
                "MEMBER_LEFT",
                userId,
                userId,
                null
            );
        } catch (Exception e) {
            log.error("广播群组成员变更失败: groupId={}, userId={}", groupId, userId, e);
        }
    }

    @Override
    public boolean inviteUser(Long groupId, GroupInviteVo inviteVo, Long userId) {
        try {
            inviteVo.setGroupId(groupId);
            return inviteMembers(inviteVo, userId);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void kickUser(Long groupId, GroupKickVo kickVo, Long userId) {
        kickVo.setGroupId(groupId);
        kickMember(kickVo, userId);
    }

    @Override
    public List<Group> getGroupsByUserId(Long userId) {
        return groupMapper.findGroupsByUserId(userId);
    }

    @Override
    public List<Group> getUserJoinedGroups(Long userId) {
        return getGroupsByUserId(userId);
    }

    @Override
    public List<Group> getUserOwnedGroups(Long userId) {
        return groupMapper.findGroupsByOwnerId(userId);
    }

    @Override
    public Group getGroupDetails(Long groupId) {
        return getById(groupId);
    }

    @Override
    public List<Map<String, Object>> getGroupMembers(Long groupId, Long userId) {
        Group group = getById(groupId);
        if (group == null) throw new WeebException("群组不存在");
        requireGroupPreviewAccess(group, userId);
        List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(groupId);
        List<Map<String, Object>> members = new ArrayList<>();
        
        for (Long memberId : memberIds) {
            User user = userMapper.selectById(memberId);
            GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, memberId);
            
            if (user != null && isActiveMember(member)) {
                Map<String, Object> memberInfo = new HashMap<>();
                memberInfo.put("userId", user.getId());
                memberInfo.put("username", user.getUsername());
                memberInfo.put("nickname", user.getNickname());
                memberInfo.put("avatar", user.getAvatar());
                memberInfo.put("role", member.getRole());
                // 注意：根据GroupMember实际字段获取加入时间
                // memberInfo.put("joinedAt", member.getJoinedAt());
                
                members.add(memberInfo);
            }
        }
        
        return members;
    }

    @Override
    public void updateGroup(Long groupId, Group groupData, Long userId) {
        // 检查用户是否有权限更新群组信息（是否为群主或管理员）
        if (!isGroupAdmin(groupId, userId)) {
            throw new AccessDeniedException("无权限更新群组信息");
        }
        
        // 获取现有群组信息
        Group existingGroup = getById(groupId);
        if (existingGroup == null) {
            throw new WeebException("群组不存在");
        }
        
        // 更新群组信息
        boolean hasChanges = false;
        java.util.Map<String, Object> changes = new java.util.HashMap<>();
        UpdateWrapper<Group> update = new UpdateWrapper<Group>().eq("id", groupId);
        
        if (groupData.getGroupName() != null && !groupData.getGroupName().equals(existingGroup.getGroupName())) {
            changes.put("oldGroupName", existingGroup.getGroupName());
            changes.put("newGroupName", groupData.getGroupName());
            update.set("group_name", groupData.getGroupName());
            hasChanges = true;
        }
        if (groupData.getGroupAvatarUrl() != null && !groupData.getGroupAvatarUrl().equals(existingGroup.getGroupAvatarUrl())) {
            changes.put("oldGroupAvatarUrl", existingGroup.getGroupAvatarUrl());
            changes.put("newGroupAvatarUrl", groupData.getGroupAvatarUrl());
            update.set("group_avatar_url", groupData.getGroupAvatarUrl());
            hasChanges = true;
        }
        if (groupData.getGroupDescription() != null && !groupData.getGroupDescription().equals(existingGroup.getGroupDescription())) {
            update.set("group_description", groupData.getGroupDescription());
            changes.put("newGroupDescription", groupData.getGroupDescription());
            hasChanges = true;
        }
        // Note: Group model doesn't have updatedAt field, using createTime for last update
        // existingGroup.setCreateTime(new Date()); // Uncomment if you want to update timestamp
        
        // 保存更新
        if (hasChanges && groupMapper.update(null, update) != 1) throw new WeebException("群组信息更新失败");
        
        // ✅ 广播群组信息变更事件
        if (hasChanges) {
            try {
                messageBroadcastService.broadcastGroupInfoChange(
                    groupId,
                    "INFO_UPDATED",
                    userId,
                    changes
                );
            } catch (Exception e) {
                log.error("广播群组信息变更失败: groupId={}", groupId, e);
            }
        }
    }

    @Override
    public boolean applyToJoinGroup(GroupApplyVo applyVo, Long userId) {
        try {
            // 输入验证
            if (applyVo == null || applyVo.getGroupId() == null) {
                throw new WeebException("申请信息不完整");
            }

            // 检查群组是否存在
            Group group = getById(applyVo.getGroupId());
            if (group == null) {
                throw new WeebException("群组不存在");
            }

            // 检查群组状态
            if (group.getStatus() != 1) {
                throw new WeebException("群组已解散或冻结，无法申请加入");
            }
            if (Integer.valueOf(0).equals(group.getIsVisible())) {
                throw new AccessDeniedException("私密群组仅允许邀请加入");
            }

            // 检查用户是否已经是群成员
            if (groupMemberMapper.isMember(applyVo.getGroupId(), userId)) {
                throw new WeebException("您已经是群组成员");
            }

            // 检查是否已有待审批的申请
            if (groupApplicationMapper.hasPendingApplication(applyVo.getGroupId(), userId)) {
                throw new WeebException("您已提交过申请，请等待审核");
            }

            // 创建申请记录
            com.web.model.GroupApplication application = new com.web.model.GroupApplication();
            application.setGroupId(applyVo.getGroupId());
            application.setUserId(userId);
            application.setMessage(applyVo.getMessage() != null ? applyVo.getMessage() : applyVo.getReason());
            application.setStatus("PENDING");

            groupApplicationMapper.insert(application);
            
            // 发送群组申请通知给群主
            try {
                notificationService.createAndPublishNotification(
                    group.getOwnerId(),      // 接收者：群主
                    userId,                  // 操作者：申请人
                    "GROUP_APPLICATION",     // 通知类型
                    "GROUP",                 // 实体类型
                    applyVo.getGroupId()     // 实体ID：群组ID
                );
                log.info("群组申请通知已发送 - 群主ID: {}, 申请人ID: {}, 群组ID: {}", 
                         group.getOwnerId(), userId, applyVo.getGroupId());
            } catch (Exception e) {
                log.error("发送群组申请通知失败", e);
                // 不抛出异常，通知失败不应影响群组申请
            }
            
            log.info("用户申请加入群组成功: groupId={}, userId={}", applyVo.getGroupId(), userId);
            return true;
        } catch (AccessDeniedException e) {
            throw e;
        } catch (WeebException e) {
            log.warn("申请加入群组失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("申请加入群组失败: groupId={}, userId={}", applyVo.getGroupId(), userId, e);
            throw new WeebException("申请加入群组失败: " + e.getMessage());
        }
    }

    @Override
    public List<Group> getUserGroups(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }

            // 获取用户加入的所有群组
            return groupMapper.findGroupsByUserId(userId);
        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("获取用户群组失败: " + e.getMessage());
        }
    }

    @Override
    public List<Group> getUserCreatedGroups(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }

            // 根据创建者ID查询群组
            return groupMapper.findGroupsByOwnerId(userId);
        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("获取用户创建的群组失败: " + e.getMessage());
        }
    }

    @Override
    public List<Group> searchGroups(String keyword, int limit) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return new ArrayList<>();
            }

            // 搜索群组名称包含关键词的群组
            return groupMapper.searchGroups(keyword.trim(), 0, limit); // 返回指定数量的结果
        } catch (Exception e) {
            throw new WeebException("搜索群组失败: " + e.getMessage());
        }
    }

    @Override
    public Group getGroupById(Long groupId) {
        return getById(groupId);
    }

    @Override
    public boolean deleteGroup(Long groupId, Long userId) {
        try {
            dissolveGroup(groupId, userId);
            return true;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean approveApplication(Long groupId, Long applicationId, Long userId, String reason) {
        try {
            log.info("开始批准群组申请: groupId={}, applicationId={}, reviewerId={}", groupId, applicationId, userId);

            // 1. 检查群组是否存在
            Group group = getById(groupId);
            if (group == null) {
                log.warn("群组不存在: groupId={}", groupId);
                throw new WeebException("群组不存在");
            }

            // 2. 检查操作者是否有权限批准申请（群主或管理员）
            if (!isGroupAdmin(groupId, userId)) {
                log.warn("用户无权限批准申请: groupId={}, userId={}", groupId, userId);
                throw new org.springframework.security.access.AccessDeniedException("只有群主或管理员可以批准申请");
            }

            // 3. 获取申请记录
            com.web.model.GroupApplication application = groupApplicationMapper.selectById(applicationId);
            if (application == null || !groupId.equals(application.getGroupId())) {
                throw new org.springframework.security.access.AccessDeniedException("Application is unavailable for this group");
            }

            // 4. 检查申请状态
            if (!"PENDING".equals(application.getStatus())) {
                log.warn("申请已被处理: applicationId={}, status={}", applicationId, application.getStatus());
                throw new WeebException("申请已被处理");
            }

            // 5. 检查申请人是否已经是群成员
            if (groupMemberMapper.isMember(groupId, application.getUserId())) {
                log.warn("申请人已是群成员: groupId={}, userId={}", groupId, application.getUserId());
                throw new WeebException("申请人已是群组成员");
            }

            // 6. 检查群组是否已满
            if (group.getMemberCount() != null && group.getMaxMembers() != null 
                && group.getMemberCount() >= group.getMaxMembers()) {
                log.warn("群组已满: groupId={}, memberCount={}, maxMembers={}", 
                    groupId, group.getMemberCount(), group.getMaxMembers());
                throw new WeebException("群组已满，无法加入");
            }

            // 7. 更新申请状态
            application.setStatus("APPROVED");
            application.setReviewerId(userId);
            application.setReviewNote(reason);
            application.setReviewedAt(new java.util.Date());
            int decided = groupApplicationMapper.update(application,
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<com.web.model.GroupApplication>()
                            .eq("id", applicationId).eq("group_id", groupId).eq("status", "PENDING"));
            if (decided != 1) throw new WeebException("Application has already been processed");

            // Reserve capacity atomically; a failure rolls back the pending decision.
            if (groupMapper.incrementMemberCountIfCapacity(groupId) != 1) {
                throw new WeebException("群组已满或不可用，无法加入");
            }

            // 8. 添加用户为群成员
            GroupMember newMember = new GroupMember();
            newMember.setGroupId(groupId);
            newMember.setUserId(application.getUserId());
            newMember.setRole(GroupRoleConstants.ROLE_MEMBER);
            newMember.setJoinStatus("ACCEPTED");
            newMember.setInvitedBy(userId); // 记录审批人
            newMember.setInviteReason("申请通过");
            if (groupMemberMapper.insert(newMember) != 1) throw new WeebException("添加群成员失败");

            // ✅ 关键修复：为新批准的成员创建ChatList记录
            try {
                ensureGroupChatList(group, application.getUserId());
            } catch (Exception e) {
                log.error("创建新批准成员ChatList记录时发生异常: groupId={}, userId={}", group.getId(), application.getUserId(), e);
                // 不影响成员批准成功
            }

            // 9.5. ✅ 广播群组成员变更事件
            try {
                messageBroadcastService.broadcastGroupMemberChange(
                    groupId,
                    "MEMBER_ADDED",
                    application.getUserId(),
                    userId,
                    null
                );
            } catch (Exception e) {
                log.error("广播群组成员变更失败: groupId={}, newMemberId={}", 
                    groupId, application.getUserId(), e);
            }

            // 10. 发送批准通知给申请人
            try {
                notificationService.createAndPublishNotification(
                    application.getUserId(),         // 接收者：申请人
                    userId,                          // 操作者：批准人
                    "GROUP_APPLICATION_APPROVED",   // 通知类型
                    "GROUP",                         // 实体类型
                    groupId                          // 实体ID：群组ID
                );
                log.info("群组申请批准通知已发送 - 申请人ID: {}, 批准人ID: {}, 群组ID: {}", 
                         application.getUserId(), userId, groupId);
            } catch (Exception e) {
                log.error("发送群组申请批准通知失败", e);
            }

            log.info("批准群组申请成功: groupId={}, applicationId={}, applicantId={}", 
                groupId, applicationId, application.getUserId());
            return true;

        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (WeebException e) {
            log.error("批准群组申请失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("批准群组申请失败: groupId={}, applicationId={}, userId={}", groupId, applicationId, userId, e);
            throw new WeebException("批准申请失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean rejectApplication(Long groupId, Long applicationId, Long userId, String reason) {
        try {
            log.info("开始拒绝群组申请: groupId={}, applicationId={}, reviewerId={}", groupId, applicationId, userId);

            // 1. 检查群组是否存在
            Group group = getById(groupId);
            if (group == null) {
                log.warn("群组不存在: groupId={}", groupId);
                throw new WeebException("群组不存在");
            }

            // 2. 检查操作者是否有权限拒绝申请（群主或管理员）
            if (!isGroupAdmin(groupId, userId)) {
                log.warn("用户无权限拒绝申请: groupId={}, userId={}", groupId, userId);
                throw new org.springframework.security.access.AccessDeniedException("只有群主或管理员可以拒绝申请");
            }

            // 3. 获取申请记录
            com.web.model.GroupApplication application = groupApplicationMapper.selectById(applicationId);
            if (application == null || !groupId.equals(application.getGroupId())) {
                throw new org.springframework.security.access.AccessDeniedException("Application is unavailable for this group");
            }

            // 4. 检查申请状态
            if (!"PENDING".equals(application.getStatus())) {
                log.warn("申请已被处理: applicationId={}, status={}", applicationId, application.getStatus());
                throw new WeebException("申请已被处理");
            }

            // 5. 更新申请状态
            application.setStatus("REJECTED");
            application.setReviewerId(userId);
            application.setReviewNote(reason != null ? reason : "申请被拒绝");
            application.setReviewedAt(new java.util.Date());
            int decided = groupApplicationMapper.update(application,
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<com.web.model.GroupApplication>()
                            .eq("id", applicationId).eq("group_id", groupId).eq("status", "PENDING"));
            if (decided != 1) throw new WeebException("Application has already been processed");

            // 6. 发送拒绝通知给申请人
            try {
                notificationService.createAndPublishNotification(
                    application.getUserId(),         // 接收者：申请人
                    userId,                          // 操作者：拒绝人
                    "GROUP_APPLICATION_REJECTED",   // 通知类型
                    "GROUP",                         // 实体类型
                    groupId                          // 实体ID：群组ID
                );
                log.info("群组申请拒绝通知已发送 - 申请人ID: {}, 拒绝人ID: {}, 群组ID: {}", 
                         application.getUserId(), userId, groupId);
            } catch (Exception e) {
                log.error("发送群组申请拒绝通知失败", e);
            }

            log.info("拒绝群组申请成功: groupId={}, applicationId={}, applicantId={}", 
                groupId, applicationId, application.getUserId());
            return true;

        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (WeebException e) {
            log.error("拒绝群组申请失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("拒绝群组申请失败: groupId={}, applicationId={}, userId={}", groupId, applicationId, userId, e);
            throw new WeebException("拒绝申请失败: " + e.getMessage());
        }
    }

    @Override
    public boolean setMemberRole(Long groupId, Long userId, String role, Long operatorId) {
        try {
            if (!isGroupOwner(groupId, operatorId)) throw new AccessDeniedException("只有群主可以设置管理员");
            final int roleValue;
            if ("admin".equalsIgnoreCase(role)) roleValue = GroupRoleConstants.ROLE_ADMIN;
            else if ("member".equalsIgnoreCase(role)) roleValue = GroupRoleConstants.ROLE_MEMBER;
            else throw new WeebException("角色必须为 admin 或 member");

            // 获取目标成员
            GroupMember targetMember = groupMemberMapper.findByGroupAndUser(groupId, userId);
            Group group = getById(groupId);
            if (!isActiveMember(targetMember)) throw new AccessDeniedException("目标用户不是有效群成员");
            if (GroupRoleConstants.isOwner(targetMember.getRole()) || userId.equals(group.getOwnerId())) {
                throw new AccessDeniedException("不能通过成员角色接口修改群主");
            }

            int oldRole = targetMember.getRole();
            if (oldRole == roleValue) return true;
            int updated = groupMemberMapper.update(null, new UpdateWrapper<GroupMember>()
                    .eq("id", targetMember.getId()).eq("group_id", groupId)
                    .eq("role", oldRole).eq("join_status", "ACCEPTED").isNull("kicked_at")
                    .set("role", roleValue));
            if (updated != 1) throw new WeebException("成员状态已变更，请重试");
            
            // ✅ 广播群组成员角色变更事件
            try {
                java.util.Map<String, Object> additionalData = new java.util.HashMap<>();
                additionalData.put("oldRole", oldRole);
                additionalData.put("newRole", roleValue);
                additionalData.put("roleName", role);
                messageBroadcastService.broadcastGroupMemberChange(
                    groupId,
                    "ROLE_CHANGED",
                    userId,
                    operatorId,
                    additionalData
                );
            } catch (Exception e) {
                log.error("广播群组成员角色变更失败: groupId={}, userId={}", groupId, userId, e);
            }
            
            return true;
        } catch (AccessDeniedException | WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("设置成员角色失败: " + e.getMessage());
        }
    }

    @Override
    public boolean removeMember(Long groupId, Long userId, Long operatorId) {
        try {
            if (!isGroupAdmin(groupId, operatorId)) throw new AccessDeniedException("无权限移除成员");

            // 获取目标成员
            GroupMember targetMember = groupMemberMapper.findByGroupAndUser(groupId, userId);
            if (!isActiveMember(targetMember)) throw new AccessDeniedException("目标用户不是有效群成员");

            // 不能移除群主
            Group group = getById(groupId);
            if (GroupRoleConstants.isOwner(targetMember.getRole()) || userId.equals(group.getOwnerId())) {
                throw new AccessDeniedException("不能移除群主");
            }
            if (targetMember.getRole() == GroupRoleConstants.ROLE_ADMIN && !isGroupOwner(groupId, operatorId)) {
                throw new AccessDeniedException("只有群主可以移除管理员");
            }
            if (userId.equals(operatorId)) throw new AccessDeniedException("请通过退出群组接口退出");

            int removed = groupMemberMapper.delete(new QueryWrapper<GroupMember>()
                    .eq("id", targetMember.getId()).eq("group_id", groupId)
                    .eq("role", targetMember.getRole()).eq("join_status", "ACCEPTED").isNull("kicked_at"));
            if (removed != 1) throw new WeebException("成员状态已变更，请重试");
            decrementActiveMemberCount(groupId);
            
            // ✅ 广播群组成员变更事件
            try {
                messageBroadcastService.broadcastGroupMemberChange(
                    groupId,
                    "MEMBER_REMOVED",
                    userId,
                    operatorId,
                    null
                );
            } catch (Exception e) {
                log.error("广播群组成员变更失败: groupId={}, userId={}", groupId, userId, e);
            }
            
            return true;
        } catch (AccessDeniedException | WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("移除群组成员失败: groupId={}, userId={}, operatorId={}", groupId, userId, operatorId, e);
            throw new WeebException("移除群组成员失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean transferGroup(Long groupId, Long newOwnerId, Long currentOwnerId) {
        try {
            log.info("开始转让群组: groupId={}, from={}, to={}", groupId, currentOwnerId, newOwnerId);

            // 1. 验证群组是否存在
            Group group = getById(groupId);
            if (group == null) {
                log.warn("群组不存在: groupId={}", groupId);
                throw new WeebException("群组不存在");
            }

            // 2. 验证当前用户是否为群主
            if (!currentOwnerId.equals(group.getOwnerId()) || !isGroupOwner(groupId, currentOwnerId)) {
                log.warn("用户不是群主，无权转让: groupId={}, userId={}", groupId, currentOwnerId);
                throw new AccessDeniedException("只有有效群主可以转让群组");
            }

            // 3. 验证新群主是否为群成员
            GroupMember newOwnerMember = groupMemberMapper.findByGroupAndUser(groupId, newOwnerId);
            User newOwner = authService.findByUserID(newOwnerId);
            if (!isActiveMember(newOwnerMember) || newOwner == null || !Integer.valueOf(1).equals(newOwner.getStatus())) {
                log.warn("新群主不是群成员: groupId={}, newOwnerId={}", groupId, newOwnerId);
                throw new AccessDeniedException("新群主必须是有效群组成员");
            }

            // 4. 验证新群主不是当前群主
            if (currentOwnerId.equals(newOwnerId)) {
                log.warn("不能转让给自己: groupId={}, userId={}", groupId, currentOwnerId);
                throw new WeebException("不能转让给自己");
            }

            // 5. 更新群组所有者
            int transferred = groupMapper.update(null, new UpdateWrapper<Group>()
                    .eq("id", groupId).eq("owner_id", currentOwnerId).eq("status", 1)
                    .set("owner_id", newOwnerId).set("last_transfer_at", new Date())
                    .setSql("transfer_count = COALESCE(transfer_count, 0) + 1"));
            if (transferred != 1) throw new WeebException("群主状态已变更，请重试");

            // 6. 更新新群主的角色为群主
            int promoted = groupMemberMapper.update(null, new UpdateWrapper<GroupMember>()
                    .eq("id", newOwnerMember.getId()).eq("role", newOwnerMember.getRole())
                    .eq("join_status", "ACCEPTED").isNull("kicked_at")
                    .set("role", GroupRoleConstants.ROLE_OWNER));
            if (promoted != 1) throw new WeebException("目标成员状态已变更，请重试");

            // 7. 更新原群主的角色为普通成员
            GroupMember oldOwnerMember = groupMemberMapper.findByGroupAndUser(groupId, currentOwnerId);
            if (!isActiveMember(oldOwnerMember)) throw new AccessDeniedException("原群主成员身份已失效");
            int demoted = groupMemberMapper.update(null, new UpdateWrapper<GroupMember>()
                    .eq("id", oldOwnerMember.getId()).eq("role", GroupRoleConstants.ROLE_OWNER)
                    .eq("join_status", "ACCEPTED").isNull("kicked_at")
                    .set("role", GroupRoleConstants.ROLE_MEMBER));
            if (demoted != 1) throw new WeebException("原群主状态已变更，请重试");

            // 8. 记录转让历史
            // TODO: 实现群组转让历史记录功能
            log.info("群组转让历史记录功能待实现: groupId={}, from={}, to={}", groupId, currentOwnerId, newOwnerId);

            // 8.5. ✅ 广播群组信息变更事件（群主转让）
            try {
                java.util.Map<String, Object> additionalData = new java.util.HashMap<>();
                additionalData.put("oldOwnerId", currentOwnerId);
                additionalData.put("newOwnerId", newOwnerId);
                messageBroadcastService.broadcastGroupInfoChange(
                    groupId,
                    "OWNER_TRANSFERRED",
                    currentOwnerId,
                    additionalData
                );
            } catch (Exception e) {
                log.error("广播群组转让事件失败: groupId={}, from={}, to={}", 
                    groupId, currentOwnerId, newOwnerId, e);
            }

            log.info("群组转让成功: groupId={}, from={}, to={}", groupId, currentOwnerId, newOwnerId);
            return true;

        } catch (AccessDeniedException e) {
            throw e;
        } catch (WeebException e) {
            log.error("群组转让失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("群组转让失败: groupId={}, from={}, to={}", groupId, currentOwnerId, newOwnerId, e);
            throw new WeebException("群组转让失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupDto> getUserGroupsWithDetails(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }
            
            log.debug("获取用户群组详细信息: userId={}", userId);
            
            // 使用新的Mapper方法获取用户群组详细信息
            List<GroupDto> groups = groupMapper.selectUserGroupsWithDetails(userId);
            
            log.debug("获取到 {} 个群组", groups != null ? groups.size() : 0);
            
            return groups != null ? groups : new ArrayList<>();
            
        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户群组详细信息失败: userId={}", userId, e);
            throw new WeebException("获取用户群组详细信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupDto> getUserCreatedGroupsWithDetails(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }
            
            log.debug("获取用户创建的群组详细信息: userId={}", userId);
            
            // 使用新的Mapper方法获取用户创建的群组详细信息
            List<GroupDto> groups = groupMapper.selectUserCreatedGroupsWithDetails(userId);
            
            log.debug("获取到 {} 个创建的群组", groups != null ? groups.size() : 0);
            
            return groups != null ? groups : new ArrayList<>();
            
        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户创建的群组详细信息失败: userId={}", userId, e);
            throw new WeebException("获取用户创建的群组详细信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDto getGroupWithDetails(Long groupId, Long userId) {
        try {
            if (groupId == null || groupId <= 0) {
                throw new WeebException("群组ID必须为正数");
            }

            // 获取群组基本信息
            Group group = getById(groupId);
            if (group == null) {
                throw new WeebException("群组不存在");
            }

            requireGroupPreviewAccess(group, userId);

            // ✅ 自动修复：如果群组没有 sharedChatId，自动创建
            if (group.getSharedChatId() == null) {
                log.warn("⚠️ 群组缺少 sharedChatId，自动创建: groupId={}", groupId);
                try {
                    Long sharedChatId = findOrCreateGroupSharedChat(group.getId());
                    if (sharedChatId != null) {
                        group.setSharedChatId(sharedChatId);
                        groupMapper.update(null, new UpdateWrapper<Group>().eq("id", groupId)
                                .isNull("shared_chat_id").set("shared_chat_id", sharedChatId));
                        log.info("✅ 自动为群组创建 sharedChatId: groupId={}, sharedChatId={}", groupId, sharedChatId);
                        
                        // 为所有群成员创建 chat_list 记录
                        List<GroupMember> members = groupMemberMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<GroupMember>()
                                .eq("group_id", groupId)
                        );
                        for (GroupMember member : members) {
                            if (isActiveMember(member)) {
                                try {
                                    // 检查是否已存在
                                    com.web.model.ChatList existingChatList = chatListMapper.selectChatListByUserIdAndSharedChatId(
                                        member.getUserId(), sharedChatId);
                                    
                                    if (existingChatList == null) {
                                        com.web.model.ChatList chatList = new com.web.model.ChatList();
                                        chatList.setId(java.util.UUID.randomUUID().toString());
                                        chatList.setUserId(member.getUserId());
                                        chatList.setSharedChatId(sharedChatId);
                                        chatList.setGroupId(groupId);
                                        chatList.setType("GROUP");
                                        chatList.setTargetInfo(group.getGroupName());
                                        chatList.setUnreadCount(0);
                                        chatList.setCreateTime(java.time.LocalDateTime.now());
                                        chatList.setUpdateTime(java.time.LocalDateTime.now());
                                        chatListMapper.insertChatList(chatList);
                                        log.debug("✅ 为群成员创建 chat_list: userId={}, groupId={}", member.getUserId(), groupId);
                                    }
                                } catch (Exception e) {
                                    log.error("为群成员创建 chat_list 失败: userId={}, groupId={}", member.getUserId(), groupId, e);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("自动创建 sharedChatId 失败: groupId={}", groupId, e);
                }
            }
            
            // 创建GroupDto对象
            GroupDto groupDto = new GroupDto();
            groupDto.setId(group.getId());
            groupDto.setGroupName(group.getGroupName());
            groupDto.setGroupDescription(group.getGroupDescription());
            groupDto.setOwnerId(group.getOwnerId());
            groupDto.setSharedChatId(group.getSharedChatId()); // ✅ 添加 sharedChatId
            groupDto.setGroupAvatarUrl(group.getGroupAvatarUrl());
            groupDto.setStatus(group.getStatus() != null ? String.valueOf(group.getStatus()) : "1");
            groupDto.setMaxMembers(group.getMaxMembers());
            groupDto.setMemberCount(group.getMemberCount());
            // 修复：将Date转换为LocalDateTime
            if (group.getCreateTime() != null) {
                groupDto.setCreatedAt(new java.sql.Timestamp(group.getCreateTime().getTime()).toLocalDateTime());
            }
            if (group.getLastTransferAt() != null) {
                groupDto.setLastTransferAt(new java.sql.Timestamp(group.getLastTransferAt().getTime()).toLocalDateTime());
            }
            groupDto.setTransferCount(group.getTransferCount());

            // 获取群主用户名
            if (group.getOwnerId() != null) {
                User owner = userMapper.selectById(group.getOwnerId());
                if (owner != null) {
                    groupDto.setOwnerUsername(owner.getUsername());
                }
            }

            // ✅ 关键修复：查询当前用户在群组中的角色
            if (userId != null && userId > 0) {
                GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, userId);
                if (isActiveMember(member) && (!GroupRoleConstants.isOwner(member.getRole())
                        || userId.equals(group.getOwnerId()))) {
                    // 设置角色数值
                    groupDto.setRole(member.getRole());
                    
                    // ✅ 设置角色名称
                    String roleName;
                    switch (member.getRole()) {
                        case 1: // GroupRoleConstants.ROLE_OWNER
                            roleName = "OWNER";
                            break;
                        case 2: // GroupRoleConstants.ROLE_ADMIN
                            roleName = "ADMIN";
                            break;
                        case 3: // GroupRoleConstants.ROLE_MEMBER
                            roleName = "MEMBER";
                            break;
                        default:
                            roleName = "MEMBER";
                    }
                    groupDto.setCurrentUserRole(roleName);
                    
                    log.debug("用户在群组中的角色: userId={}, groupId={}, role={}, roleName={}", 
                        userId, groupId, member.getRole(), roleName);
                } else {
                    // 用户不是群组成员
                    groupDto.setCurrentUserRole("NON_MEMBER");
                    log.debug("用户不是群组成员: userId={}, groupId={}", userId, groupId);
                }
            } else {
                // 未提供用户ID，设置为非成员
                groupDto.setCurrentUserRole("NON_MEMBER");
            }

            return groupDto;
        } catch (AccessDeniedException e) {
            throw e;
        } catch (WeebException e) {
            log.warn("获取群组详情失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取群组详情失败: groupId={}, userId={}", groupId, userId, e);
            throw new WeebException("获取群组详情失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.web.model.GroupApplication> getPendingApplications(Long groupId, Long userId) {
        try {
            // 检查操作者是否有权限查看申请（群主或管理员）
            if (!isGroupAdmin(groupId, userId)) {
                throw new AccessDeniedException("只有群主或管理员可以查看申请列表");
            }

            return groupApplicationMapper.findPendingApplicationsByGroupId(groupId);
        } catch (AccessDeniedException | WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取待审批申请列表失败: groupId={}, userId={}", groupId, userId, e);
            throw new WeebException("获取待审批申请列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.web.model.GroupApplication> getAllApplications(Long groupId, Long userId) {
        try {
            // 检查操作者是否有权限查看申请（群主或管理员）
            if (!isGroupAdmin(groupId, userId)) {
                throw new AccessDeniedException("只有群主或管理员可以查看申请列表");
            }

            return groupApplicationMapper.findAllApplicationsByGroupId(groupId);
        } catch (AccessDeniedException | WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取所有申请列表失败: groupId={}, userId={}", groupId, userId, e);
            throw new WeebException("获取所有申请列表失败: " + e.getMessage());
        }
    }
}
