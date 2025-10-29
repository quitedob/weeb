package com.web.service.Impl;

import com.web.constant.ContactStatus;
import com.web.dto.UserDto;
import com.web.dto.ContactDto;
import com.web.exception.WeebException;
import com.web.mapper.ContactMapper;
import com.web.service.ContactService;
import com.web.vo.contact.ContactApplyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 联系人服务实现类
 * 处理好友申请、接受、拒绝、拉黑等业务逻辑
 */
@Slf4j
@Service
@Transactional
public class ContactServiceImpl implements ContactService {

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private com.web.service.UserService userService;

    @Autowired
    private com.web.service.NotificationService notificationService;

    @Override
    public void apply(ContactApplyVo applyVo, Long fromUserId) {
        // 输入验证
        if (applyVo == null) {
            throw new WeebException("申请信息不能为空");
        }
        if (fromUserId == null || fromUserId <= 0) {
            throw new WeebException("申请人ID必须为正数");
        }
        if (applyVo.getFriendId() == null || applyVo.getFriendId() <= 0) {
            throw new WeebException("好友ID必须为正数");
        }
        if (fromUserId.equals(applyVo.getFriendId())) {
            throw new WeebException("不能添加自己为好友");
        }
        
        // 检查是否已经存在联系人关系
        boolean exists = contactMapper.isContactExists(fromUserId, applyVo.getFriendId());
        if (exists) {
            throw new WeebException("联系人关系已存在");
        }
        
        // 创建联系人申请记录
        int result = contactMapper.createContactApply(fromUserId, applyVo.getFriendId(), applyVo.getRemarks());
        if (result <= 0) {
            throw new WeebException("申请添加好友失败");
        }
        
        // 发送好友申请通知
        try {
            notificationService.createAndPublishNotification(
                applyVo.getFriendId(),   // 接收者：被申请的用户
                fromUserId,              // 操作者：发起申请的用户
                "FRIEND_REQUEST",        // 通知类型
                "CONTACT",               // 实体类型
                null                     // 实体ID（好友申请没有特定ID）
            );
            log.info("好友申请通知已发送 - 接收者ID: {}", applyVo.getFriendId());
        } catch (Exception e) {
            log.error("发送好友申请通知失败", e);
            // 不抛出异常，通知失败不应影响好友申请的创建
        }
    }

    @Override
    public void applyByUsername(String username, String message, Long fromUserId) {
        // 输入验证
        if (username == null || username.trim().isEmpty()) {
            throw new WeebException("用户名不能为空");
        }
        if (fromUserId == null || fromUserId <= 0) {
            throw new WeebException("申请人ID必须为正数");
        }
        if (message != null && message.length() > 200) {
            throw new WeebException("申请消息不能超过200个字符");
        }
        
        // 根据用户名查找用户
        com.web.model.User targetUser = userService.findByUsername(username.trim());
        if (targetUser == null) {
            throw new WeebException("用户不存在");
        }
        
        // 不能添加自己为好友
        if (targetUser.getId().equals(fromUserId)) {
            throw new WeebException("不能添加自己为好友");
        }
        
        // 查询所有相关的联系人记录
        java.util.List<com.web.model.Contact> existingContacts = contactMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.web.model.Contact>()
                .and(wrapper -> wrapper
                    .and(w -> w.eq("user_id", fromUserId).eq("friend_id", targetUser.getId()))
                    .or(w -> w.eq("user_id", targetUser.getId()).eq("friend_id", fromUserId))
                )
        );
        
        log.info("好友申请检查 - 申请人ID: {}, 目标用户: {} (ID: {}), 找到 {} 条记录", 
                 fromUserId, username, targetUser.getId(), existingContacts.size());
        
        // 打印每条记录的详细信息
        for (com.web.model.Contact contact : existingContacts) {
            log.info("  记录详情 - ID: {}, user_id: {}, friend_id: {}, status: {} ({})", 
                     contact.getId(), contact.getUserId(), contact.getFriendId(), 
                     contact.getStatus(), getStatusName(contact.getStatus()));
        }
        
        // 检查是否有ACCEPTED状态的关系
        boolean hasAccepted = existingContacts.stream()
            .anyMatch(c -> c.getStatus() == ContactStatus.ACCEPTED.getCode());
        
        if (hasAccepted) {
            throw new WeebException("你们已经是好友了");
        }
        
        // 检查是否有PENDING状态的关系
        java.util.List<com.web.model.Contact> pendingContacts = existingContacts.stream()
            .filter(c -> c.getStatus() == ContactStatus.PENDING.getCode())
            .collect(java.util.stream.Collectors.toList());
        
        if (!pendingContacts.isEmpty()) {
            // 检查是否是当前用户发起的PENDING请求
            boolean hasSelfPending = pendingContacts.stream()
                .anyMatch(c -> c.getUserId().equals(fromUserId));
            
            if (hasSelfPending) {
                throw new WeebException("你已经发送过好友申请，请等待对方处理");
            } else {
                throw new WeebException("对方已向你发送好友申请，请在好友申请列表中处理");
            }
        }
        
        // 删除所有REJECTED和BLOCKED状态的旧记录，允许重新申请
        existingContacts.stream()
            .filter(c -> c.getStatus() == ContactStatus.REJECTED.getCode() 
                      || c.getStatus() == ContactStatus.BLOCKED.getCode())
            .forEach(c -> {
                contactMapper.deleteById(c.getId());
                log.info("删除旧的联系人记录 - ID: {}, 状态: {}", c.getId(), c.getStatus());
            });
        
        // 创建联系人申请记录
        int result = contactMapper.createContactApply(fromUserId, targetUser.getId(), message);
        if (result <= 0) {
            throw new WeebException("申请添加好友失败");
        }
        
        log.info("好友申请创建成功 - 申请人ID: {}, 目标用户ID: {}", fromUserId, targetUser.getId());
        
        // 发送好友申请通知
        try {
            notificationService.createAndPublishNotification(
                targetUser.getId(),  // 接收者：被申请的用户
                fromUserId,          // 操作者：发起申请的用户
                "FRIEND_REQUEST",    // 通知类型
                "CONTACT",           // 实体类型
                null                 // 实体ID（好友申请没有特定ID）
            );
            log.info("好友申请通知已发送 - 接收者ID: {}", targetUser.getId());
        } catch (Exception e) {
            log.error("发送好友申请通知失败", e);
            // 不抛出异常，通知失败不应影响好友申请的创建
        }
    }

    @Override
    public void accept(Long contactId, Long toUserId) {
        // 输入验证
        if (contactId == null || contactId <= 0) {
            throw new WeebException("联系人记录ID必须为正数");
        }
        if (toUserId == null || toUserId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        
        // 检查联系人记录是否属于当前用户
        boolean belongsToUser = contactMapper.isContactBelongsToUser(contactId, toUserId);
        if (!belongsToUser) {
            throw new WeebException("无权限操作此联系人记录");
        }
        
        // 获取联系人记录以便发送通知
        com.web.model.Contact contact = contactMapper.selectById(contactId);
        if (contact == null) {
            throw new WeebException("联系人记录不存在");
        }
        
        // 更新联系人状态为已接受
        int result = contactMapper.updateContactStatus(contactId, ContactStatus.ACCEPTED.getCode());
        if (result <= 0) {
            throw new WeebException("接受好友申请失败");
        }
        
        // 发送好友申请被接受的通知
        try {
            // 通知申请人：他的好友申请被接受了
            Long applicantId = contact.getUserId(); // 申请人ID
            notificationService.createAndPublishNotification(
                applicantId,              // 接收者：申请人
                toUserId,                 // 操作者：接受申请的人
                "FRIEND_ACCEPTED",        // 通知类型
                "CONTACT",                // 实体类型
                contactId                 // 实体ID
            );
            log.info("好友申请接受通知已发送 - 接收者ID: {}", applicantId);
        } catch (Exception e) {
            log.error("发送好友申请接受通知失败", e);
        }
    }

    @Override
    public void declineOrBlock(Long contactId, Long currentUserId, ContactStatus newStatus) {
        // 输入验证
        if (contactId == null || contactId <= 0) {
            throw new WeebException("联系人记录ID必须为正数");
        }
        if (currentUserId == null || currentUserId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (newStatus == null) {
            throw new WeebException("新状态不能为空");
        }
        
        // 检查联系人记录是否属于当前用户
        boolean belongsToUser = contactMapper.isContactBelongsToUser(contactId, currentUserId);
        if (!belongsToUser) {
            throw new WeebException("无权限操作此联系人记录");
        }
        
        // 检查新状态是否有效
        if (newStatus != ContactStatus.REJECTED && newStatus != ContactStatus.BLOCKED) {
            throw new WeebException("无效的状态操作");
        }
        
        // 更新联系人状态
        int result = contactMapper.updateContactStatus(contactId, newStatus.getCode());
        if (result <= 0) {
            String action = newStatus == ContactStatus.REJECTED ? "拒绝" : "拉黑";
            throw new WeebException(action + "操作失败");
        }
    }

    @Override
    public List<UserDto> getContacts(Long userId, ContactStatus status) {
        // 输入验证
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (status == null) {
            throw new WeebException("联系人状态不能为空");
        }
        
        // 根据todo.txt的要求，对于待处理的好友申请，需要查询发送给当前用户的请求
        // 即 friend_id = currentUserId 且 status = PENDING
        if (status == ContactStatus.PENDING) {
            // 获取待处理的好友申请详情
            List<com.web.dto.ContactRequestDto> pendingRequests = contactMapper.selectPendingContactsReceivedByUser(userId);
            // 转换为UserDto格式
            return pendingRequests.stream()
                .map(request -> {
                    UserDto userDto = new UserDto();
                    userDto.setId(request.getId()); // 申请人ID
                    userDto.setName(request.getNickname() != null ? request.getNickname() : request.getUsername()); // 优先使用昵称，否则使用用户名
                    userDto.setAvatar(request.getAvatar());
                    return userDto;
                })
                .collect(java.util.stream.Collectors.toList());
        } else {
            // 对于其他状态（如已接受的好友），查询用户自己的联系人列表
            return contactMapper.selectContactsByUserAndStatus(userId, status.getCode());
        }
    }

    @Override
    public List<Long> getContactUserIds(Long userId, ContactStatus status) {
        // 输入验证
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (status == null) {
            throw new WeebException("联系人状态不能为空");
        }
        
        return contactMapper.selectContactUserIdsByUserAndStatus(userId, status.getCode());
    }

    // ==================== 联系人分组管理实现 ====================

    @Autowired
    private com.web.mapper.ContactGroupMapper contactGroupMapper;

    /**
     * 获取状态名称（用于日志）
     */
    private String getStatusName(Integer status) {
        if (status == null) return "NULL";
        return switch (status) {
            case 0 -> "PENDING";
            case 1 -> "ACCEPTED";
            case 2 -> "REJECTED";
            case 3 -> "BLOCKED";
            default -> "UNKNOWN(" + status + ")";
        };
    }

    @Override
    @Transactional
    public Long createContactGroup(Long userId, String groupName, Integer groupOrder) {
        try {
            // 验证分组名称
            if (groupName == null || groupName.trim().isEmpty()) {
                throw new WeebException("分组名称不能为空");
            }

            // 检查是否已存在同名分组
            com.web.model.ContactGroup existingGroup = contactGroupMapper.findByUserIdAndName(userId, groupName);
            if (existingGroup != null) {
                throw new WeebException("分组名称已存在");
            }

            // 创建分组
            com.web.model.ContactGroup group = new com.web.model.ContactGroup();
            group.setUserId(userId);
            group.setGroupName(groupName);
            group.setGroupOrder(groupOrder != null ? groupOrder : 0);
            group.setIsDefault(false);
            
            contactGroupMapper.insert(group);
            return group.getId();

        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("创建分组失败: " + e.getMessage());
        }
    }

    @Override
    public List<com.web.model.ContactGroup> getUserContactGroups(Long userId) {
        try {
            return contactGroupMapper.findByUserId(userId);
        } catch (Exception e) {
            throw new WeebException("获取分组列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean updateContactGroupName(Long groupId, Long userId, String newName) {
        try {
            if (newName == null || newName.trim().isEmpty()) {
                throw new WeebException("分组名称不能为空");
            }

            // 获取分组并验证所有权
            com.web.model.ContactGroup group = contactGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new WeebException("分组不存在或无权限");
            }

            // 不能修改默认分组名称
            if (group.getIsDefault()) {
                throw new WeebException("不能修改默认分组名称");
            }

            // 检查新名称是否已存在
            com.web.model.ContactGroup existingGroup = contactGroupMapper.findByUserIdAndName(userId, newName);
            if (existingGroup != null && !existingGroup.getId().equals(groupId)) {
                throw new WeebException("分组名称已存在");
            }

            group.setGroupName(newName);
            return contactGroupMapper.updateById(group) > 0;

        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("更新分组名称失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean updateContactGroupOrder(Long groupId, Long userId, Integer newOrder) {
        try {
            // 获取分组并验证所有权
            com.web.model.ContactGroup group = contactGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new WeebException("分组不存在或无权限");
            }

            return contactGroupMapper.updateGroupOrder(groupId, newOrder) > 0;

        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("更新分组排序失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean deleteContactGroup(Long groupId, Long userId) {
        try {
            // 获取分组并验证所有权
            com.web.model.ContactGroup group = contactGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new WeebException("分组不存在或无权限");
            }

            // 不能删除默认分组
            if (group.getIsDefault()) {
                throw new WeebException("不能删除默认分组");
            }

            // 将该分组下的联系人移到默认分组
            com.web.model.ContactGroup defaultGroup = getDefaultContactGroup(userId);
            if (defaultGroup != null) {
                contactMapper.update(null, 
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<com.web.model.Contact>()
                        .eq("user_id", userId)
                        .eq("group_id", groupId)
                        .set("group_id", defaultGroup.getId())
                );
            }

            return contactGroupMapper.deleteGroup(groupId, userId) > 0;

        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("删除分组失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean addContactToGroup(Long contactId, Long groupId, Long userId) {
        try {
            // 验证联系人所有权
            com.web.model.Contact contact = contactMapper.selectById(contactId);
            if (contact == null || !contact.getUserId().equals(userId)) {
                throw new WeebException("联系人不存在或无权限");
            }

            // 验证分组所有权
            com.web.model.ContactGroup group = contactGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new WeebException("分组不存在或无权限");
            }

            // 更新联系人分组
            contact.setGroupId(groupId);
            return contactMapper.updateById(contact) > 0;

        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("添加联系人到分组失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean removeContactFromGroup(Long contactId, Long userId) {
        try {
            // 验证联系人所有权
            com.web.model.Contact contact = contactMapper.selectById(contactId);
            if (contact == null || !contact.getUserId().equals(userId)) {
                throw new WeebException("联系人不存在或无权限");
            }

            // 移到默认分组
            com.web.model.ContactGroup defaultGroup = getDefaultContactGroup(userId);
            if (defaultGroup != null) {
                contact.setGroupId(defaultGroup.getId());
            } else {
                contact.setGroupId(null);
            }

            return contactMapper.updateById(contact) > 0;

        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("移除联系人分组失败: " + e.getMessage());
        }
    }

    @Override
    public List<UserDto> getContactsByGroup(Long groupId, Long userId) {
        try {
            // 验证分组所有权
            com.web.model.ContactGroup group = contactGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new WeebException("分组不存在或无权限");
            }

            // 查询该分组下的联系人
            List<com.web.model.Contact> contacts = contactMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.web.model.Contact>()
                    .eq("user_id", userId)
                    .eq("group_id", groupId)
                    .eq("status", ContactStatus.ACCEPTED.getCode())
            );

            // 转换为UserDto列表
            return contacts.stream()
                .map(contact -> {
                    com.web.model.User user = userService.getUserBasicInfo(contact.getFriendId());
                    if (user != null) {
                        UserDto dto = new UserDto();
                        dto.setId(user.getId());
                        dto.setUsername(user.getUsername());
                        dto.setNickname(user.getNickname());
                        dto.setName(user.getUsername()); // 向后兼容
                        dto.setEmail(user.getUserEmail());
                        dto.setAvatar(user.getAvatar());
                        dto.setBio(user.getBio());
                        dto.setType(user.getType());
                        dto.setBadge(user.getBadge());
                        dto.setOnlineStatus(user.getOnlineStatus());
                        dto.setIpOwnership(user.getIpOwnership());
                        dto.setCreatedAt(user.getCreatedAt() != null ?
                            java.time.LocalDateTime.ofInstant(user.getCreatedAt().toInstant(),
                                java.time.ZoneId.systemDefault()) : null);
                        dto.setUpdatedAt(user.getUpdatedAt() != null ?
                            java.time.LocalDateTime.ofInstant(user.getUpdatedAt().toInstant(),
                                java.time.ZoneId.systemDefault()) : null);
                        return dto;
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(java.util.stream.Collectors.toList());

        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            throw new WeebException("获取分组联系人失败: " + e.getMessage());
        }
    }

    @Override
    public com.web.model.ContactGroup getDefaultContactGroup(Long userId) {
        try {
            com.web.model.ContactGroup defaultGroup = contactGroupMapper.findDefaultByUserId(userId);
            
            // 如果没有默认分组，创建一个
            if (defaultGroup == null) {
                contactGroupMapper.createDefaultGroup(userId, "我的好友");
                defaultGroup = contactGroupMapper.findDefaultByUserId(userId);
            }
            
            return defaultGroup;

        } catch (Exception e) {
            throw new WeebException("获取默认分组失败: " + e.getMessage());
        }
    }

    @Override
    public List<com.web.dto.ContactRequestDto> getPendingRequests(Long userId) {
        try {
            return contactMapper.selectPendingContactsReceivedByUser(userId);
        } catch (Exception e) {
            log.error("获取待处理好友申请失败", e);
            throw new WeebException("获取待处理好友申请失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactDto> getContactsWithDetails(Long userId, ContactStatus status) {
        try {
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }
            
            if (status == null) {
                throw new WeebException("联系人状态不能为空");
            }
            
            log.debug("获取用户联系人详细信息: userId={}, status={}", userId, status);
            
            // 使用新的Mapper方法获取联系人详细信息
            List<ContactDto> contacts = contactMapper.selectContactsWithDetails(userId, status);
            
            log.debug("获取到 {} 个联系人", contacts != null ? contacts.size() : 0);
            
            return contacts != null ? contacts : new java.util.ArrayList<>();
            
        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取联系人详细信息失败: userId={}, status={}", userId, status, e);
            throw new WeebException("获取联系人详细信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean deleteContact(Long contactId, Long userId) {
        try {
            // 输入验证
            if (contactId == null || contactId <= 0) {
                throw new WeebException("联系人记录ID必须为正数");
            }
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }
            
            // 检查联系人记录是否存在且属于当前用户
            com.web.model.Contact contact = contactMapper.selectById(contactId);
            if (contact == null) {
                throw new WeebException("联系人记录不存在");
            }
            
            // 检查权限：只能删除自己的联系人记录或者是双向关系中的任一方
            boolean hasPermission = contact.getUserId().equals(userId) || contact.getFriendId().equals(userId);
            if (!hasPermission) {
                throw new WeebException("无权限删除此联系人");
            }
            
            // 删除联系人记录
            int result = contactMapper.deleteById(contactId);
            if (result > 0) {
                log.info("联系人删除成功: contactId={}, userId={}", contactId, userId);
                return true;
            } else {
                log.warn("联系人删除失败: contactId={}, userId={}", contactId, userId);
                return false;
            }
            
        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除联系人失败: contactId={}, userId={}", contactId, userId, e);
            throw new WeebException("删除联系人失败: " + e.getMessage());
        }
    }
}