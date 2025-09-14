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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.web.constant.UserOnlineStatus;
import com.web.constant.ContactStatus;
import com.web.mapper.UserMapper;
import java.util.Map;
import com.web.Config.RedisConfig;
import com.web.dto.RedisBroadcastMsg;
import org.springframework.data.redis.core.RedisTemplate;
import cn.hutool.json.JSONUtil;


import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务类，主要用于处理 WebSocket 的连接、消息发送、用户在线管理等功能。
 */
@Service
public class WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);

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
    CacheUtil cacheUtil; // 注入缓存工具类，用于管理用户会话缓存

    @Resource
    JwtUtil jwtUtil; // 注入JWT工具类

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
            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class); // 从 token 中获取用户 ID 并转换为 Long
            if (userId == null) {
                throw new IllegalArgumentException("Invalid userId in token");
            }

            String cacheToken = cacheUtil.getUserSessionCache(userId); // 从缓存中获取用户会话 token

            // 验证 token 是否有效（是否是最新登录的会话）
            if (!token.equals(cacheToken)) {
                // 如果 token 无效，通知用户并关闭连接
                sendMsg(channel, ResultUtil.Fail("已在其他地方登录"), WsContentType.MSG.getCode());
                channel.close();
                return;
            }

            // 将用户加入在线映射表
            Online_User.put(userId, channel);
            Online_Channel.put(channel, userId);

            // 通知 UserService 用户上线
            updateAndBroadcastStatus(userId, UserOnlineStatus.ONLINE);
        } catch (Exception e) {
            // 如果发生异常，通知用户并关闭连接
            sendMsg(channel, ResultUtil.Fail("连接错误"), WsContentType.MSG.getCode());
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

            // 通知 UserService 用户下线
            updateAndBroadcastStatus(userId, UserOnlineStatus.OFFLINE);
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
                sendMsg(localTargetChannel, msgPayload, WsContentType.MSG.getCode());
            }
            return;
        }

        // 构建 Redis 广播消息
        RedisBroadcastMsg broadcastMsg = RedisBroadcastMsg.builder()
                .targetUserId(targetId)
                .messageBody(JSONUtil.toJsonStr(msgPayload))
                .build();

        // 通过 Redis 发布消息
        redisTemplate.convertAndSend(RedisConfig.USER_MESSAGE_TOPIC, broadcastMsg);
        log.info("Message sent via Redis from user {} to user {}", fromUserId, targetId);
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
     * 获取在线用户数量
     * @return 在线用户数量
     */
    public Integer getOnlineNum() {
        return Online_User.size();
    }

    /**
     * 获取在线用户列表
     * @return 在线用户ID列表
     */
    public List<Long> getOnlineUser() {
        return new ArrayList<>(Online_User.keySet());
    }

    /**
     * 向指定用户发送视频消息
     * @param msg 视频消息内容
     * @param userId 目标用户 ID
     */
    public void sendVideoToUser(Object msg, Long userId) {
        Channel channel = Online_User.get(userId); // 获取用户的连接通道
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.VIDEO.getCode()); // 发送视频消息
        }
    }

    /**
     * 向指定用户发送文件消息
     * @param msg 文件消息内容
     * @param userId 目标用户 ID
     */
    public void sendFileToUser(Object msg, Long userId) {
        Channel channel = Online_User.get(userId); // 获取用户的连接通道
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.MSG.getCode()); // 发送文件消息
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
        notifyDto.setType(WsContentType.STATUS_CHANGE.getCode());
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
    public void sendLocalMessage(Long targetUserId, String messageText) { // targetUserId is Long
        Channel channel = Online_User.get(targetUserId); // Online_User is ConcurrentHashMap<Long, Channel>
        if (channel != null && channel.isActive()) { // isActive() for Netty Channel
            try {
                log.debug("Sending local message (WsContent JSON) to user {}: {}", targetUserId, messageText);
                // messageText is already the JSON string of the WsContent object
                // that the client expects.
                channel.writeAndFlush(new TextWebSocketFrame(messageText));
            } catch (Exception e) {
                log.error("Send local message to " + targetUserId + " failed for message: " + messageText, e);
            }
        } else {
            log.debug("User {} not connected to this instance. No local message sent for: {}", targetUserId, messageText);
        }
    }
}
