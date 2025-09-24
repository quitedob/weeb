package com.web.service.impl;

import com.web.constant.ContactStatus;
import com.web.dto.UserDto;
import com.web.mapper.ContactMapper;
import com.web.service.ContactService;
import com.web.vo.contact.ContactApplyVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 联系人服务实现类
 * 处理好友申请、接受、拒绝、拉黑等业务逻辑
 */
@Service
@Transactional
public class ContactServiceImpl implements ContactService {

    @Autowired
    private ContactMapper contactMapper;

    @Override
    public void apply(ContactApplyVo applyVo, Long fromUserId) {
        // 检查是否已经存在联系人关系
        boolean exists = contactMapper.isContactExists(fromUserId, applyVo.getFriendId());
        if (exists) {
            throw new RuntimeException("联系人关系已存在");
        }
        
        // 创建联系人申请记录
        int result = contactMapper.createContactApply(fromUserId, applyVo.getFriendId(), applyVo.getRemarks());
        if (result <= 0) {
            throw new RuntimeException("申请添加好友失败");
        }
    }

    @Override
    public void accept(Long contactId, Long toUserId) {
        // 检查联系人记录是否属于当前用户
        boolean belongsToUser = contactMapper.isContactBelongsToUser(contactId, toUserId);
        if (!belongsToUser) {
            throw new RuntimeException("无权限操作此联系人记录");
        }
        
        // 更新联系人状态为已接受
        int result = contactMapper.updateContactStatus(contactId, ContactStatus.ACCEPTED.getCode());
        if (result <= 0) {
            throw new RuntimeException("接受好友申请失败");
        }
    }

    @Override
    public void declineOrBlock(Long contactId, Long currentUserId, ContactStatus newStatus) {
        // 检查联系人记录是否属于当前用户
        boolean belongsToUser = contactMapper.isContactBelongsToUser(contactId, currentUserId);
        if (!belongsToUser) {
            throw new RuntimeException("无权限操作此联系人记录");
        }
        
        // 检查新状态是否有效
        if (newStatus != ContactStatus.REJECTED && newStatus != ContactStatus.BLOCKED) {
            throw new RuntimeException("无效的状态操作");
        }
        
        // 更新联系人状态
        int result = contactMapper.updateContactStatus(contactId, newStatus.getCode());
        if (result <= 0) {
            String action = newStatus == ContactStatus.REJECTED ? "拒绝" : "拉黑";
            throw new RuntimeException(action + "操作失败");
        }
    }

    @Override
    public List<UserDto> getContacts(Long userId, ContactStatus status) {
        return contactMapper.selectContactsByUserAndStatus(userId, status.getCode());
    }

    @Override
    public List<Long> getContactUserIds(Long userId, ContactStatus status) {
        return contactMapper.selectContactUserIdsByUserAndStatus(userId, status.getCode());
    }
}