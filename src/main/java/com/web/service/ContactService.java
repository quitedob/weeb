package com.web.service;

import com.web.constant.ContactStatus; // For getContacts and getContactUserIds status param
import com.web.dto.UserDto;
import com.web.vo.contact.ContactApplyVo;
import java.util.List;

/**
 * 联系人服务接口
 * 简化注释：联系人服务
 */
public interface ContactService {

    /**
     * 申请添加好友
     * @param applyVo 申请信息
     * @param fromUserId 申请人ID (Long)
     */
    void apply(ContactApplyVo applyVo, Long fromUserId);

    /**
     * 同意好友申请
     * @param contactId 申请记录的ID (ID of the Contact entity)
     * @param toUserId 被申请人ID（当前用户, Long）
     */
    void accept(Long contactId, Long toUserId);

    /**
     * 拒绝或拉黑好友申请/现有好友
     * @param contactId 关系ID (ID of the Contact entity)
     * @param currentUserId 当前用户ID (Long)
     * @param newStatus The new status (DECLINED or BLOCKED)
     */
    void declineOrBlock(Long contactId, Long currentUserId, ContactStatus newStatus);

    /**
     * 获取联系人列表（如好友、待处理请求等）
     * @param userId 当前用户ID (Long)
     * @param status The status of contacts to retrieve (e.g., ACCEPTED for friends)
     * @return 用户信息列表
     */
    List<UserDto> getContacts(Long userId, ContactStatus status);

    /**
     * 获取用户指定状态的联系人ID列表（为内部服务调用，如WebSocket通知）
     * @param userId 用户ID (Long)
     * @param status The status of contacts whose IDs to retrieve
     * @return 好友ID列表
     */
    List<Long> getContactUserIds(Long userId, ContactStatus status); // Return List<Long>
}
