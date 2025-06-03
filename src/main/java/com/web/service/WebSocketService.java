package com.web.service;

import cn.hutool.core.util.StrUtil;
import com.web.service.AuthService;
import cn.hutool.json.JSONUtil;
import com.web.constant.WsContentType;
import com.web.dto.NotifyDto;
import com.web.model.Message;
import com.web.util.CacheUtil;
import com.web.util.JwtUtil;
import com.web.util.ResultUtil;
import io.jsonwebtoken.Claims;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Data;
import org.slf4j.Logger; // Added
import org.slf4j.LoggerFactory; // Added
import org.springframework.beans.factory.annotation.Autowired; // Added
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// Added imports
import com.web.constant.UserOnlineStatus;
import com.web.constant.ContactStatus;
import com.web.mapper.UserMapper;
import java.util.Map;
// NotifyDto is already imported but will be used generically.
import com.web.Config.RedisConfig; // For USER_MESSAGE_TOPIC
import com.web.dto.RedisBroadcastMsg;
import org.springframework.data.redis.core.RedisTemplate;
import cn.hutool.json.JSONUtil; // Already uses this, ensure it's used for RedisBroadcastMsg content if needed.


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date; // Added for NotifyDto time
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务类，主要用于处理 WebSocket 的连接、消息发送、用户在线管理等功能。
 */
@Service
public class WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class); // Added logger

    private static UserMapper userMapper;
    private static ContactService contactService;
    // Keep existing userService for now, as updateAndBroadcastStatus has a fallback to it.
    // Eventually, the direct userService.online/offline might be fully replaced.
    @Resource
    @Lazy // 延迟加载 AuthService，避免循环依赖
    AuthService userService;


    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketService.userMapper = userMapper;
    }

    @Autowired
    public void setContactService(ContactService contactService) {
        WebSocketService.contactService = contactService;
    }

    private static RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        WebSocketService.redisTemplate = redisTemplate;
    }

    /**
     * 内部静态类，用于封装 WebSocket 消息的内容。
     */
    @Data
    public static class WsContent {
        private String type; // 消息类型
        private Object content; // 消息内容
    }

    @Resource
    @Lazy // 延迟加载 AuthService，避免循环依赖
    // AuthService userService; // This is now defined above with other static fields if needed there, or kept as instance field if some methods still use it directly.
                               // The provided code has it as an instance field, so keeping that structure for now.

    @Resource
    CacheUtil cacheUtil; // 注入缓存工具类，用于管理用户会话缓存

    // 在线用户映射表：userId -> Channel（用于查找用户对应的连接）
    public static final ConcurrentHashMap<Long, Channel> Online_User = new ConcurrentHashMap<>();

    // 在线通道映射表：Channel -> userId（用于通过连接查找用户）
    public static final ConcurrentHashMap<Channel, Long> Online_Channel = new ConcurrentHashMap<>();

    /**
     * 用户上线处理方法
     * @param channel WebSocket 连接通道
     * @param token 用户的身份令牌
     */
    public void online(Channel channel, String token) {
        try {
            // 解析 token，获取用户信息
            Claims claims = JwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class); // 从 token 中获取用户 ID 并转换为 Long
            if (userId == null) {
                throw new IllegalArgumentException("Invalid userId in token");
            }

            String cacheToken = cacheUtil.getUserSessionCache(userId); // 从缓存中获取用户会话 token

            // 验证 token 是否有效（是否是最新登录的会话）
            if (!token.equals(cacheToken)) {
                // 如果 token 无效，通知用户并关闭连接
                sendMsg(channel, ResultUtil.Fail("已在其他地方登录"), WsContentType.Msg);
                channel.close();
                return;
            }

            // 将用户加入在线映射表
            Online_User.put(userId, channel);
            Online_Channel.put(channel, userId);

            // 通知 UserService 用户上线 // REMOVE THIS
            // userService.online(userId);
            updateAndBroadcastStatus(userId, UserOnlineStatus.ONLINE); // ADD THIS
        } catch (Exception e) {
            // 如果发生异常，通知用户并关闭连接
            sendMsg(channel, ResultUtil.Fail("连接错误"), WsContentType.Msg);
            channel.close();
        }
    }

    /**
     * 用户下线处理方法
     * @param channel WebSocket 连接通道
     */
    public void offline(Channel channel) {
        Long userId = Online_Channel.get(channel); // 根据连接通道获取用户 ID
        if (userId != null) {
            // 从在线映射表中移除用户
            Online_User.remove(userId);
            Online_Channel.remove(channel);

            // 通知 UserService 用户下线 // REMOVE THIS
            // userService.offline(userId);
            updateAndBroadcastStatus(userId, UserOnlineStatus.OFFLINE); // ADD THIS
        }
    }

    /**
     * 向指定的连接通道发送消息
     * @param channel WebSocket 连接通道
     * @param msg 消息内容
     * @param type 消息类型
     */
    private void sendMsg(Channel channel, Object msg, String type) {
        WsContent wsContent = new WsContent();
        wsContent.setType(type); // 设置消息类型
        wsContent.setContent(msg); // 设置消息内容
        channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(wsContent))); // 发送消息
    }

    /**
     * 向特定用户发送消息（包括发送方和目标用户）
     * @param msg 消息内容
     * @param userId 发送方用户 ID
     * @param targetId 目标用户 ID
     */
    public void sendMsgToUser(Object msgPayload, Long fromUserId, Long targetId) { // msgPayload is likely a Message or NotifyDto
        if (redisTemplate == null) {
            log.warn("RedisTemplate not injected in sendMsgToUser for message from {} to {}. Message not sent via Redis.", fromUserId, targetId);
            // Fallback to direct send if user is local? Or just fail? For now, log and return.
            // Alternative: direct send if target is local, but that bypasses Redis for that message.
            // For consistency, if Redis is down/unconfigured, messages might not be sent.
            Channel localTargetChannel = Online_User.get(targetId);
            if (localTargetChannel != null && localTargetChannel.isActive()) {
                log.warn("Attempting direct local send for user {} due to missing RedisTemplate.", targetId);
                // We need to know the WsContentType of msgPayload. This is problematic for a generic Object.
                // This fallback needs a way to determine type, or sendMsg needs to handle Object better.
                // For now, assuming msgPayload is a Message DTO if it's not a NotifyDto
                String type = (msgPayload instanceof NotifyDto) ? ((NotifyDto<?>)msgPayload).getType() : WsContentType.MSG.getType();
                sendMsg(localTargetChannel, msgPayload, type);
            }
            Channel localFromChannel = Online_User.get(fromUserId);
             if (localFromChannel != null && localFromChannel.isActive() && !fromUserId.equals(targetId)) {
                log.warn("Attempting direct local self-send for user {} due to missing RedisTemplate.", fromUserId);
                String type = (msgPayload instanceof NotifyDto) ? ((NotifyDto<?>)msgPayload).getType() : WsContentType.MSG.getType();
                sendMsg(localFromChannel, msgPayload, type);
            }
            return;
        }
        String messageBodyJson = JSONUtil.toJsonStr(msgPayload);

        // Send to target user via Redis
        RedisBroadcastMsg toTargetMsg = RedisBroadcastMsg.builder()
                .targetUserId(targetId)
                .messageBody(messageBodyJson)
                .build();
        redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, toTargetMsg);
        log.debug("Published message from {} to target {} via Redis: {}", fromUserId, targetId, messageBodyJson);

        // Send to self (sender) via Redis as well, if they should get a copy on all their sessions
        if (!fromUserId.equals(targetId)) { // Avoid double send if sending to self
            RedisBroadcastMsg toSelfMsg = RedisBroadcastMsg.builder()
                    .targetUserId(fromUserId)
                    .messageBody(messageBodyJson) // Same message body
                    .build();
            redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, toSelfMsg);
            log.debug("Published self-copy of message to user {} via Redis: {}", fromUserId, messageBodyJson);
        }
    }

    // Old sendMsgToGroup and sendNotifyToGroup are removed as group messaging is now handled by services publishing per-user messages to Redis.
    // /**
    //  * 向所有在线用户发送群组消息
    //  * @param message 消息内容
    //  */
    // public void sendMsgToGroup(Message message) {
    //     Online_Channel.forEach((channel, userId) -> {
    //         sendMsg(channel, message, WsContentType.Msg);
    //     });
    // }

    /**
     * 获取当前在线用户数量
     * @return 在线用户数量
     */
    public Integer getOnlineNum() {
        return Online_User.size();
    }

    /**
     * 获取当前所有在线用户的 ID 列表
     * @return 在线用户 ID 列表
     */
    public List<Long> getOnlineUser() {
        return new ArrayList<>(Online_User.keySet());
    }

    /**
     * 向所有在线用户发送通知
     * @param notify 通知内容
     */
    // public void sendNotifyToGroup(NotifyDto notify) {
    //     Online_Channel.forEach((channel, userId) -> {
    //         sendMsg(channel, notify, WsContentType.Notify);
    //     });
    // }

    /**
     * 向特定用户发送视频流消息
     * @param msg 消息内容
     * @param userId 用户 ID
     */
    public void sendVideoToUser(Object msg, Long userId) {
        Channel channel = Online_User.get(userId); // 获取用户的连接通道
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Video); // 发送视频消息
        }
    }

    /**
     * 向特定用户发送文件消息
     * @param msg 文件内容
     * @param userId 用户 ID
     */
    public void sendFileToUser(Object msg, Long userId) {
        Channel channel = Online_User.get(userId); // 获取用户的连接通道
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.File); // 发送文件消息
        }
    }

    private void updateAndBroadcastStatus(Long userId, UserOnlineStatus status) {
        if (userMapper == null || contactService == null || redisTemplate == null) { // Added redisTemplate check
            log.warn("UserMapper, ContactService, or RedisTemplate not injected. Cannot update/broadcast status for user {}.", userId);
            // Potentially call original userService.online/offline as fallback if that's critical
            if (status == UserOnlineStatus.ONLINE && userService != null) {
                log.info("Falling back to userService.online for user {}", userId);
                userService.online(userId);
            }
            if (status == UserOnlineStatus.OFFLINE && userService != null) {
                log.info("Falling back to userService.offline for user {}", userId);
                userService.offline(userId);
            }
            return;
        }
        log.info("Updating status for user {}: {}", userId, status.name());
        userMapper.updateOnlineStatus(userId, status.getCode());

        NotifyDto<Map<String, Object>> notifyDto = new NotifyDto<>();
        notifyDto.setType(WsContentType.STATUS_CHANGE.getType());
        notifyDto.setData(Map.of("userId", userId, "status", status.getCode()));
        notifyDto.setTime(new Date());

        String messageBodyJson = JSONUtil.toJsonStr(notifyDto); // Serialize NotifyDto to JSON string for messageBody

        List<Long> contactFriendIds = contactService.getContactUserIds(userId, ContactStatus.ACCEPTED);

        if (contactFriendIds != null && !contactFriendIds.isEmpty()) {
            log.info("Publishing status change of user {} to Redis for contacts: {}", userId, contactFriendIds);
            for (Long contactId : contactFriendIds) {
                RedisBroadcastMsg broadcastMsg = RedisBroadcastMsg.builder()
                        .targetUserId(contactId)
                        .messageBody(messageBodyJson) // messageBody is the JSON string of NotifyDto
                        .build();
                redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, broadcastMsg); // Send RedisBroadcastMsg object
            }
        } else {
            log.info("User {} has no contacts to notify for status change.", userId);
        }
        // The old sendMessageToUsers call is removed from here.
    }

    // sendMessageToUsers method removed as per plan. sendLocalMessage will handle messages from Redis.
    // public void sendMessageToUsers(List<Long> userIds, NotifyDto<?> notificationPayload) { ... } // REMOVE or DEPRECATE

    /**
     * 【NEW METHOD】Sends a message string to a user connected to THIS SPECIFIC instance.
     * This method is intended to be called by RedisSubscriber.
     * @param targetUserId The ID of the user to send the message to.
     * @param messageText The raw message text (expected to be a JSON string of the actual payload, e.g., serialized NotifyDto or WsContent).
     */
    public void sendLocalMessage(Long targetUserId, String messageText) {
        Channel channel = Online_User.get(targetUserId);
        if (channel != null && channel.isActive()) {
            try {
                // messageText is the JSON of a NotifyDto (or potentially other DTOs in future).
                // We need to deserialize it to get its type for WsContent, then pass the DTO as content.
                NotifyDto<?> receivedPayload = JSONUtil.toBean(messageText, NotifyDto.class, true); // Added 'true' for ignoreError

                if (receivedPayload != null && receivedPayload.getType() != null) {
                    log.debug("Sending local message type '{}' to user {}", receivedPayload.getType(), targetUserId);
                    sendMsg(channel, receivedPayload, receivedPayload.getType());
                } else {
                    log.warn("Could not determine WsContentType from messageText for local delivery to user {}: {}", targetUserId, messageText);
                    // Fallback: send as generic message if type extraction fails and it's just a string.
                    // This might not be desired if strict DTO structure is expected.
                    // sendMsg(channel, messageText, WsContentType.MSG.getType());
                }

            } catch (Exception e) {
                log.error("Send local message to " + targetUserId + " failed: " + messageText, e);
            }
        } else {
            log.debug("User {} not connected to this instance. No local message sent for payload: {}", targetUserId, messageText);
        }
    }
}
