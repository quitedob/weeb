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
    public void sendMsgToUser(Object msg, Long userId, Long targetId) {
        Channel channel = Online_User.get(userId); // 获取发送方的连接通道
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Msg); // 向发送方发送消息
        }
        channel = Online_User.get(targetId); // 获取目标用户的连接通道
        if (channel != null) {
            sendMsg(channel, msg, WsContentType.Msg); // 向目标用户发送消息
        }
    }

    /**
     * 向所有在线用户发送群组消息
     * @param message 消息内容
     */
    public void sendMsgToGroup(Message message) {
        Online_Channel.forEach((channel, userId) -> {
            sendMsg(channel, message, WsContentType.Msg);
        });
    }

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
    public void sendNotifyToGroup(NotifyDto notify) {
        Online_Channel.forEach((channel, userId) -> {
            sendMsg(channel, notify, WsContentType.Notify);
        });
    }

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
        if (userMapper == null || contactService == null) {
            log.warn("UserMapper or ContactService not injected into WebSocketService. Cannot update/broadcast status for user {}.", userId);
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
        notifyDto.setType(WsContentType.STATUS_CHANGE.getType()); // Use the enum's type string
        notifyDto.setData(Map.of("userId", userId, "status", status.getCode()));
        notifyDto.setTime(new Date()); // Set time for the notification

        List<Long> contactFriendIds = contactService.getContactUserIds(userId, ContactStatus.ACCEPTED);
        if (contactFriendIds != null && !contactFriendIds.isEmpty()) {
            log.info("Broadcasting status change of user {} to contacts: {}", userId, contactFriendIds);
            sendMessageToUsers(contactFriendIds, notifyDto);
        } else {
            log.info("User {} has no contacts to notify for status change.", userId);
        }
    }

    // New method as per user spec, adapted to use existing sendMsg structure
    public void sendMessageToUsers(List<Long> userIds, NotifyDto<?> notificationPayload) { // Specifically for NotifyDto
        if (userIds == null || userIds.isEmpty() || notificationPayload == null) {
            return;
        }
        // The existing sendMsg(Channel, Object, String type) expects the 'content' part of WsContent
        // and the type string. The notificationPayload is the 'content'.
        String notificationType = notificationPayload.getType(); // This should be WsContentType.STATUS_CHANGE.getType()

        for (Long targetUserId : userIds) {
            Channel channel = Online_User.get(targetUserId);
            if (channel != null && channel.isActive()) { // Use isActive() for Netty Channel
                // Pass the NotifyDto itself as the content for sendMsg,
                // and its type as the type string for WsContent.
                log.debug("Sending notification type '{}' to user {}", notificationType, targetUserId);
                sendMsg(channel, notificationPayload, notificationType);
            } else {
                log.debug("Channel not active or found for user {} while sending notification type '{}'", targetUserId, notificationType);
            }
        }
    }
}
