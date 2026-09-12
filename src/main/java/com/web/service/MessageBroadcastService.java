package com.web.service;

import com.web.model.ChatList;
import com.web.model.Message;
import com.web.model.User;
import com.web.vo.message.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息广播服务
 * 负责将消息转发给接收者，实现实时消息推送
 */
@Slf4j
@Service
public class MessageBroadcastService {

    /** Called only for a committed outbox event. Let dispatch failures reach the retry worker. */
    public void dispatchOutboxEvent(String username, String type, java.util.Map<String, Object> payload) {
        String destination = switch (type) {
            case "MESSAGE" -> "/queue/private";
            case "READ" -> "/queue/read-receipt";
            case "REACTION" -> "/queue/reaction-change";
            default -> throw new IllegalArgumentException("Unsupported message event");
        };
        messagingTemplate.convertAndSendToUser(username, destination, payload);
    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private UserOnlineStatusService onlineStatusService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private com.web.service.ChatUnreadCountService chatUnreadCountService;

    @Autowired(required = false)
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    /**
     * ✅ 群聊消息转发
     * @param message 消息对象
     * @param groupId 群组ID
     */
    public void broadcastMessageToGroup(Message message, Long groupId) {
        try {
            // 1. 获取群组成员列表
            List<Long> memberIds = getGroupMemberIds(groupId);
            
            if (memberIds.isEmpty()) {
                log.warn("群组成员列表为空: groupId={}", groupId);
                return;
            }

            // 2. 获取发送者信息
            User sender = userService.getUserBasicInfo(message.getSenderId());
            String senderName = sender != null ? sender.getUsername() : "Unknown";

            // 3. 构建消息响应对象
            MessageResponse response = new MessageResponse();
            response.setId(message.getId());
            response.setMessageId(message.getId()); // 前端期望messageId字段
            response.setFromId(message.getSenderId());
            response.setFromName(senderName);
            response.setContent(extractContent(message));
            response.setMsgContent(extractContent(message));
            response.setTimestamp(message.getCreatedAt());
            response.setMessageType(message.getMessageType());
            response.setChatId(String.valueOf(message.getChatId()));
            response.setTargetId(message.getChatId()); // 前端期望targetId字段
            response.setRoomId(String.valueOf(groupId));
            response.setIsRecalled(message.getIsRecalled() != null ? message.getIsRecalled() : 0);

            // 4. 批量转发给所有群成员（排除发送者）
            int successCount = 0;
            int failCount = 0;

            for (Long memberId : memberIds) {
                // 跳过发送者自己
                if (memberId.equals(message.getSenderId())) {
                    continue;
                }

                try {
                    // 设置isFromMe标志
                    response.setIsFromMe(false);
                    
                    // 检查成员是否在线
                    chatUnreadCountService.incrementUnreadCount(memberId, message.getChatId(), 1);
                    boolean isOnline = onlineStatusService.isUserOnline(memberId);
                    
                    if (isOnline) {
                        // 在线：立即发送
                        User member = userService.getUserBasicInfo(memberId);
                        if (member != null) {
                            response.setStatus(2); // DELIVERED
                            messagingTemplate.convertAndSendToUser(
                                member.getUsername(),
                                "/queue/private",
                                response
                            );
                            successCount++;
                        }
                    } else {
                        // 离线：标记存储并增加未读计数
                        response.setStatus(1); // SENT
                        storeOfflineMessage(memberId, response);
                        failCount++;
                    }
                } catch (Exception e) {
                    log.error("转发群聊消息给成员失败: memberId={}", memberId, e);
                    failCount++;
                }
            }

            log.info("✅ 群聊消息转发完成: groupId={}, messageId={}, 成功={}, 离线={}",
                groupId, message.getId(), successCount, failCount);

        } catch (Exception e) {
            log.error("❌ 群聊消息转发失败: groupId={}, messageId={}",
                groupId, message.getId(), e);
        }
    }

    /**
     * 获取群组成员ID列表
     * @param groupId 群组ID
     * @return 成员ID列表
     */
    private List<Long> getGroupMemberIds(Long groupId) {
        try {
            // 使用JdbcTemplate查询群成员
            String sql = "SELECT user_id FROM group_member WHERE group_id = ? AND join_status = 'ACCEPTED' AND kicked_at IS NULL";
            return jdbcTemplate.queryForList(sql, Long.class, groupId);
        } catch (Exception e) {
            log.error("获取群组成员列表失败: groupId={}", groupId, e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 转发消息给接收者
     * ✅ 改进：支持在线状态检查和离线消息存储
     * @param message 消息对象
     * @param receiverId 接收者ID
     */
    public void broadcastMessageToReceiver(Message message, Long receiverId) {
        try {
            // 1. 获取接收者用户信息
            User receiver = userService.getUserBasicInfo(receiverId);
            if (receiver == null) {
                log.warn("接收者不存在: receiverId={}", receiverId);
                return;
            }

            // 2. 获取发送者用户名
            User sender = userService.getUserBasicInfo(message.getSenderId());
            String senderName = sender != null ? sender.getUsername() : "Unknown";

            // 3. 构建消息响应对象
            MessageResponse response = new MessageResponse();
            response.setId(message.getId());
            response.setMessageId(message.getId()); // 前端期望messageId字段
            response.setFromId(message.getSenderId());
            response.setFromName(senderName);
            response.setContent(extractContent(message));
            response.setMsgContent(extractContent(message));
            response.setTimestamp(message.getCreatedAt());
            response.setIsFromMe(false); // 对接收者来说不是自己发的
            response.setMessageType(message.getMessageType());
            response.setChatId(String.valueOf(message.getChatId()));
            response.setTargetId(message.getChatId()); // 前端期望targetId字段
            response.setRoomId(String.valueOf(message.getChatId()));
            response.setIsRecalled(message.getIsRecalled() != null ? message.getIsRecalled() : 0);

            // 4. ✅ 检查接收者是否在线
            boolean isOnline = onlineStatusService.isUserOnline(receiverId);
            
            if (isOnline) {
                // 在线：立即发送消息，状态为DELIVERED
                response.setStatus(2); // DELIVERED状态
                
                messagingTemplate.convertAndSendToUser(
                    receiver.getUsername(),
                    "/queue/private",
                    response
                );

                log.info("✅ 消息已转发给在线接收者: messageId={}, receiverId={}, receiverUsername={}",
                    message.getId(), receiverId, receiver.getUsername());
            } else {
                // 离线：消息已存储在数据库，状态为SENT
                // 用户上线时会通过HTTP API拉取离线消息
                response.setStatus(1); // SENT状态
                
                log.info("📦 接收者离线，消息已存储: messageId={}, receiverId={}", 
                    message.getId(), receiverId);
                
                // 可选：存储到离线消息队列（Redis）以便快速推送
                storeOfflineMessage(receiverId, response);
                
            }

        } catch (Exception e) {
            log.error("❌ 转发消息失败: messageId={}, receiverId={}",
                message.getId(), receiverId, e);
        }
    }

    /**
     * ✅ 存储离线消息到Redis
     * @param userId 用户ID
     * @param message 消息对象
     */
    private void storeOfflineMessage(Long userId, MessageResponse message) {
        try {
            // 使用Redis List存储离线消息
            String offlineKey = "chat:offline:" + userId;
            
            // ✅ 实现Redis离线消息队列
            if (redisTemplate != null) {
                redisTemplate.opsForList().rightPush(offlineKey, message);
                redisTemplate.expire(offlineKey, 7, java.util.concurrent.TimeUnit.DAYS); // 7天过期
                log.debug("✅ 离线消息已存储到Redis: userId={}, messageId={}", userId, message.getId());
            } else {
                log.warn("⚠️ RedisTemplate未注入，离线消息仅存储在数据库");
            }
        } catch (Exception e) {
            log.error("❌ 存储离线消息到Redis失败: userId={}, 消息将仅存储在数据库", userId, e);
        }
    }

    /**
     * 向发送者确认消息已发送
     * @param message 消息对象
     * @param senderId 发送者ID
     * @param clientMessageId 客户端消息ID（用于关联临时消息）
     */
    public void confirmMessageToSender(Message message, Long senderId, String clientMessageId) {
        try {
            User sender = userService.getUserBasicInfo(senderId);
            if (sender == null) {
                log.warn("发送者不存在: senderId={}", senderId);
                return;
            }

            MessageResponse response = new MessageResponse();
            response.setId(message.getId());
            response.setMessageId(message.getId()); // 前端期望messageId字段
            response.setFromId(message.getSenderId());
            response.setFromName(sender.getUsername());
            response.setContent(extractContent(message));
            response.setMsgContent(extractContent(message));
            response.setTimestamp(message.getCreatedAt());
            response.setStatus(message.getStatus() == null ? Message.STATUS_SENT : message.getStatus());
            response.setIsFromMe(true);
            response.setMessageType(message.getMessageType());
            response.setChatId(String.valueOf(message.getChatId()));
            response.setTargetId(message.getChatId()); // 前端期望targetId字段
            response.setRoomId(String.valueOf(message.getChatId()));
            response.setClientMessageId(clientMessageId); // 关联临时消息
            response.setIsRecalled(message.getIsRecalled() != null ? message.getIsRecalled() : 0);

            messagingTemplate.convertAndSendToUser(
                sender.getUsername(),
                "/queue/private",
                response
            );

            log.info("✅ 已向发送者确认消息: messageId={}, senderId={}, clientMessageId={}",
                message.getId(), senderId, clientMessageId);

        } catch (Exception e) {
            log.error("❌ 向发送者确认消息失败: messageId={}, senderId={}",
                message.getId(), senderId, e);
        }
    }

    /**
     * 更新聊天列表给相关用户
     * @param chatList 聊天列表对象
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     */
    public void updateChatListForUsers(ChatList chatList, Long userId1, Long userId2) {
        try {
            User user1 = userService.getUserBasicInfo(userId1);
            User user2 = userService.getUserBasicInfo(userId2);

            if (user1 != null) {
                messagingTemplate.convertAndSendToUser(
                    user1.getUsername(),
                    "/queue/chat-list-update",
                    chatList
                );
                log.debug("聊天列表更新已发送给用户: userId={}", userId1);
            }

            if (user2 != null) {
                messagingTemplate.convertAndSendToUser(
                    user2.getUsername(),
                    "/queue/chat-list-update",
                    chatList
                );
                log.debug("聊天列表更新已发送给用户: userId={}", userId2);
            }
        } catch (Exception e) {
            log.error("更新聊天列表失败: userId1={}, userId2={}", userId1, userId2, e);
        }
    }

    /**
     * ✅ 广播群组成员变更事件
     * @param groupId 群组ID
     * @param changeType 变更类型：MEMBER_ADDED, MEMBER_REMOVED, MEMBER_LEFT, ROLE_CHANGED
     * @param affectedUserId 受影响的用户ID
     * @param operatorId 操作者ID（可选）
     * @param additionalData 额外数据（可选）
     */
    public void broadcastGroupMemberChange(Long groupId, String changeType, Long affectedUserId, 
                                          Long operatorId, java.util.Map<String, Object> additionalData) {
        try {
            log.info("📢 开始广播群组成员变更: groupId={}, changeType={}, affectedUserId={}, operatorId={}", 
                groupId, changeType, affectedUserId, operatorId);

            // 1. 获取群组所有成员列表
            List<Long> memberIds = getGroupMemberIds(groupId);
            
            if (memberIds.isEmpty()) {
                log.warn("群组成员列表为空，无法广播: groupId={}", groupId);
                return;
            }

            // 2. 构建群组成员变更事件消息
            java.util.Map<String, Object> changeEvent = new java.util.HashMap<>();
            changeEvent.put("type", "GROUP_MEMBER_CHANGE");
            changeEvent.put("groupId", groupId);
            changeEvent.put("changeType", changeType);
            changeEvent.put("affectedUserId", affectedUserId);
            changeEvent.put("operatorId", operatorId);
            changeEvent.put("timestamp", new java.util.Date());
            
            // 添加额外数据
            if (additionalData != null && !additionalData.isEmpty()) {
                changeEvent.putAll(additionalData);
            }

            // 3. 获取受影响用户的信息
            User affectedUser = userService.getUserBasicInfo(affectedUserId);
            if (affectedUser != null) {
                changeEvent.put("affectedUsername", affectedUser.getUsername());
                changeEvent.put("affectedNickname", affectedUser.getNickname());
                changeEvent.put("affectedAvatar", affectedUser.getAvatar());
            }

            // 4. 获取操作者信息
            if (operatorId != null) {
                User operator = userService.getUserBasicInfo(operatorId);
                if (operator != null) {
                    changeEvent.put("operatorUsername", operator.getUsername());
                    changeEvent.put("operatorNickname", operator.getNickname());
                }
            }

            // 5. 批量广播给所有群成员
            int successCount = 0;
            int failCount = 0;

            for (Long memberId : memberIds) {
                try {
                    // 检查成员是否在线
                    boolean isOnline = onlineStatusService.isUserOnline(memberId);
                    
                    if (isOnline) {
                        User member = userService.getUserBasicInfo(memberId);
                        if (member != null) {
                            // 在线：立即发送WebSocket消息
                            messagingTemplate.convertAndSendToUser(
                                member.getUsername(),
                                "/queue/group-member-change",
                                changeEvent
                            );
                            successCount++;
                            log.debug("✅ 群组成员变更事件已发送: memberId={}, username={}", 
                                memberId, member.getUsername());
                        }
                    } else {
                        // 离线：记录日志，用户上线后会通过HTTP API获取最新群组信息
                        failCount++;
                        log.debug("📦 成员离线，变更事件未发送: memberId={}", memberId);
                    }
                } catch (Exception e) {
                    log.error("发送群组成员变更事件失败: memberId={}", memberId, e);
                    failCount++;
                }
            }

            log.info("✅ 群组成员变更广播完成: groupId={}, changeType={}, 成功={}, 离线={}", 
                groupId, changeType, successCount, failCount);

        } catch (Exception e) {
            log.error("❌ 广播群组成员变更失败: groupId={}, changeType={}", groupId, changeType, e);
        }
    }

    /**
     * ✅ 广播群组信息变更事件
     * @param groupId 群组ID
     * @param changeType 变更类型：INFO_UPDATED, OWNER_TRANSFERRED, GROUP_DISSOLVED
     * @param operatorId 操作者ID
     * @param additionalData 额外数据
     */
    public void broadcastGroupInfoChange(Long groupId, String changeType, Long operatorId, 
                                        java.util.Map<String, Object> additionalData) {
        try {
            log.info("📢 开始广播群组信息变更: groupId={}, changeType={}, operatorId={}", 
                groupId, changeType, operatorId);

            // 1. 获取群组所有成员列表
            List<Long> memberIds = getGroupMemberIds(groupId);
            
            if (memberIds.isEmpty()) {
                log.warn("群组成员列表为空，无法广播: groupId={}", groupId);
                return;
            }

            // 2. 构建群组信息变更事件消息
            java.util.Map<String, Object> changeEvent = new java.util.HashMap<>();
            changeEvent.put("type", "GROUP_INFO_CHANGE");
            changeEvent.put("groupId", groupId);
            changeEvent.put("changeType", changeType);
            changeEvent.put("operatorId", operatorId);
            changeEvent.put("timestamp", new java.util.Date());
            
            // 添加额外数据
            if (additionalData != null && !additionalData.isEmpty()) {
                changeEvent.putAll(additionalData);
            }

            // 3. 获取操作者信息
            if (operatorId != null) {
                User operator = userService.getUserBasicInfo(operatorId);
                if (operator != null) {
                    changeEvent.put("operatorUsername", operator.getUsername());
                    changeEvent.put("operatorNickname", operator.getNickname());
                }
            }

            // 4. 批量广播给所有群成员
            int successCount = 0;

            for (Long memberId : memberIds) {
                try {
                    boolean isOnline = onlineStatusService.isUserOnline(memberId);
                    
                    if (isOnline) {
                        User member = userService.getUserBasicInfo(memberId);
                        if (member != null) {
                            messagingTemplate.convertAndSendToUser(
                                member.getUsername(),
                                "/queue/group-info-change",
                                changeEvent
                            );
                            successCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("发送群组信息变更事件失败: memberId={}", memberId, e);
                }
            }

            log.info("✅ 群组信息变更广播完成: groupId={}, changeType={}, 成功={}", 
                groupId, changeType, successCount);

        } catch (Exception e) {
            log.error("❌ 广播群组信息变更失败: groupId={}, changeType={}", groupId, changeType, e);
        }
    }

    /**
     * ✅ 广播消息反应变更事件
     * @param chatId 聊天ID（sharedChatId）
     * @param reactionData 反应数据
     */
    public void broadcastReactionChange(Long chatId, java.util.Map<String, Object> reactionData) {
        try {
            log.info("📢 开始广播消息反应变更: chatId={}, messageId={}", 
                chatId, reactionData.get("messageId"));

            // 1. 确定接收者列表
            List<Long> recipientIds = new java.util.ArrayList<>();
            
            // 检查是否是群聊
            String sql = "SELECT gm.user_id FROM `group` g JOIN group_member gm ON gm.group_id = g.id "
                    + "WHERE g.shared_chat_id = ? AND g.status = 1 AND gm.join_status = 'ACCEPTED' AND gm.kicked_at IS NULL";
            List<Long> groupMembers = jdbcTemplate.queryForList(sql, Long.class, chatId);
            
            if (!groupMembers.isEmpty()) {
                // 群聊：发送给所有群成员
                recipientIds.addAll(groupMembers);
                log.debug("群聊反应变更，接收者数量: {}", recipientIds.size());
            } else {
                // 私聊：发送给聊天的两个参与者
                String privateSql = "SELECT participant_1_id FROM shared_chat WHERE id = ? AND chat_type = 'PRIVATE' "
                        + "UNION SELECT participant_2_id FROM shared_chat WHERE id = ? AND chat_type = 'PRIVATE'";
                recipientIds.addAll(jdbcTemplate.queryForList(privateSql, Long.class, chatId, chatId));
                log.debug("私聊反应变更，接收者数量: {}", recipientIds.size());
            }

            if (recipientIds.isEmpty()) {
                log.warn("未找到接收者: chatId={}", chatId);
                return;
            }

            // 2. 构建反应变更事件消息
            java.util.Map<String, Object> reactionEvent = new java.util.HashMap<>();
            reactionEvent.put("type", "MESSAGE_REACTION_CHANGE");
            reactionEvent.put("chatId", chatId);
            reactionEvent.put("messageId", reactionData.get("messageId"));
            reactionEvent.put("userId", reactionData.get("userId"));
            reactionEvent.put("reactionType", reactionData.get("reactionType"));
            reactionEvent.put("action", reactionData.get("action")); // "add" or "remove"
            reactionEvent.put("reactions", reactionData.get("reactions")); // 完整的反应统计
            reactionEvent.put("timestamp", new java.util.Date());

            // 3. 批量广播给所有相关用户
            int successCount = 0;
            int failCount = 0;

            for (Long recipientId : recipientIds) {
                try {
                    // 检查用户是否在线
                    boolean isOnline = onlineStatusService.isUserOnline(recipientId);
                    
                    if (isOnline) {
                        User recipient = userService.getUserBasicInfo(recipientId);
                        if (recipient != null) {
                            // 在线：立即发送WebSocket消息
                            messagingTemplate.convertAndSendToUser(
                                recipient.getUsername(),
                                "/queue/reaction-change",
                                reactionEvent
                            );
                            successCount++;
                            log.debug("✅ 反应变更事件已发送: recipientId={}, username={}", 
                                recipientId, recipient.getUsername());
                        }
                    } else {
                        // 离线：用户上线后会通过HTTP API获取最新消息（包含反应）
                        failCount++;
                        log.debug("📦 用户离线，反应变更事件未发送: recipientId={}", recipientId);
                    }
                } catch (Exception e) {
                    log.error("发送反应变更事件失败: recipientId={}", recipientId, e);
                    failCount++;
                }
            }

            log.info("✅ 消息反应变更广播完成: chatId={}, messageId={}, 成功={}, 离线={}", 
                chatId, reactionData.get("messageId"), successCount, failCount);

        } catch (Exception e) {
            log.error("❌ 广播消息反应变更失败: chatId={}", chatId, e);
        }
    }

    /**
     * 提取消息内容
     * @param message 消息对象
     * @return 消息内容字符串
     */
    private String extractContent(Message message) {
        if (message.getContent() == null) {
            return "";
        }

        // 如果content是TextMessageContent对象
        if (message.getContent() instanceof com.web.vo.message.TextMessageContent) {
            com.web.vo.message.TextMessageContent textContent =
                (com.web.vo.message.TextMessageContent) message.getContent();
            return textContent.getContent();
        }

        // 如果是字符串，直接返回
        return message.getContent().toString();
    }
}
