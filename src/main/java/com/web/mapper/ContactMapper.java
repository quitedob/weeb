package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.Contact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 联系人（好友）关系 Mapper 接口
 * 简化注释：联系人Mapper
 */
@Mapper
public interface ContactMapper extends BaseMapper<Contact> {

    /**
     * 检查是否存在联系人关系
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否存在联系人关系
     */
    boolean isContactExists(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 创建联系人申请记录
     * @param fromUserId 申请人ID
     * @param toUserId 被申请人ID
     * @param remarks 申请备注
     * @return 受影响行数
     */
    int createContactApply(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId, @Param("remarks") String remarks);

    /**
     * 检查联系人记录是否属于指定用户
     * @param contactId 联系人记录ID
     * @param userId 用户ID
     * @return 是否属于该用户
     */
    boolean isContactBelongsToUser(@Param("contactId") Long contactId, @Param("userId") Long userId);

    /**
     * 更新联系人状态
     * @param contactId 联系人记录ID
     * @param status 新的状态代码
     * @return 受影响行数
     */
    int updateContactStatus(@Param("contactId") Long contactId, @Param("status") int status);

    /**
     * 根据用户和状态查询联系人列表
     * @param userId 用户ID
     * @param status 联系人状态代码
     * @return 联系人列表
     */
    List<com.web.dto.UserDto> selectContactsByUserAndStatus(@Param("userId") Long userId, @Param("status") int status);

    /**
     * 根据用户和状态查询联系人ID列表
     * @param userId 用户ID
     * @param status 联系人状态代码
     * @return 联系人ID列表
     */
    List<Long> selectContactUserIdsByUserAndStatus(@Param("userId") Long userId, @Param("status") int status);

    /**
     * 查询发送给指定用户的待处理好友申请列表
     * @param userId 当前用户ID (作为接收者)
     * @return 发起好友申请的用户列表
     */
    List<com.web.dto.ContactRequestDto> selectPendingContactsReceivedByUser(@Param("userId") Long userId);

    /**
     * 查找过期的PENDING状态请求
     * @param expireTime 过期时间点
     * @return 过期的联系人请求列表
     */
    List<Contact> findExpiredPendingRequests(@Param("expireTime") java.time.LocalDateTime expireTime);

    /**
     * 删除旧的已拒绝请求
     * @param cleanTime 清理时间点（删除此时间之前的记录）
     * @return 删除的记录数
     */
    int deleteOldRejectedRequests(@Param("cleanTime") java.time.LocalDateTime cleanTime);

    /**
     * 统计指定状态的联系人数量
     * @param status 联系人状态代码
     * @return 数量
     */
    int countByStatus(@Param("status") int status);
}
