package com.web.service;

import com.web.model.Group; // Assuming Group model will be created
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import com.web.vo.group.GroupKickVo;

public interface GroupService {
    Group createGroup(GroupCreateVo createVo, Long userId); // Return type Group, userId as Long

    void inviteMembers(GroupInviteVo inviteVo, Long userId); // userId as Long

    void kickMember(GroupKickVo kickVo, Long userId); // userId as Long

    void leaveGroup(Long groupId, Long userId); // groupId and userId as Long
}
