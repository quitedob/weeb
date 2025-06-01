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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务类，主要用于处理 WebSocket 的连接、消息发送、用户在线管理等功能。
 */
@Service
public class WebSocketService {

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
    AuthService userService;

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

            // 通知 UserService 用户上线
            userService.online(userId);
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

            // 通知 UserService 用户下线
            userService.offline(userId);
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
}
