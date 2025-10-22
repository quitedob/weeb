package com.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.exception.WeebException;
import com.web.mapper.GroupMapper;
import com.web.mapper.GroupMemberMapper;
import com.web.mapper.UserMapper;
import com.web.model.Group;
import com.web.model.GroupMember;
import com.web.model.User;
import com.web.service.GroupService;
import com.web.util.ValidationUtils;
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import com.web.vo.group.GroupKickVo;
import com.web.vo.group.GroupApplyVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群组服务实现类
 * 处理群组创建、成员管理、群组信息维护等业务逻辑
 */
@Service
@Transactional
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Group createGroup(GroupCreateVo createVo, Long userId) {
        // 创建群组对象
        Group group = new Group();
        group.setGroupName(createVo.getGroupName());
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
        ownerMember.setRole(1); // 1表示群主
        // 注意：根据GroupMember实际字段设置时间
        
        groupMemberMapper.insert(ownerMember);
        
        return group;
    }

    @Override
    public void inviteMembers(GroupInviteVo inviteVo, Long userId) {
        // 检查用户是否有权限邀请（是否为群主或管理员）
        GroupMember inviter = groupMemberMapper.findByGroupAndUser(inviteVo.getGroupId(), userId);
        if (inviter == null || inviter.getRole() < 1) {
            throw new WeebException("无权限邀请成员");
        }
        
        // 邀请用户加入群组
        for (Long inviteeId : inviteVo.getMemberIds()) {
            // 检查用户是否已经是群成员
            if (!groupMemberMapper.isMember(inviteVo.getGroupId(), inviteeId)) {
                GroupMember newMember = new GroupMember();
                newMember.setGroupId(inviteVo.getGroupId());
                newMember.setUserId(inviteeId);
                newMember.setRole(0); // 0表示普通成员
                // 注意：根据GroupMember实际字段设置时间
                
                groupMemberMapper.insert(newMember);
            }
        }
    }

    @Override
    public void kickMember(GroupKickVo kickVo, Long userId) {
        // 检查操作者是否有权限踢人（是否为群主或管理员）
        GroupMember operator = groupMemberMapper.findByGroupAndUser(kickVo.getGroupId(), userId);
        if (operator == null || operator.getRole() < 1) {
            throw new WeebException("无权限踢出成员");
        }
        
        // 检查被踢用户是否为群成员
        GroupMember targetMember = groupMemberMapper.findByGroupAndUser(kickVo.getGroupId(), kickVo.getKickedUserId());
        if (targetMember == null) {
            throw new WeebException("用户不是群成员");
        }
        
        // 不能踢出群主
        if (targetMember.getRole() >= 2) {
            throw new WeebException("不能踢出群主");
        }
        
        // 移除群成员
        groupMemberMapper.deleteById(targetMember.getId());
    }

    @Override
    public void leaveGroup(Long groupId, Long userId) {
        quitGroup(groupId, userId);
    }

    @Override
    public void dissolveGroup(Long groupId, Long userId) {
        // 检查用户是否为群主
        Group group = getById(groupId);
        if (group == null) {
            throw new WeebException("群组不存在");
        }
        
        if (!group.getOwnerId().equals(userId)) {
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
        // 检查用户是否为群成员
        GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, userId);
        if (member == null) {
            throw new WeebException("用户不是群成员");
        }
        
        // 群主不能直接退出，需要先转让群主或解散群组
        if (member.getRole() >= 2) {
            throw new WeebException("群主不能退出群组，请先转让群主或解散群组");
        }
        
        // 移除群成员
        groupMemberMapper.deleteById(member.getId());
    }

    @Override
    public void inviteUser(Long groupId, GroupInviteVo inviteVo, Long userId) {
        inviteVo.setGroupId(groupId);
        inviteMembers(inviteVo, userId);
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
    public void applyToJoinGroup(GroupApplyVo applyVo, Long userId) {
        // 检查群组是否存在
        Group group = getById(applyVo.getGroupId());
        if (group == null) {
            throw new WeebException("群组不存在");
        }
        
        // 检查用户是否已经是群成员
        if (groupMemberMapper.isMember(applyVo.getGroupId(), userId)) {
            throw new WeebException("您已经是该群组的成员");
        }
        
        // 直接加入群组（简化实现，实际项目中可能需要审核流程）
        GroupMember newMember = new GroupMember();
        newMember.setGroupId(applyVo.getGroupId());
        newMember.setUserId(userId);
        newMember.setRole(0); // 0表示普通成员
        
        groupMemberMapper.insert(newMember);
    }
}
