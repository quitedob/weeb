package com.web.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.web.constant.ContactStatus;
import com.web.dto.UserDto;
import com.web.exception.WeebException; // Assuming custom exception class
import com.web.mapper.ContactMapper;
import com.web.mapper.UserMapper; // Assuming UserMapper exists
import com.web.model.Contact;
import com.web.model.User; // For fetching user details
import com.web.service.ContactService;
// import com.web.service.WebSocketService; // For notifications
import com.web.vo.contact.ContactApplyVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils; // For copying properties to UserDto
import org.springframework.beans.factory.annotation.Autowired; // Using Autowired
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl implements ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private UserMapper userMapper; // Assuming UserMapper exists and works with User model

    // 通过Redis广播实时通知，避免形成循环依赖
    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate; // Redis模板

    // 移除未使用依赖，常量直接使用 RedisConfig.USER_MESSAGE_TOPIC

    @Override
    @Transactional
    public void apply(ContactApplyVo applyVo, Long fromUserId) {
        log.info("User {} applying to be friends with {}", fromUserId, applyVo.getFriendId());

        if (fromUserId.equals(applyVo.getFriendId())) {
            throw new WeebException("Cannot send a friend request to yourself.");
        }

        // Validate friendId (user exists)
        User friend = userMapper.selectById(applyVo.getFriendId());
        if (friend == null) {
            throw new WeebException("Target user does not exist.");
        }

        // Check for existing relationship (pending, accepted, or blocked by target)
        // This query checks both directions for an active relationship.
        QueryWrapper<Contact> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
            .eq("user_id", fromUserId).eq("friend_id", applyVo.getFriendId()))
            .or(wrapper -> wrapper
            .eq("user_id", applyVo.getFriendId()).eq("friend_id", fromUserId));

        Contact existingContact = contactMapper.selectOne(queryWrapper);

        if (existingContact != null) {
            if (ContactStatus.ACCEPTED.getCode() == existingContact.getStatus()) {
                throw new WeebException("You are already friends.");
            }
            if (ContactStatus.PENDING.getCode() == existingContact.getStatus()) {
                // If current user is the original sender of pending request
                if(existingContact.getUserId().equals(fromUserId)){
                   throw new WeebException("Friend request already sent and is pending.");
                } else { // Pending request exists from target user to current user
                   throw new WeebException("This user has already sent you a friend request. Please check your pending requests.");
                }
            }
            if (ContactStatus.BLOCKED.getCode() == existingContact.getStatus()) {
                // If the current user was blocked by the friend, or vice-versa
                if (existingContact.getFriendId().equals(fromUserId) && existingContact.getUserId().equals(applyVo.getFriendId())) { // Target blocked current user
                    throw new WeebException("Cannot send request. You are blocked by this user.");
                }
                // If current user blocked target, they should unblock first.
                // Or, allow sending request which might implicitly unblock, depending on product decision.
                // For now, assume if any block exists, prevent new request.
                throw new WeebException("A blocked relationship exists. Please resolve it first.");
            }
            // If it's DECLINED, allow a new request by deleting old one or creating new.
            // For simplicity, let's assume a new request can be made if previous was declined.
            // Or, we could update the existing declined one to pending.
            // Here, we'll just create a new one if no active (pending, accepted, blocked) found.
        }

        // If existing contact is declined by one of the parties, allow new request by creating a new record or updating.
        // For this implementation, we will create a new contact request if no PENDING, ACCEPTED or BLOCKED one exists.
        // A more robust solution might involve checking the 'declined' one and updating it.
        // For now, we proceed to create if the above checks pass for active relationships.

        Contact newContact = new Contact(fromUserId, applyVo.getFriendId(), applyVo.getRemarks());
        // status is already PENDING by constructor
        contactMapper.insert(newContact);

        log.info("Friend request from {} to {} created with id {}", fromUserId, applyVo.getFriendId(), newContact.getId());
        // 通过Redis发布通知消息（由 RedisSubscriber 转发到目标用户）
        try {
            var payload = java.util.Map.of(
                    "event", "contact_apply",
                    "fromUserId", fromUserId,
                    "contactId", newContact.getId()
            );
            com.web.service.WebSocketService.WsContent ws = new com.web.service.WebSocketService.WsContent();
            ws.setType(com.web.constant.WsContentType.NOTIFICATION.getCode());
            ws.setContent(payload);
            String wsJson = cn.hutool.json.JSONUtil.toJsonStr(ws);
            com.web.dto.RedisBroadcastMsg msg = com.web.dto.RedisBroadcastMsg.builder()
                    .targetUserId(applyVo.getFriendId())
                    .messageBody(wsJson)
                    .build();
            redisTemplate.convertAndSend(com.web.Config.RedisConfig.USER_MESSAGE_TOPIC, msg);
        } catch (Exception ex) {
            log.warn("Notify contact apply failed: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void accept(Long contactId, Long toUserId) {
        log.info("User {} accepting contact request id {}", toUserId, contactId);

        Contact contactRequest = contactMapper.selectById(contactId);
        if (contactRequest == null) {
            throw new WeebException("Friend request not found.");
        }

        // Validate it's a PENDING request and toUserId is the friendId in the record
        if (!contactRequest.getFriendId().equals(toUserId)) {
            throw new WeebException("This request is not addressed to you.");
        }
        if (ContactStatus.PENDING.getCode() != contactRequest.getStatus()) {
            throw new WeebException("This request is not pending acceptance.");
        }

        contactRequest.setStatus(ContactStatus.ACCEPTED.getCode());
        contactRequest.setUpdateTime(new Date());
        contactMapper.updateById(contactRequest);

        // Optional: Create a reciprocal ACCEPTED record for easier querying from the other side,
        // or ensure queries for friends always check both (userId, friendId) and (friendId, userId)
        // For simplicity, we assume queries will handle bidirectionality.

        log.info("Contact request id {} accepted by user {}", contactId, toUserId);
        // 通过Redis发布通知：好友请求被接受
        try {
            var payload = java.util.Map.of(
                    "event", "contact_accepted",
                    "byUserId", toUserId,
                    "contactId", contactId
            );
            com.web.service.WebSocketService.WsContent ws = new com.web.service.WebSocketService.WsContent();
            ws.setType(com.web.constant.WsContentType.NOTIFICATION.getCode());
            ws.setContent(payload);
            String wsJson = cn.hutool.json.JSONUtil.toJsonStr(ws);
            com.web.dto.RedisBroadcastMsg msg = com.web.dto.RedisBroadcastMsg.builder()
                    .targetUserId(contactRequest.getUserId())
                    .messageBody(wsJson)
                    .build();
            redisTemplate.convertAndSend(com.web.Config.RedisConfig.USER_MESSAGE_TOPIC, msg);
        } catch (Exception ex) {
            log.warn("Notify contact accepted failed: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void declineOrBlock(Long contactId, Long currentUserId, ContactStatus newStatus) {
        if (newStatus != ContactStatus.REJECTED && newStatus != ContactStatus.BLOCKED) {
            throw new WeebException("Invalid status for this operation. Only REJECTED or BLOCKED allowed.");
        }
        log.info("User {} setting contact record {} to status {}", currentUserId, contactId, newStatus);

        Contact contact = contactMapper.selectById(contactId);
        if (contact == null) {
            throw new WeebException("Contact record not found.");
        }

        // Validate currentUserId is part of the contact record
        // For PENDING, currentUserId should be contact.getFriendId() (the receiver)
        // For ACCEPTED, currentUserId can be either contact.getUserId() or contact.getFriendId()
        boolean isReceiverOfPending = ContactStatus.PENDING.getCode() == contact.getStatus() && contact.getFriendId().equals(currentUserId);
        boolean isPartOfAccepted = ContactStatus.ACCEPTED.getCode() == contact.getStatus() && (contact.getUserId().equals(currentUserId) || contact.getFriendId().equals(currentUserId));

        if (!isReceiverOfPending && !isPartOfAccepted) {
            // If trying to decline/block a request not meant for them, or a relationship they are not part of.
            // Or if trying to decline an already accepted request from their side (they should use block or remove friend)
            throw new WeebException("You do not have permission to modify this contact record or action is not applicable.");
        }

        contact.setStatus(newStatus.getCode());
        contact.setUpdateTime(new Date());
        contactMapper.updateById(contact);

        log.info("Contact record {} status updated to {} by user {}", contactId, newStatus, currentUserId);
        // 通过Redis通知对方（如被拉黑、被拒绝）
        try {
            var payload = java.util.Map.of(
                    "event", "contact_status_changed",
                    "status", newStatus.getCode(),
                    "contactId", contactId
            );
            com.web.service.WebSocketService.WsContent ws = new com.web.service.WebSocketService.WsContent();
            ws.setType(com.web.constant.WsContentType.NOTIFICATION.getCode());
            ws.setContent(payload);
            String wsJson = cn.hutool.json.JSONUtil.toJsonStr(ws);
            Long otherUserId = contact.getUserId().equals(currentUserId) ? contact.getFriendId() : contact.getUserId();
            com.web.dto.RedisBroadcastMsg msg = com.web.dto.RedisBroadcastMsg.builder()
                    .targetUserId(otherUserId)
                    .messageBody(wsJson)
                    .build();
            redisTemplate.convertAndSend(com.web.Config.RedisConfig.USER_MESSAGE_TOPIC, msg);
        } catch (Exception ex) {
            log.warn("Notify contact status change failed: {}", ex.getMessage());
        }
    }

    @Override
    public List<UserDto> getContacts(Long userId, ContactStatus status) {
        log.info("Fetching contacts for user {} with status {}", userId, status);
        QueryWrapper<Contact> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
            .eq("user_id", userId).or().eq("friend_id", userId))
            .eq("status", status.getCode());

        List<Contact> contacts = contactMapper.selectList(queryWrapper);
        if (contacts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> friendIds = new ArrayList<>();
        for (Contact contact : contacts) {
            if (contact.getUserId().equals(userId)) {
                friendIds.add(contact.getFriendId());
            } else {
                friendIds.add(contact.getUserId());
            }
        }

        if (friendIds.isEmpty()) { // Should not happen if contacts is not empty, but as a safeguard
            return Collections.emptyList();
        }

        List<User> users = userMapper.selectBatchIds(friendIds);
        return users.stream().map(user -> {
            UserDto dto = new UserDto();
            BeanUtils.copyProperties(user, dto); // Assumes UserDto has matching field names (id, username, avatar)
                                                // Existing UserDto has 'name' not 'username'. This will need alignment.
                                                // For now, this will copy 'id' and 'avatar' if they match User.java.
                                                // 'name' from User.java will be copied to 'name' in UserDto.java.
            // dto.setOnlineStatus(user.getOnlineStatus()); // When online status is added to User model
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Long> getContactUserIds(Long userId, ContactStatus status) {
        log.info("Fetching contact User IDs for user {} with status {}", userId, status);
        QueryWrapper<Contact> queryWrapper = new QueryWrapper<>();
        // If status is PENDING, we might want to fetch IDs of users who sent requests TO me.
        if (status == ContactStatus.PENDING) {
            queryWrapper.eq("friend_id", userId).eq("status", ContactStatus.PENDING.getCode());
        } else {
        // For other statuses (ACCEPTED, BLOCKED by me, etc.), consider both directions
            queryWrapper.and(wrapper -> wrapper
                .eq("user_id", userId).or().eq("friend_id", userId))
                .eq("status", status.getCode());
        }

        List<Contact> contacts = contactMapper.selectList(queryWrapper);
        if (contacts.isEmpty()) {
            return Collections.emptyList();
        }

        return contacts.stream()
            .map(contact -> contact.getUserId().equals(userId) ? contact.getFriendId() : contact.getUserId())
            .distinct() // Ensure unique IDs
            .collect(Collectors.toList());
    }
}
