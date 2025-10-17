package com.web.Config;

import cn.hutool.json.JSONUtil;
import com.web.constant.WsContentType;
import com.web.service.WebSocketService;
import com.web.util.ResultUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * WebSocket 消息处理器
 */
@Slf4j
@Component
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    @Autowired
    private WebSocketService webSocketService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    /**
     * 处理WebSocket消息
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否是关闭消息
        if (frame instanceof CloseWebSocketFrame) {
            webSocketService.offline(ctx.channel());
            ctx.channel().close();
            return;
        }

        // 判断是否是Ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        // 判断是否是Pong消息
        if (frame instanceof PongWebSocketFrame) {
            // Pong消息不需要处理
            return;
        }

        // 本程序只支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format(
                    "%s frame types not supported", frame.getClass().getName()));
        }

        // 处理文本消息
        String request = ((TextWebSocketFrame) frame).text();
        log.info("收到WebSocket消息: {}", request);

        try {
            // 解析消息
            WebSocketMessage message = JSONUtil.toBean(request, WebSocketMessage.class);

            // 根据消息类型处理
            switch (message.getType()) {
                case "auth":
                    handleAuthMessage(ctx, message);
                    break;
                case "chat":
                    handleChatMessage(ctx, message);
                    break;
                case "heartbeat":
                    handleHeartbeatMessage(ctx, message);
                    break;
                case "typing":
                    handleTypingMessage(ctx, message);
                    break;
                case "message_status":
                    handleMessageStatusMessage(ctx, message);
                    break;
                case "reaction":
                    handleReactionMessage(ctx, message);
                    break;
                case "recall":
                    handleRecallMessage(ctx, message);
                    break;
                default:
                    log.warn("未知消息类型: {}", message.getType());
                    sendErrorMessage(ctx, "未知消息类型");
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendErrorMessage(ctx, "消息格式错误");
        }
    }

    /**
     * 处理认证消息
     */
    private void handleAuthMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            // 转换data为Map以便安全访问
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) message.getData();
            String token = dataMap.get("token").toString();
            webSocketService.online(ctx.channel(), token);

            // 发送认证成功消息
            WebSocketMessage response = new WebSocketMessage();
            response.setType("auth_success");
            response.setData(ResultUtil.Succeed("认证成功"));
            response.setTimestamp(new Date());
            sendMessage(ctx, response);

        } catch (Exception e) {
            log.error("WebSocket认证失败", e);
            sendErrorMessage(ctx, "认证失败");
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            Long userId = WebSocketService.Online_Channel.get(ctx.channel());
            if (userId == null) {
                sendErrorMessage(ctx, "请先认证");
                return;
            }

            // 解析聊天数据
            Object chatData = message.getData();
            if (chatData instanceof java.util.Map) {
                java.util.Map<?, ?> chatMap = (java.util.Map<?, ?>) chatData;

                String content = chatMap.get("content").toString();
                Long targetId = Long.valueOf(chatMap.get("targetId").toString());
                String chatType = chatMap.get("chatType").toString(); // PRIVATE 或 GROUP
                Long chatId = chatMap.containsKey("chatId") ?
                    Long.valueOf(chatMap.get("chatId").toString()) : null;
                Integer messageType = chatMap.containsKey("messageType") ?
                    Integer.valueOf(chatMap.get("messageType").toString()) : 1;

                // 这里可以调用消息服务来保存消息并转发
                // 暂时直接转发消息
                WebSocketMessage response = new WebSocketMessage();
                response.setType("chat_message");
                response.setData(java.util.Map.of(
                    "fromUserId", userId,
                    "content", content,
                    "targetId", targetId,
                    "chatType", chatType,
                    "chatId", chatId,
                    "messageType", messageType,
                    "timestamp", new Date()
                ));
                response.setTimestamp(new Date());

                // 发送消息给目标用户
                webSocketService.sendMsgToUser(response, userId, targetId);

                // 发送确认给发送者
                sendMessage(ctx, java.util.Map.of("type", "message_sent", "messageId", System.currentTimeMillis()));
            }
        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
            sendErrorMessage(ctx, "消息发送失败");
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeatMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        WebSocketMessage response = new WebSocketMessage();
        response.setType("heartbeat_response");
        response.setData(ResultUtil.Succeed("pong"));
        response.setTimestamp(new Date());
        sendMessage(ctx, response);
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(ChannelHandlerContext ctx, String errorMsg) {
        WebSocketMessage response = new WebSocketMessage();
        response.setType("error");
        response.setData(ResultUtil.Fail(errorMsg));
        response.setTimestamp(new Date());
        sendMessage(ctx, response);
    }

    /**
     * 处理打字指示器消息
     */
    private void handleTypingMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            Long userId = WebSocketService.Online_Channel.get(ctx.channel());
            if (userId == null) {
                sendErrorMessage(ctx, "请先认证");
                return;
            }

            // 解析打字数据
            Object typingData = message.getData();
            if (typingData instanceof java.util.Map) {
                java.util.Map<?, ?> typingMap = (java.util.Map<?, ?>) typingData;

                Long targetId = Long.valueOf(typingMap.get("targetId").toString());
                String chatType = typingMap.get("chatType").toString(); // PRIVATE 或 GROUP
                String action = typingMap.get("action").toString(); // start 或 stop

                // 构建打字指示器消息转发给目标用户
                WebSocketMessage typingResponse = new WebSocketMessage();
                typingResponse.setType("typing");
                typingResponse.setFromUserId(userId);
                typingResponse.setToUserId(targetId);
                typingResponse.setData(java.util.Map.of(
                    "fromUserId", userId,
                    "action", action,
                    "chatType", chatType,
                    "timestamp", new Date()
                ));
                typingResponse.setTimestamp(new Date());

                // 发送给目标用户
                webSocketService.sendMsgToUser(typingResponse, userId, targetId);
            }
        } catch (Exception e) {
            log.error("处理打字指示器消息失败", e);
            sendErrorMessage(ctx, "打字指示器消息发送失败");
        }
    }

    /**
     * 处理消息状态更新消息
     */
    private void handleMessageStatusMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            Long userId = WebSocketService.Online_Channel.get(ctx.channel());
            if (userId == null) {
                sendErrorMessage(ctx, "请先认证");
                return;
            }

            // 解析消息状态数据
            Object statusData = message.getData();
            if (statusData instanceof java.util.Map) {
                java.util.Map<?, ?> statusMap = (java.util.Map<?, ?>) statusData;

                String messageId = statusMap.get("messageId").toString();
                Integer status = Integer.valueOf(statusMap.get("status").toString());
                Long targetId = Long.valueOf(statusMap.get("targetId").toString());

                // 构建消息状态更新消息转发给发送者
                WebSocketMessage statusResponse = new WebSocketMessage();
                statusResponse.setType("message_status");
                statusResponse.setMessageId(messageId);
                statusResponse.setFromUserId(userId);
                statusResponse.setToUserId(targetId);
                statusResponse.setMessageStatus(status);
                statusResponse.setData(java.util.Map.of(
                    "messageId", messageId,
                    "status", status,
                    "fromUserId", userId,
                    "timestamp", new Date()
                ));
                statusResponse.setTimestamp(new Date());

                // 发送给消息发送者
                webSocketService.sendMsgToUser(statusResponse, userId, targetId);
            }
        } catch (Exception e) {
            log.error("处理消息状态更新消息失败", e);
            sendErrorMessage(ctx, "消息状态更新失败");
        }
    }

    /**
     * 处理消息反应消息
     */
    private void handleReactionMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            Long userId = WebSocketService.Online_Channel.get(ctx.channel());
            if (userId == null) {
                sendErrorMessage(ctx, "请先认证");
                return;
            }

            // 解析反应数据
            Object reactionData = message.getData();
            if (reactionData instanceof java.util.Map) {
                java.util.Map<?, ?> reactionMap = (java.util.Map<?, ?>) reactionData;

                String messageId = reactionMap.get("messageId").toString();
                String reaction = reactionMap.get("reaction").toString();
                Long targetId = Long.valueOf(reactionMap.get("targetId").toString());
                String action = reactionMap.get("action").toString(); // add 或 remove

                // 构建反应消息转发给目标用户
                WebSocketMessage reactionResponse = new WebSocketMessage();
                reactionResponse.setType("reaction");
                reactionResponse.setMessageId(messageId);
                reactionResponse.setFromUserId(userId);
                reactionResponse.setToUserId(targetId);
                reactionResponse.setData(java.util.Map.of(
                    "messageId", messageId,
                    "reaction", reaction,
                    "action", action,
                    "fromUserId", userId,
                    "timestamp", new Date()
                ));
                reactionResponse.setTimestamp(new Date());

                // 发送给目标用户
                webSocketService.sendMsgToUser(reactionResponse, userId, targetId);
            }
        } catch (Exception e) {
            log.error("处理消息反应消息失败", e);
            sendErrorMessage(ctx, "消息反应失败");
        }
    }

    /**
     * 处理消息撤回消息
     */
    private void handleRecallMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            Long userId = WebSocketService.Online_Channel.get(ctx.channel());
            if (userId == null) {
                sendErrorMessage(ctx, "请先认证");
                return;
            }

            // 解析撤回数据
            Object recallData = message.getData();
            if (recallData instanceof java.util.Map) {
                java.util.Map<?, ?> recallMap = (java.util.Map<?, ?>) recallData;

                String messageId = recallMap.get("messageId").toString();
                Long targetId = Long.valueOf(recallMap.get("targetId").toString());

                // 构建撤回消息转发给目标用户
                WebSocketMessage recallResponse = new WebSocketMessage();
                recallResponse.setType("recall");
                recallResponse.setMessageId(messageId);
                recallResponse.setFromUserId(userId);
                recallResponse.setToUserId(targetId);
                recallResponse.setMessageStatus(5); // 已撤回状态
                recallResponse.setData(java.util.Map.of(
                    "messageId", messageId,
                    "fromUserId", userId,
                    "timestamp", new Date()
                ));
                recallResponse.setTimestamp(new Date());

                // 发送给目标用户
                webSocketService.sendMsgToUser(recallResponse, userId, targetId);
            }
        } catch (Exception e) {
            log.error("处理消息撤回消息失败", e);
            sendErrorMessage(ctx, "消息撤回失败");
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(ChannelHandlerContext ctx, Object message) {
        if (ctx.channel().isActive()) {
            String jsonMessage = JSONUtil.toJsonStr(message);
            ctx.channel().writeAndFlush(new TextWebSocketFrame(jsonMessage));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("WebSocket连接建立: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("WebSocket连接断开: {}", ctx.channel().remoteAddress());
        webSocketService.offline(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket连接异常", cause);
        webSocketService.offline(ctx.channel());
        ctx.channel().close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            String eventType = null;

            switch (event.state()) {
                case READER_IDLE:
                    eventType = "读空闲";
                    break;
                case WRITER_IDLE:
                    eventType = "写空闲";
                    break;
                case ALL_IDLE:
                    eventType = "读写空闲";
                    // 发送心跳
                    WebSocketMessage heartbeat = new WebSocketMessage();
                    heartbeat.setType("heartbeat");
                    heartbeat.setData("ping");
                    heartbeat.setTimestamp(new Date());
                    sendMessage(ctx, heartbeat);
                    break;
                default:
                    break;
            }
            log.debug("连接 {} 发生 {} 事件", ctx.channel().remoteAddress(), eventType);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}