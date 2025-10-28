package com.web.service.Impl;

import com.web.constant.ContactStatus;
import com.web.dto.UserDto;
import com.web.exception.WeebException;
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

    @Autowired
    private com.web.service.UserService userService;

    @Override
    public void apply(ContactApplyVo applyVo, Long fromUserId) {
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
    }

    @Override
    public void applyByUsername(String username, String message, Long fromUserId) {
        // 根据用户名查找用户
        com.web.model.User targetUser = userService.findByUsername(username);
        if (targetUser == null) {
            throw new WeebException("用户不存在");
        }
        
        // 不能添加自己为好友
        if (targetUser.getId().equals(fromUserId)) {
            throw new WeebException("不能添加自己为好友");
        }
        
        // 检查是否已经存在联系人关系
        boolean exists = contactMapper.isContactExists(fromUserId, targetUser.getId());
        if (exists) {
            throw new WeebException("联系人关系已存在或申请已发送");
        }
        
        // 创建联系人申请记录
        int result = contactMapper.createContactApply(fromUserId, targetUser.getId(), message);
        if (result <= 0) {
            throw new WeebException("申请添加好友失败");
        }
    }

    @Override
    public void accept(Long contactId, Long toUserId) {
        // 检查联系人记录是否属于当前用户
        boolean belongsToUser = contactMapper.isContactBelongsToUser(contactId, toUserId);
        if (!belongsToUser) {
            throw new WeebException("无权限操作此联系人记录");
        }
        
        // 更新联系人状态为已接受
        int result = contactMapper.updateContactStatus(contactId, ContactStatus.ACCEPTED.getCode());
        if (result <= 0) {
            throw new WeebException("接受好友申请失败");
        }
    }

    @Override
    public void declineOrBlock(Long contactId, Long currentUserId, ContactStatus newStatus) {
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
        // 根据todo.txt的要求，对于待处理的好友申请，需要查询发送给当前用户的请求
        // 即 friend_id = currentUserId 且 status = PENDING
        if (status == ContactStatus.PENDING) {
            return contactMapper.selectPendingContactsReceivedByUser(userId);
        } else {
            // 对于其他状态（如已接受的好友），查询用户自己的联系人列表
            return contactMapper.selectContactsByUserAndStatus(userId, status.getCode());
        }
    }

    @Override
    public List<Long> getContactUserIds(Long userId, ContactStatus status) {
        return contactMapper.selectContactUserIdsByUserAndStatus(userId, status.getCode());
    }

    // ==================== 联系人分组管理实现 ====================

    @Autowired
    private com.web.mapper.ContactGroupMapper contactGroupMapper;

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
                        dto.setName(user.getUsername());
                        dto.setAvatar(user.getAvatar());
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
}