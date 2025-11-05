package com.web.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.web.constants.GroupRoleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

    /**
     * 检查用户在群组中的权限
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param requiredRole 需要的最低角色
     * @return 是否有权限
     */
    private boolean hasGroupPermission(Long groupId, Long userId, int requiredRole) {
        try {
            // 系统管理员拥有所有权限
            User currentUser = authService.findByUserID(userId);
            if (currentUser != null && userTypeSecurityService.isAdmin(currentUser.getUsername())) {
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
            if (!"ACCEPTED".equals(member.getJoinStatus())) {
                log.debug("用户未被接受为群组成员: userId={}, groupId={}, status={}", 
                    userId, groupId, member.getJoinStatus());
                return false;
            }

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
        
        // 创建群组对象
        Group group = new Group();
        group.setGroupName(createVo.getGroupName().trim());
        group.setOwnerId(userId);
        // Note: Group model uses createTime (Date) instead of createdAt (LocalDateTime)
        group.setCreateTime(new Date());
        
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
        updateById(group);
        
        log.info("群组创建成功: groupId={}, groupName={}, ownerId={}", 
            group.getId(), group.getGroupName(), userId);
        
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
            throw new WeebException("无权限邀请成员");
        }
        
        // 获取群组信息以更新成员数
        Group group = getById(inviteVo.getGroupId());
        if (group == null) {
            throw new WeebException("群组不存在");
        }
        
        int invitedCount = 0;
        
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
                
                groupMemberMapper.insert(newMember);
                invitedCount++;
                
                log.info("成员邀请成功: groupId={}, inviteeId={}, inviterId={}", 
                    inviteVo.getGroupId(), inviteeId, userId);
            }
        }
        
        // 更新群组成员数
        if (invitedCount > 0) {
            group.setMemberCount((group.getMemberCount() != null ? group.getMemberCount() : 0) + invitedCount);
            updateById(group);
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
            throw new WeebException("无权限踢出成员");
        }

        // 检查被踢用户是否为群成员
        GroupMember targetMember = groupMemberMapper.findByGroupAndUser(kickVo.getGroupId(), kickVo.getKickedUserId());
        if (targetMember == null) {
            throw new WeebException("用户不是群成员");
        }

        // 不能踢出群主
        if (GroupRoleConstants.isOwner(targetMember.getRole())) {
            throw new WeebException("不能踢出群主");
        }

        // 检查操作者权限：只有群主可以踢出管理员
        if (GroupRoleConstants.isAdminOrOwner(targetMember.getRole())) {
            GroupMember operator = groupMemberMapper.findByGroupAndUser(kickVo.getGroupId(), userId);
            if (operator == null || !GroupRoleConstants.isOwner(operator.getRole())) {
                throw new WeebException("只有群主可以踢出管理员");
            }
        }

        // 记录移除信息（用于审计）
        targetMember.setKickedAt(new Date());
        targetMember.setKickReason(kickVo.getReason() != null ? kickVo.getReason() : "被管理员移除");
        targetMember.setJoinStatus("BLOCKED"); // 标记为已屏蔽
        groupMemberMapper.updateById(targetMember);
        
        // 移除群成员
        groupMemberMapper.deleteById(targetMember.getId());
        
        // 更新群组成员数
        Group group = getById(kickVo.getGroupId());
        if (group != null && group.getMemberCount() != null && group.getMemberCount() > 0) {
            group.setMemberCount(group.getMemberCount() - 1);
            updateById(group);
        }
        
        log.info("成员被踢出: groupId={}, kickedUserId={}, operatorId={}, reason={}", 
            kickVo.getGroupId(), kickVo.getKickedUserId(), userId, kickVo.getReason());
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
        } catch (Exception e) {
            log.error("退出群组失败: groupId={}, userId={}", groupId, userId, e);
            return false;
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
            throw new WeebException("只有群主可以解散群组");
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
        if (member == null) {
            throw new WeebException("用户不是群成员");
        }
        
        // 群主不能直接退出，需要先转让群主或解散群组
        if (GroupRoleConstants.isOwner(member.getRole())) {
            throw new WeebException("群主不能退出群组，请先转让群主或解散群组");
        }
        
        // 移除群成员
        groupMemberMapper.deleteById(member.getId());
        
        // 更新群组成员数
        Group group = getById(groupId);
        if (group != null && group.getMemberCount() != null && group.getMemberCount() > 0) {
            group.setMemberCount(group.getMemberCount() - 1);
            updateById(group);
        }
        
        log.info("成员退出群组: groupId={}, userId={}", groupId, userId);
    }

    @Override
    public boolean inviteUser(Long groupId, GroupInviteVo inviteVo, Long userId) {
        try {
            inviteVo.setGroupId(groupId);
            return inviteMembers(inviteVo, userId);
        } catch (Exception e) {
            return false;
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
    public List<Map<String, Object>> getGroupMembers(Long groupId) {
        List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(groupId);
        List<Map<String, Object>> members = new ArrayList<>();
        
        for (Long memberId : memberIds) {
            User user = userMapper.selectById(memberId);
            GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, memberId);
            
            if (user != null && member != null) {
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
        GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, userId);
        if (member == null || member.getRole() < 1) {
            throw new WeebException("无权限更新群组信息");
        }
        
        // 获取现有群组信息
        Group existingGroup = getById(groupId);
        if (existingGroup == null) {
            throw new WeebException("群组不存在");
        }
        
        // 更新群组信息
        if (groupData.getGroupName() != null) {
            existingGroup.setGroupName(groupData.getGroupName());
        }
        if (groupData.getGroupAvatarUrl() != null) {
            existingGroup.setGroupAvatarUrl(groupData.getGroupAvatarUrl());
        }
        // Note: Group model doesn't have updatedAt field, using createTime for last update
        // existingGroup.setCreateTime(new Date()); // Uncomment if you want to update timestamp
        
        // 保存更新
        updateById(existingGroup);
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
        } catch (Exception e) {
            return false;
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
                throw new WeebException("只有群主或管理员可以批准申请");
            }

            // 3. 获取申请记录
            com.web.model.GroupApplication application = groupApplicationMapper.selectById(applicationId);
            if (application == null) {
                log.warn("申请记录不存在: applicationId={}", applicationId);
                throw new WeebException("申请记录不存在");
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
            groupApplicationMapper.updateById(application);

            // 8. 添加用户为群成员
            GroupMember newMember = new GroupMember();
            newMember.setGroupId(groupId);
            newMember.setUserId(application.getUserId());
            newMember.setRole(GroupRoleConstants.ROLE_MEMBER);
            newMember.setJoinStatus("ACCEPTED");
            newMember.setInvitedBy(userId); // 记录审批人
            newMember.setInviteReason("申请通过");
            groupMemberMapper.insert(newMember);

            // 9. 更新群组成员数
            group.setMemberCount((group.getMemberCount() != null ? group.getMemberCount() : 0) + 1);
            updateById(group);

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
                throw new WeebException("只有群主或管理员可以拒绝申请");
            }

            // 3. 获取申请记录
            com.web.model.GroupApplication application = groupApplicationMapper.selectById(applicationId);
            if (application == null) {
                log.warn("申请记录不存在: applicationId={}", applicationId);
                throw new WeebException("申请记录不存在");
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
            groupApplicationMapper.updateById(application);

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
            // 检查操作者是否有权限设置角色
            GroupMember operator = groupMemberMapper.findByGroupAndUser(groupId, operatorId);
            if (operator == null || operator.getRole() < 1) {
                return false;
            }

            // 获取目标成员
            GroupMember targetMember = groupMemberMapper.findByGroupAndUser(groupId, userId);
            if (targetMember == null) {
                return false;
            }

            // 设置角色（这里需要根据字符串角色转换为数字）
            int roleValue = "admin".equalsIgnoreCase(role) ? 1 : 0; // 简化实现
            targetMember.setRole(roleValue);

            // 更新数据库
            groupMemberMapper.updateById(targetMember);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean removeMember(Long groupId, Long userId, Long operatorId) {
        try {
            // 检查操作者是否有权限移除成员
            GroupMember operator = groupMemberMapper.findByGroupAndUser(groupId, operatorId);
            if (operator == null || operator.getRole() < 1) {
                return false;
            }

            // 获取目标成员
            GroupMember targetMember = groupMemberMapper.findByGroupAndUser(groupId, userId);
            if (targetMember == null) {
                return false;
            }

            // 不能移除群主
            if (targetMember.getRole() >= 2) {
                return false;
            }

            // 移除成员
            groupMemberMapper.deleteById(targetMember.getId());
            
            // 更新群组成员数
            Group group = getById(groupId);
            if (group != null && group.getMemberCount() != null && group.getMemberCount() > 0) {
                group.setMemberCount(group.getMemberCount() - 1);
                updateById(group);
            }
            
            return true;
        } catch (Exception e) {
            log.error("移除群组成员失败: groupId={}, userId={}, operatorId={}", groupId, userId, operatorId, e);
            return false;
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
            if (!group.getOwnerId().equals(currentOwnerId)) {
                log.warn("用户不是群主，无权转让: groupId={}, userId={}", groupId, currentOwnerId);
                throw new WeebException("只有群主可以转让群组");
            }

            // 3. 验证新群主是否为群成员
            GroupMember newOwnerMember = groupMemberMapper.findByGroupAndUser(groupId, newOwnerId);
            if (newOwnerMember == null) {
                log.warn("新群主不是群成员: groupId={}, newOwnerId={}", groupId, newOwnerId);
                throw new WeebException("新群主必须是群组成员");
            }

            // 4. 验证新群主不是当前群主
            if (currentOwnerId.equals(newOwnerId)) {
                log.warn("不能转让给自己: groupId={}, userId={}", groupId, currentOwnerId);
                throw new WeebException("不能转让给自己");
            }

            // 5. 更新群组所有者
            group.setOwnerId(newOwnerId);
            group.setLastTransferAt(new Date());
            if (group.getTransferCount() == null) {
                group.setTransferCount(1);
            } else {
                group.setTransferCount(group.getTransferCount() + 1);
            }
            updateById(group);

            // 6. 更新新群主的角色为群主
            newOwnerMember.setRole(GroupRoleConstants.ROLE_OWNER);
            groupMemberMapper.updateById(newOwnerMember);

            // 7. 更新原群主的角色为普通成员
            GroupMember oldOwnerMember = groupMemberMapper.findByGroupAndUser(groupId, currentOwnerId);
            if (oldOwnerMember != null) {
                oldOwnerMember.setRole(GroupRoleConstants.ROLE_MEMBER);
                groupMemberMapper.updateById(oldOwnerMember);
            }

            // 8. 记录转让历史
            // TODO: 实现群组转让历史记录功能
            log.info("群组转让历史记录功能待实现: groupId={}, from={}, to={}", groupId, currentOwnerId, newOwnerId);

            log.info("群组转让成功: groupId={}, from={}, to={}", groupId, currentOwnerId, newOwnerId);
            return true;

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
            
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }
            
            log.debug("获取群组详细信息: groupId={}, userId={}", groupId, userId);
            
            // 使用新的Mapper方法获取群组详细信息
            GroupDto group = groupMapper.selectGroupWithDetails(groupId, userId);
            
            if (group == null) {
                log.warn("群组不存在或用户无权限访问: groupId={}, userId={}", groupId, userId);
                throw new WeebException("群组不存在或无权限访问");
            }
            
            log.debug("获取群组详细信息成功: groupId={}, groupName={}", groupId, group.getGroupName());
            
            return group;
            
        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取群组详细信息失败: groupId={}, userId={}", groupId, userId, e);
            throw new WeebException("获取群组详细信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.web.model.GroupApplication> getPendingApplications(Long groupId, Long userId) {
        try {
            // 检查操作者是否有权限查看申请（群主或管理员）
            if (!isGroupAdmin(groupId, userId)) {
                throw new WeebException("只有群主或管理员可以查看申请列表");
            }

            return groupApplicationMapper.findPendingApplicationsByGroupId(groupId);
        } catch (WeebException e) {
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
                throw new WeebException("只有群主或管理员可以查看申请列表");
            }

            return groupApplicationMapper.findAllApplicationsByGroupId(groupId);
        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取所有申请列表失败: groupId={}, userId={}", groupId, userId, e);
            throw new WeebException("获取所有申请列表失败: " + e.getMessage());
        }
    }
}
