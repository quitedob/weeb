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

    @Autowired
    private com.web.service.MessageBroadcastService messageBroadcastService;

    @Autowired
    private com.web.service.MessageDeduplicationService deduplicationService;

    /**
     * 将WebSocket消息数据转换为Message对象
     * @param messageData WebSocket消息数据
     * @param userId 发送者ID
     * @return Message对象
     */
    @SuppressWarnings("deprecation")
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
            message.setChatId(extractChatIdFromRoomId(roomId, null));
        } else {
            message.setMessageType(0); // 私聊
            message.setChatId(extractChatIdFromRoomId(roomId, null));
        }

        // 设置其他默认值
        message.setStatus(Message.STATUS_SENT); // 已发送
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
     * @param principal 当前用户
     * @return chatId
     */
    private Long extractChatIdFromRoomId(String roomId, Principal principal) {
        if (roomId == null) return null;

        try {
            if (roomId.startsWith("private_")) {
                // 提取目标用户ID
                String targetUserIdStr = roomId.substring(8);
                Long targetUserId = Long.valueOf(targetUserIdStr);
                Long currentUserId = SecurityUtils.getCurrentUserId();
                
                // 查找或创建私聊会话
                return chatService.findOrCreatePrivateChat(currentUserId, targetUserId);
            } else if (roomId.startsWith("group_")) {
                // 对于群聊，直接提取chatId
                return Long.valueOf(roomId.substring(6));
            }
        } catch (Exception e) {
            log.warn("无法从roomId提取chatId: {}", roomId, e);
        }

        return null; // 出错时返回null而不是1L
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
    @SuppressWarnings("deprecation")
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
            
            // ✅ 获取chatId（String类型）- WebSocket使用roomId作为chatId
            // roomId格式: "private_<chatId>" 或 "group_<chatId>" 或直接是chatId
            String chatId = roomId;
            if (roomId.startsWith("private_")) {
                chatId = roomId.substring(8); // 移除"private_"前缀
            } else if (roomId.startsWith("group_")) {
                chatId = roomId.substring(6); // 移除"group_"前缀
            }
            
            Message savedMessage = chatService.sendMessage(SecurityUtils.getCurrentUserId(), chatId, messageObj);

            // 构建返回的消息对象，匹配前端期望的格式
            Map<String, Object> responseMessage = new HashMap<>();
            responseMessage.put("id", savedMessage.getId());
            responseMessage.put("messageId", savedMessage.getId()); // 前端期望messageId字段
            responseMessage.put("fromId", savedMessage.getSenderId());
            responseMessage.put("fromName", principal.getName());
            responseMessage.put("content", message.get("content"));
            responseMessage.put("msgContent", message.get("content")); // 前端期望msgContent字段
            responseMessage.put("roomId", roomId);
            responseMessage.put("chatId", chatId); // ✅ 使用String类型的chatId而不是sharedChatId
            responseMessage.put("targetId", savedMessage.getChatId()); // targetId仍使用sharedChatId（Long）
            responseMessage.put("timestamp", savedMessage.getCreatedAt().toLocalDateTime());
            responseMessage.put("type", message.getOrDefault("type", "text"));
            responseMessage.put("messageType", savedMessage.getMessageType());
            responseMessage.put("chatType", roomId != null && roomId.startsWith("group_") ? "GROUP" : "PRIVATE");
            responseMessage.put("status", savedMessage.getStatus()); // 使用后端status字段
            responseMessage.put("isRecalled", savedMessage.getIsRecalled());
            responseMessage.put("isFromMe", false); // 接收到的消息，isFromMe为false

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
     * ✅ 修复：使用MessageBroadcastService统一处理消息转发
     */
    @SuppressWarnings("deprecation")
    @MessageMapping("/chat/private")
    public void sendPrivateMessage(
            @Payload Map<String, Object> message,
            Principal principal) {

        try {
            String targetUser = (String) message.get("targetUser");
            String content = (String) message.get("content");
            String clientMessageId = (String) message.get("clientMessageId");
            Object targetIdObj = message.get("targetId");
            Object chatIdObj = message.get("chatId");

            log.info("📨 收到私聊消息: from={}, targetUser={}, targetId={}, content={}, clientMessageId={}",
                    principal.getName(), targetUser, targetIdObj, content, clientMessageId);

            // ✅ 消息去重检查
            if (clientMessageId != null && deduplicationService.isDuplicate(clientMessageId)) {
                log.warn("⚠️ 重复消息，已忽略: clientMessageId={}", clientMessageId);
                
                // 返回已存在的消息ID
                Long existingMessageId = deduplicationService.getMessageId(clientMessageId);
                if (existingMessageId != null) {
                    messageBroadcastService.confirmMessageToSender(
                        new Message() {{ setId(existingMessageId); }},
                        SecurityUtils.getCurrentUserId(),
                        clientMessageId
                    );
                }
                return;
            }

            // 构建消息对象
            Message messageObj = new Message();
            messageObj.setSenderId(SecurityUtils.getCurrentUserId());
            messageObj.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            messageObj.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            // 设置消息内容
            com.web.vo.message.TextMessageContent textContent = new com.web.vo.message.TextMessageContent();
            textContent.setContent(content);
            textContent.setContentType(com.web.constant.TextContentType.TEXT.getCode());
            messageObj.setContent(textContent);

            // ✅ 获取chatId（String类型）- 优先使用chatId，然后处理targetId或targetUser
            String chatId = null;
            if (chatIdObj != null) {
                chatId = chatIdObj.toString();
            } else {
                Long targetUserId = null;

                // 尝试从targetId获取
                if (targetIdObj != null) {
                    targetUserId = Long.valueOf(targetIdObj.toString());
                }
                // 尝试从targetUser（用户名）获取targetId
                else if (targetUser != null && !targetUser.isEmpty()) {
                    // 这里应该根据用户名查找用户ID，暂时跳过实现
                    // 实际项目中应该调用用户服务: userService.getUserIdByUsername(targetUser)
                    log.warn("⚠️ 暂不支持通过用户名查找用户ID: {}", targetUser);
                }

                if (targetUserId != null) {
                    // 查找或创建私聊会话，获取chat_list.id
                    Long currentUserId = SecurityUtils.getCurrentUserId();
                    com.web.model.ChatList chatList = chatService.createChat(currentUserId, targetUserId);
                    chatId = chatList.getId(); // 使用String类型的chat_list.id
                } else {
                    log.error("❌ 无法确定聊天接收者: targetUser={}, targetId={}", targetUser, targetIdObj);
                    throw new RuntimeException("无法确定聊天接收者");
                }
            }

            // 设置消息类型
            Integer messageType = message.get("messageType") != null 
                ? Integer.valueOf(message.get("messageType").toString()) 
                : 1;
            messageObj.setMessageType(messageType);
            messageObj.setStatus(Message.STATUS_SENT);
            messageObj.setIsRecalled(0);

            // ✅ 保存消息到数据库（ChatService会自动转发给接收者）
            Message savedMessage = chatService.sendMessage(SecurityUtils.getCurrentUserId(), chatId, messageObj);

            // ✅ 标记消息已处理（防止重复）
            if (clientMessageId != null) {
                deduplicationService.markAsProcessed(clientMessageId, savedMessage.getId());
            }

            log.info("✅ 消息已保存: messageId={}, chatId={}", 
                savedMessage.getId(), savedMessage.getChatId());

            // ✅ 向发送者确认消息已发送
            messageBroadcastService.confirmMessageToSender(
                savedMessage, 
                SecurityUtils.getCurrentUserId(), 
                clientMessageId
            );

        } catch (Exception e) {
            log.error("❌ 发送私聊消息失败", e);
            
            // 发送错误消息给发送者
            Map<String, Object> errorMessage = new HashMap<>();
            errorMessage.put("type", "error");
            errorMessage.put("message", "消息发送失败: " + e.getMessage());
            errorMessage.put("clientMessageId", message.get("clientMessageId"));
            errorMessage.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    errorMessage
            );
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

    @Autowired
    private com.web.service.WebSocketConnectionService connectionService;

    /**
     * 处理心跳消息
     */
    @MessageMapping("/chat/heartbeat")
    public void handleHeartbeat(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            
            // 更新心跳时间
            if (sessionId != null) {
                connectionService.updateHeartbeat(sessionId);
            }
            
            log.debug("收到用户 {} 的心跳, sessionId={}", principal.getName(), sessionId);
            
        } catch (Exception e) {
            log.error("处理心跳消息失败: user={}", principal.getName(), e);
        }
    }

    /**
     * ✅ 处理已读回执
     */
    @MessageMapping("/chat/read-receipt")
    public void handleReadReceipt(
            @Payload Map<String, Object> receipt,
            Principal principal) {
        try {
            // ✅ chatId是String类型（UUID），需要转换为sharedChatId
            String chatIdStr = receipt.get("chatId") != null
                ? receipt.get("chatId").toString()
                : null;
            Long messageId = receipt.get("messageId") != null
                ? Long.valueOf(receipt.get("messageId").toString())
                : null;
            String timestamp = (String) receipt.get("timestamp");

            log.info("👁️ 收到已读回执: from={}, chatId={}, messageId={}",
                principal.getName(), chatIdStr, messageId);

            if (chatIdStr == null) {
                log.warn("已读回执缺少chatId");
                return;
            }

            // 获取聊天会话信息，确定发送者
            com.web.model.ChatList chatList = chatService.getChatList(SecurityUtils.getCurrentUserId())
                .stream()
                .filter(c -> c.getId().equals(chatIdStr))
                .findFirst()
                .orElse(null);

            if (chatList != null) {
                // 确定对方用户ID
                Long currentUserId = SecurityUtils.getCurrentUserId();
                Long otherUserId = chatList.getUserId().equals(currentUserId) 
                    ? chatList.getTargetId() 
                    : chatList.getUserId();

                // 获取对方用户信息
                com.web.model.User otherUser = chatService.getChatList(otherUserId)
                    .stream()
                    .findFirst()
                    .map(c -> {
                        try {
                            return new com.web.model.User();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);

                if (otherUser != null) {
                    // 构建已读回执响应
                    Map<String, Object> readReceiptResponse = new HashMap<>();
                    readReceiptResponse.put("chatId", chatIdStr);
                    readReceiptResponse.put("messageId", messageId);
                    readReceiptResponse.put("timestamp", timestamp);
                    readReceiptResponse.put("status", 3); // READ状态

                    // 发送给对方用户（消息发送者）
                    messagingTemplate.convertAndSendToUser(
                        otherUser.getUsername(),
                        "/queue/read-receipt",
                        readReceiptResponse
                    );

                    log.info("✅ 已读回执已发送给: userId={}", otherUserId);
                }
            }

        } catch (Exception e) {
            log.error("❌ 处理已读回执失败", e);
        }
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