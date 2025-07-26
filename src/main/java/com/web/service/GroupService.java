package com.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.web.model.Group; // Assuming Group model will be created
import com.web.vo.group.GroupCreateVo;
import com.web.vo.group.GroupInviteVo;
import com.web.vo.group.GroupKickVo;
import java.util.List;

public interface GroupService extends IService<Group> {
    Group createGroup(GroupCreateVo createVo, Long userId); // Return type Group, userId as Long

    void inviteMembers(GroupInviteVo inviteVo, Long userId); // userId as Long

    void kickMember(GroupKickVo kickVo, Long userId); // userId as Long

    void leaveGroup(Long groupId, Long userId); // groupId and userId as Long

    /**
     * 根据用户ID获取该用户加入的所有群组
     * @param userId 用户ID
     * @return 群组列表
     */
    List<Group> getGroupsByUserId(Integer userId);
}
