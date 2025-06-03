package com.web.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper; // Added for QueryWrapper usage in leaveGroup
import com.web.constant.ChatListType;
import com.web.constant.GroupRole;
import com.web.exception.WeebException; // Assuming custom exception
import com.web.mapper.ChatListMapper;
import com.web.mapper.GroupMapper;
import com.web.mapper.GroupMemberMapper;
import com.web.model.ChatList;
import com.web.model.Group;
import com.web.model.GroupMember;
// import com.web.model.User; // Not directly used for User details here, ownerId is Long
import com.web.service.GroupService;
// import com.web.service.WebSocketService; // For future notifications
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import com.web.vo.group.GroupKickVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Using Autowired
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils; // For checking if list is empty

import java.util.Date;
import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private ChatListMapper chatListMapper;

    // @Autowired
    // private WebSocketService webSocketService; // For future notifications

    @Override
    @Transactional
    public Group createGroup(GroupCreateVo createVo, Long userId) { // userId is Long
        log.info("User {} attempting to create group with name: {}", userId, createVo.getGroupName());

        // 1. Create Group entity
        Group group = new Group();
        group.setGroupName(createVo.getGroupName());
        group.setOwnerId(userId); // Owner is the creator
        // group.setGroupAvatarUrl(); // Can be set later or if provided in VO
        // createTime is set by Group constructor
        groupMapper.insert(group); // Insert and get the auto-generated group ID

        // 2. Create Group's ChatList entry
        ChatList chatList = new ChatList();
        // chatList.setId(); // Assuming ID is auto-generated or set by DB for ChatList if it's also mapped with auto-increment.
                           // If ChatList.id is String and needs manual generation (e.g. UUID), do it here.
                           // For now, assuming ChatListMapper.insert handles ID generation or it's not strictly required here for this flow.
                           // The user's ChatList.java has String id. It might be an issue if not handled.
                           // Let's assume for now the mapper or DB handles ChatList ID.
        chatList.setType(ChatListType.GROUP.getCode());
        chatList.setGroupId(group.getId());
        chatList.setTargetInfo(group.getGroupName()); // Set group name as target info
        // chatList.setUserId(userId); // Not relevant for group chatlist entry itself in this context
        // chatList.setTargetId(group.getId()); // Or set targetId to groupId if that's the convention
        // lastMessage, unreadCount will be updated by message flow
        chatList.setCreateTime(new Date().toString()); // ChatList createTime is String
        chatList.setUpdateTime(new Date().toString()); // ChatList updateTime is String
        chatListMapper.insert(chatList);
        log.info("Created ChatList entry for group id {}", group.getId());


        // 3. Add creator as OWNER in GroupMember
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole(GroupRole.OWNER.getCode());
        ownerMember.setCreateTime(new Date());
        groupMemberMapper.insert(ownerMember);
        log.info("Added user {} as OWNER of group id {}", userId, group.getId());

        // 4. Add initial members (if any)
        if (!CollectionUtils.isEmpty(createVo.getInitialMemberIds())) {
            for (Long memberId : createVo.getInitialMemberIds()) {
                if (memberId.equals(userId)) continue; // Skip owner if listed again

                // Check if user exists (optional, depends on desired strictness)
                // User invitedUser = userMapper.selectById(memberId);
                // if(invitedUser == null) { log.warn("User with id {} not found, skipping.", memberId); continue; }

                if (groupMemberMapper.isMember(group.getId(), memberId)) {
                    log.warn("User {} is already a member of group {}, skipping initial add.", memberId, group.getId());
                    continue;
                }
                GroupMember member = new GroupMember();
                member.setGroupId(group.getId());
                member.setUserId(memberId);
                member.setRole(GroupRole.MEMBER.getCode());
                member.setCreateTime(new Date());
                groupMemberMapper.insert(member);
                log.info("Added initial member {} to group id {}", memberId, group.getId());
            }
        }
        // TODO: Notify initial members via WebSocket?
        return group;
    }

    @Override
    @Transactional
    public void inviteMembers(GroupInviteVo inviteVo, Long userId) { // userId is Long
        log.info("User {} inviting members to group {}", userId, inviteVo.getGroupId());
        checkAdminPermission(inviteVo.getGroupId(), userId); // Check if inviter has permission

        for (Long memberId : inviteVo.getMemberIds()) {
            // User invitedUser = userMapper.selectById(memberId);
            // if(invitedUser == null) { log.warn("User to invite (id {}) not found, skipping.", memberId); continue; }

            if (groupMemberMapper.isMember(inviteVo.getGroupId(), memberId)) {
                log.warn("User {} is already a member of group {}. Skipping invitation.", memberId, inviteVo.getGroupId());
                continue;
            }
            GroupMember member = new GroupMember();
            member.setGroupId(inviteVo.getGroupId());
            member.setUserId(memberId);
            member.setRole(GroupRole.MEMBER.getCode());
            member.setCreateTime(new Date());
            groupMemberMapper.insert(member);
            log.info("Invited user {} to group {}", memberId, inviteVo.getGroupId());
            // TODO: Notify invited user (memberId) via WebSocket
        }
    }

    @Override
    @Transactional
    public void kickMember(GroupKickVo kickVo, Long userId) { // userId is Long
        log.info("User {} attempting to kick user {} from group {}", userId, kickVo.getKickedUserId(), kickVo.getGroupId());
        checkAdminPermission(kickVo.getGroupId(), userId); // Check if kicker has permission

        GroupMember memberToKick = groupMemberMapper.findByGroupAndUser(kickVo.getGroupId(), kickVo.getKickedUserId());
        if (memberToKick == null) {
            throw new WeebException("User to be kicked is not a member of this group.");
        }

        if (GroupRole.OWNER.getCode().equals(memberToKick.getRole())) {
            throw new WeebException("Group owner cannot be kicked.");
        }

        // Optional: Check if kicker's role is higher than memberToKick's role (e.g. admin cannot kick other admins unless owner)
        // GroupMember kicker = groupMemberMapper.findByGroupAndUser(kickVo.getGroupId(), userId);
        // if (kicker.getRole() <= memberToKick.getRole() && !GroupRole.OWNER.getCode().equals(kicker.getRole())) {
        //    throw new WeebException("You do not have sufficient permission to kick this member.");
        // }


        int deletedRows = groupMemberMapper.deleteById(memberToKick.getId());
        if (deletedRows > 0) {
            log.info("User {} successfully kicked from group {}", kickVo.getKickedUserId(), kickVo.getGroupId());
            // TODO: Notify kicked user and other group members via WebSocket
        } else {
            log.warn("Failed to kick user {} from group {}. Member record not found or delete failed.", kickVo.getKickedUserId(), kickVo.getGroupId());
            // This case should ideally not be reached if findByGroupAndUser found the member.
        }
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, Long userId) { // userId is Long
        log.info("User {} attempting to leave group {}", userId, groupId);
        GroupMember member = groupMemberMapper.findByGroupAndUser(groupId, userId);
        if (member == null) {
            throw new WeebException("You are not a member of this group.");
        }

        if (GroupRole.OWNER.getCode().equals(member.getRole())) {
            // Check if other owners exist or if there are other members to transfer ownership to
            long ownerCount = groupMemberMapper.selectCount(
                new QueryWrapper<GroupMember>()
                    .eq("group_id", groupId)
                    .eq("role", GroupRole.OWNER.getCode())
            );
            if (ownerCount <= 1) { // This user is the sole owner
                long totalMembers = groupMemberMapper.selectCount(
                    new QueryWrapper<GroupMember>().eq("group_id", groupId)
                );
                if (totalMembers > 1) { // There are other members
                    throw new WeebException("You are the sole owner. Please transfer ownership before leaving the group.");
                } else { // Sole owner and sole member, group can be deleted or left empty
                    log.info("Sole owner {} leaving group {}. Group will be empty.", userId, groupId);
                }
            }
        }

        groupMemberMapper.deleteById(member.getId());
        log.info("User {} successfully left group {}", userId, groupId);
        // TODO: Notify other group members via WebSocket
    }

    /**
     * Helper method to check if a user has admin or owner permissions in a group.
     * @param groupId Group ID
     * @param userId User ID
     */
    private void checkAdminPermission(Long groupId, Long userId) { // groupId, userId are Long
        GroupMember operator = groupMemberMapper.findByGroupAndUser(groupId, userId);
        if (operator == null) {
            throw new WeebException("You are not a member of this group.");
        }
        if (operator.getRole() < GroupRole.ADMIN.getCode()) { // OWNER(2), ADMIN(1), MEMBER(0)
            throw new WeebException("You do not have permission to perform this action (Admin or Owner required).");
        }
    }
}
