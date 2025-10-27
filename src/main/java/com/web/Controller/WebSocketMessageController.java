package com.web.Controller;

import com.web.model.Message;
import com.web.service.ChatService;
import com.web.service.MessageService;
import com.web.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 消息控制器
 * 处理实时聊天消息
 */
@Slf4j
@Controller
public class WebSocketMessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    /**
     * 将WebSocket消息数据转换为Message对象
     * @param messageData WebSocket消息数据
     * @param userId 发送者ID
     * @return Message对象
     */
    private Message convertWebSocketMessageToMessage(Map<String, Object> messageData, Long userId) {
        Message message = new Message();

        // 设置基本信息
        message.setSenderId(userId);
        message.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        message.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

        // 从Map中提取数据
        String content = (String) messageData.get("content");
        String roomId = (String) messageData.get("roomId");
        String type = (String) messageData.getOrDefault("type", "text");

        // 设置消息内容
        com.web.vo.message.TextMessageContent textContent = new com.web.vo.message.TextMessageContent();
        textContent.setContent(content);

        // 根据类型设置contentType
        if ("image".equals(type)) {
            textContent.setContentType(com.web.constant.TextContentType.IMAGE.getCode());
            textContent.setUrl((String) messageData.get("url"));
        } else if ("file".equals(type)) {
            textContent.setContentType(com.web.constant.TextContentType.FILE.getCode());
            textContent.setUrl((String) messageData.get("url"));
        } else {
            textContent.setContentType(com.web.constant.TextContentType.TEXT.getCode());
        }

        message.setContent(textContent);

        // 设置消息类型和chatId
        if (roomId != null && roomId.startsWith("group_")) {
            message.setMessageType(1); // 群聊
            message.setChatId(extractChatIdFromRoomId(roomId));
        } else {
            message.setMessageType(0); // 私聊
            message.setChatId(extractChatIdFromRoomId(roomId));
        }

        // 设置其他默认值
        message.setReadStatus(0); // 未读
        message.setIsRecalled(0); // 未撤回
        message.setIsShowTime(1); // 显示时间
        message.setUserIp("WebSocket"); // 标记来源为WebSocket
        message.setSource("WebSocket");

        // 设置回复信息（如果有）
        if (messageData.containsKey("replyToMessageId")) {
            Object replyToId = messageData.get("replyToMessageId");
            if (replyToId != null) {
                message.setReplyToMessageId(Long.valueOf(replyToId.toString()));
            }
        }

        // 设置线程信息（如果有）
        if (messageData.containsKey("threadId")) {
            Object threadId = messageData.get("threadId");
            if (threadId != null) {
                message.setThreadId(Long.valueOf(threadId.toString()));
            }
        }

        return message;
    }

    /**
     * 从roomId中提取chatId
     * @param roomId 房间ID
     * @return chatId
     */
    private Long extractChatIdFromRoomId(String roomId) {
        if (roomId == null) return null;

        try {
            // 假设roomId格式为 "private_{userId}" 或 "group_{chatId}"
            if (roomId.startsWith("private_")) {
                // 对于私聊，需要根据发送者和接收者查找或创建chatId
                // 这里暂时返回一个默认值，实际实现需要更复杂的逻辑
                return 1L; // 临时返回值
            } else if (roomId.startsWith("group_")) {
                // 对于群聊，直接提取chatId
                return Long.valueOf(roomId.substring(6));
            }
        } catch (Exception e) {
            log.warn("无法从roomId提取chatId: {}", roomId, e);
        }

        return 1L; // 默认值
    }

    /**
     * 处理连接事件
     */
    @SubscribeMapping("/chat/connect")
    public String handleConnect(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        log.info("用户 {} 连接到聊天服务", principal.getName());

        // 设置用户会话信息
        headerAccessor.getSessionAttributes().put("username", principal.getName());
        headerAccessor.getSessionAttributes().put("connectTime", LocalDateTime.now());

        return "连接成功";
    }

    /**
     * 发送聊天消息
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/chat/{roomId}")
    public Map<String, Object> sendMessage(
            @Payload Map<String, Object> message,
            @DestinationVariable String roomId,
            Principal principal) {

        try {
            log.info("收到消息: from={}, roomId={}, content={}",
                    principal.getName(), roomId, message.get("content"));

            // 构建消息对象
            Map<String, Object> chatMessage = new HashMap<>();
            chatMessage.put("content", message.get("content"));
            chatMessage.put("roomId", roomId);
            chatMessage.put("type", message.getOrDefault("type", "text"));

            // 添加额外字段
            if (message.containsKey("replyToMessageId")) {
                chatMessage.put("replyToMessageId", message.get("replyToMessageId"));
            }
            if (message.containsKey("threadId")) {
                chatMessage.put("threadId", message.get("threadId"));
            }
            if (message.containsKey("url")) {
                chatMessage.put("url", message.get("url"));
            }

            // 保存消息到数据库 - 使用ChatService统一消息存储逻辑
            Message messageObj = convertWebSocketMessageToMessage(chatMessage, SecurityUtils.getCurrentUserId());
            Message savedMessage = chatService.sendMessage(SecurityUtils.getCurrentUserId(), messageObj);

            // 构建返回的消息对象
            Map<String, Object> responseMessage = new HashMap<>();
            responseMessage.put("id", savedMessage.getId());
            responseMessage.put("fromId", savedMessage.getSenderId());
            responseMessage.put("fromName", principal.getName());
            responseMessage.put("content", message.get("content"));
            responseMessage.put("roomId", roomId);
            responseMessage.put("timestamp", savedMessage.getCreatedAt().toLocalDateTime());
            responseMessage.put("type", message.getOrDefault("type", "text"));

            return responseMessage;
        } catch (Exception e) {
            log.error("处理消息失败", e);

            Map<String, Object> errorMessage = new HashMap<>();
            errorMessage.put("error", "消息发送失败");
            errorMessage.put("timestamp", LocalDateTime.now());
            return errorMessage;
        }
    }

    /**
     * 用户加入聊天室
     */
    @MessageMapping("/chat/join/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public Map<String, Object> joinChat(
            @DestinationVariable String roomId,
            Principal principal) {

        log.info("用户 {} 加入聊天室 {}", principal.getName(), roomId);

        Map<String, Object> joinMessage = new HashMap<>();
        joinMessage.put("type", "join");
        joinMessage.put("username", principal.getName());
        joinMessage.put("roomId", roomId);
        joinMessage.put("timestamp", LocalDateTime.now());
        joinMessage.put("message", principal.getName() + " 加入了聊天室");

        return joinMessage;
    }

    /**
     * 用户离开聊天室
     */
    @MessageMapping("/chat/leave/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public Map<String, Object> leaveChat(
            @DestinationVariable String roomId,
            Principal principal) {

        log.info("用户 {} 离开聊天室 {}", principal.getName(), roomId);

        Map<String, Object> leaveMessage = new HashMap<>();
        leaveMessage.put("type", "leave");
        leaveMessage.put("username", principal.getName());
        leaveMessage.put("roomId", roomId);
        leaveMessage.put("timestamp", LocalDateTime.now());
        leaveMessage.put("message", principal.getName() + " 离开了聊天室");

        return leaveMessage;
    }

    /**
     * 用户正在输入
     */
    @MessageMapping("/chat/typing/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public Map<String, Object> userTyping(
            @DestinationVariable String roomId,
            Principal principal) {

        Map<String, Object> typingMessage = new HashMap<>();
        typingMessage.put("type", "typing");
        typingMessage.put("username", principal.getName());
        typingMessage.put("roomId", roomId);
        typingMessage.put("timestamp", LocalDateTime.now());

        return typingMessage;
    }

    /**
     * 发送私聊消息
     */
    @MessageMapping("/chat/private")
    public void sendPrivateMessage(
            @Payload Map<String, Object> message,
            Principal principal) {

        try {
            String targetUser = (String) message.get("targetUser");
            String content = (String) message.get("content");

            log.info("私聊消息: from={}, to={}, content={}",
                    principal.getName(), targetUser, content);

            // 构建消息数据用于保存
            Map<String, Object> chatMessage = new HashMap<>();
            chatMessage.put("content", content);
            chatMessage.put("roomId", "private_" + targetUser); // 私聊房间ID格式
            chatMessage.put("type", message.getOrDefault("type", "text"));

            // 添加额外字段
            if (message.containsKey("replyToMessageId")) {
                chatMessage.put("replyToMessageId", message.get("replyToMessageId"));
            }
            if (message.containsKey("threadId")) {
                chatMessage.put("threadId", message.get("threadId"));
            }
            if (message.containsKey("url")) {
                chatMessage.put("url", message.get("url"));
            }

            // 保存消息到数据库 - 使用ChatService统一消息存储逻辑
            Message messageObj = convertWebSocketMessageToMessage(chatMessage, SecurityUtils.getCurrentUserId());
            Message savedMessage = chatService.sendMessage(SecurityUtils.getCurrentUserId(), messageObj);

            // 构建私聊消息对象
            Map<String, Object> privateMessage = new HashMap<>();
            privateMessage.put("type", "private");
            privateMessage.put("id", savedMessage.getId());
            privateMessage.put("fromId", savedMessage.getSenderId());
            privateMessage.put("fromName", principal.getName());
            privateMessage.put("toUser", targetUser);
            privateMessage.put("content", content);
            privateMessage.put("timestamp", savedMessage.getCreatedAt().toLocalDateTime());
            privateMessage.put("roomId", "private_" + targetUser);

            // 发送给目标用户
            messagingTemplate.convertAndSendToUser(
                    targetUser,
                    "/queue/private",
                    privateMessage
            );

            // 发送给发送者（用于显示在自己的聊天界面）
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/private",
                    privateMessage
            );

        } catch (Exception e) {
            log.error("发送私聊消息失败", e);
        }
    }

    /**
     * 消息撤回
     */
    @MessageMapping("/chat/recall/{messageId}")
    @SendTo("/topic/chat/{roomId}")
    public Map<String, Object> recallMessage(
            @DestinationVariable String messageId,
            @DestinationVariable String roomId,
            Principal principal) {

        try {
            log.info("用户 {} 撤回消息: messageId={}, roomId={}",
                    principal.getName(), messageId, roomId);

            Map<String, Object> recallMessage = new HashMap<>();
            recallMessage.put("type", "recall");
            recallMessage.put("messageId", messageId);
            recallMessage.put("username", principal.getName());
            recallMessage.put("roomId", roomId);
            recallMessage.put("timestamp", LocalDateTime.now());
            recallMessage.put("message", principal.getName() + " 撤回了一条消息");

            // 实际应该调用服务层撤回消息
            // messageService.recallMessage(messageId, principal.getName());

            return recallMessage;
        } catch (Exception e) {
            log.error("撤回消息失败", e);

            Map<String, Object> errorMessage = new HashMap<>();
            errorMessage.put("type", "error");
            errorMessage.put("message", "撤回消息失败");
            return errorMessage;
        }
    }

    /**
     * 处理心跳消息
     */
    @MessageMapping("/chat/heartbeat")
    public void handleHeartbeat(Principal principal) {
        // 更新用户最后活跃时间
        // 可以在这里实现用户在线状态管理
        log.debug("收到用户 {} 的心跳", principal.getName());
    }

    /**
     * 错误处理
     */
    @MessageExceptionHandler
    public void handleException(Exception exception, Principal principal) {
        log.error("WebSocket消息处理异常: user={}, error={}",
                principal.getName(), exception.getMessage(), exception);

        // 发送错误消息给用户
        Map<String, Object> errorMessage = new HashMap<>();
        errorMessage.put("type", "error");
        errorMessage.put("message", "消息处理失败: " + exception.getMessage());
        errorMessage.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                errorMessage
        );
    }
}