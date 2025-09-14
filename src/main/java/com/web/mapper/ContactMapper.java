package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.Contact;
import org.apache.ibatis.annotations.Mapper;
// No custom methods defined in this initial step.
// ServiceImpl will use QueryWrapper or custom methods can be added here later if needed.

/**
 * 联系人（好友）关系 Mapper 接口
 * 简化注释：联系人Mapper
 */
@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
    // Example of potential custom methods (can be added later):
    /*
    default Contact findExistingContact(Long userId, Long friendId) {
        return selectOne(new QueryWrapper<Contact>()
            .and(wrapper -> wrapper.eq("user_id", userId).eq("friend_id", friendId))
            .or(wrapper -> wrapper.eq("user_id", friendId).eq("friend_id", userId))
        );
    }

    default Contact findPendingContactByIdAndReceiver(Long contactId, Long receiverId) {
        return selectOne(new QueryWrapper<Contact>()
            .eq("id", contactId)
            .eq("friend_id", receiverId) // The one who needs to accept
            .eq("status", com.web.constant.ContactStatus.PENDING.getCode())
        );
    }
    */
}
