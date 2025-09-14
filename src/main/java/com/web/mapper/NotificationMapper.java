package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
    /**
     * 根据接收者ID分页查询通知列表
     * @param recipientId 接收者用户ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 通知列表
     */
    List<Notification> findNotificationsByRecipientId(@Param("recipientId") Long recipientId,
                                                     @Param("offset") int offset,
                                                     @Param("limit") int limit);
    
    /**
     * 统计用户未读通知数量
     * @param recipientId 接收者用户ID
     * @return 未读通知数量
     */
    int countUnreadNotifications(@Param("recipientId") Long recipientId);
    
    /**
     * 将指定用户的所有通知标记为已读
     * @param recipientId 接收者用户ID
     * @return 更新的记录数
     */
    int markAllAsRead(@Param("recipientId") Long recipientId);
    
    /**
     * 将指定通知标记为已读
     * @param notificationId 通知ID
     * @param recipientId 接收者用户ID（用于安全验证）
     * @return 更新的记录数
     */
    int markAsRead(@Param("notificationId") Long notificationId, @Param("recipientId") Long recipientId);
    
    /**
     * 删除指定用户的所有已读通知
     * @param recipientId 接收者用户ID
     * @return 删除的记录数
     */
    int deleteReadNotifications(@Param("recipientId") Long recipientId);
    
    /**
     * 统计用户总通知数量
     * @param recipientId 接收者用户ID
     * @return 总通知数量
     */
    int countTotalNotifications(@Param("recipientId") Long recipientId);
} 