package com.web.controller;

import com.web.annotation.Userid;
import com.web.annotation.UrlLimit;
import com.web.common.ApiResponse;
import com.web.constant.TextContentType;
import com.web.model.ChatList;
import com.web.model.Message;
import com.web.dto.ChatListDTO;
import com.web.service.ChatService;
import com.web.vo.chat.ChatCreateVo;
import com.web.vo.chat.ChatMessageVo;
import com.web.vo.chat.ChatMessagesVo;
import com.web.vo.message.TextMessageContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天控制器 - 统一聊天API
 * 提供统一的聊天相关功能的REST API接口
 * 整合了原有的MessageController和ChatListController功能
 */
@Slf4j
@RestController
@RequestMapping("/api/chats")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private com.web.service.ChatUnreadCountService chatUnreadCountService;

    /**
     * 获取用户的聊天列表
     * 接口说明：
     *  - 获取当前用户的所有聊天会话（私聊和群聊）
     *
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回聊天列表
     */
    @UrlLimit
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatListDTO>>> getChatList(@Userid Long userId) {
        List<ChatList> chatLists = chatService.getChatList(userId);

        // 转换为DTO格式
        List<ChatListDTO> result = chatLists.stream()
                .map(ChatListDTO::fromEntity)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 创建新的聊天会话
     * 接口说明：
     *  - 根据目标用户ID创建私聊会话
     *  - 如果会话已存在则返回现有会话
     *  - ✅ HARDENED: Added manual validation for targetId to handle malformed input like "2_2"
     *
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @param chatCreateVo 聊天创建请求对象，包含目标用户ID（String类型）
     * @return 返回创建的聊天会话
     */
    @UrlLimit
    @PostMapping
    public ResponseEntity<ApiResponse<ChatList>> createChat(@Userid Long userId,
                                                           @RequestBody @Valid ChatCreateVo chatCreateVo) {
        // ✅ Manual validation: Parse String targetId to Long
        Long targetId;
        try {
            targetId = Long.parseLong(chatCreateVo.getTargetId());
        } catch (NumberFormatException e) {
            log.error("Invalid targetId format received from client. userId={}, input='{}'", 
                     userId, chatCreateVo.getTargetId(), e);
            throw new com.web.exception.WeebException(com.web.common.ApiResponse.ErrorCode.PARAM_ERROR, "目标ID格式无效，必须为数字");
        }
        
        ChatList result = chatService.createChat(userId, targetId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取聊天消息历史记录
     * 接口说明：
     *  - 分页获取指定聊天的消息历史
     *  - ✅ 修复：使用sharedChatId（Long类型）作为路径参数
     *
     * @param sharedChatId 共享聊天ID（Long类型）
     * @param chatMessagesVo 分页参数
     * @return 返回消息列表
     */
    @UrlLimit
    @GetMapping("/{sharedChatId}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getChatMessages(@Userid Long userId, @PathVariable Long sharedChatId,
                                                                     @ModelAttribute @Valid ChatMessagesVo chatMessagesVo) {
        List<Message> result = chatService.getChatMessagesBySharedChatId(userId, sharedChatId, chatMessagesVo.getPage(), chatMessagesVo.getSize());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 发送聊天消息
     * 接口说明：
     *  - 向指定聊天会话发送消息
     *  - ✅ 修复：使用sharedChatId（Long类型）作为路径参数
     *
     * @param sharedChatId 共享聊天ID（Long类型）
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @param chatMessageVo 消息内容
     * @return 返回发送的完整消息对象
     */
    @UrlLimit
    @PostMapping("/{sharedChatId}/messages")
    public ResponseEntity<ApiResponse<Message>> sendMessage(@PathVariable Long sharedChatId,
                                                           @Userid Long userId,
                                                           @RequestBody @Valid ChatMessageVo chatMessageVo) {
        log.info("发送消息请求: sharedChatId={}, userId={}, messageType={}, content类型={}",
                 sharedChatId, userId, chatMessageVo.getMessageType(),
                 chatMessageVo.getContent() != null ? chatMessageVo.getContent().getClass().getSimpleName() : "null");

        Message message = new Message();
        message.setSenderId(userId);
        message.setClientMessageId(chatMessageVo.getClientMessageId());
        message.setReplyToMessageId(chatMessageVo.getReplyToMessageId());
        // ✅ 修复：直接使用sharedChatId
        message.setChatId(sharedChatId);

        // 处理消息内容 - 支持JSON对象或字符串
        TextMessageContent textContent = new TextMessageContent();

        try {
            Object contentObj = chatMessageVo.getContent();
            log.debug("消息内容对象: {}", contentObj);

            if (contentObj instanceof Map) {
                // 如果是Map对象（来自JSON反序列化），解析为TextMessageContent
                @SuppressWarnings("unchecked")
                Map<String, Object> contentMap = (Map<String, Object>) contentObj;

                log.debug("解析Map格式消息内容: {}", contentMap);

                // 安全地获取各个字段，使用默认值
                String contentStr = contentMap.get("content") != null ?
                    String.valueOf(contentMap.get("content")) : "";
                textContent.setContent(contentStr);

                Integer contentType = (Integer) contentMap.get("contentType");
                textContent.setContentType(contentType != null ? contentType : TextContentType.TEXT.getCode());

                String url = (String) contentMap.get("url");
                textContent.setUrl(url);

                @SuppressWarnings("unchecked")
                List<Integer> atUidList = (List<Integer>) contentMap.get("atUidList");
                if (atUidList != null) {
                    textContent.setAtUidList(atUidList);
                }

                log.info("消息内容解析完成: content={}, contentType={}, url={}",
                        contentStr, contentType, url);
            } else {
                // 如果是字符串或其他类型，直接作为内容
                String contentStr = String.valueOf(contentObj);
                textContent.setContent(contentStr);
                textContent.setContentType(TextContentType.TEXT.getCode());
                log.info("字符串消息内容: {}", contentStr);
            }
        } catch (Exception e) {
            // 如果解析失败，使用默认值并记录日志
            log.error("解析消息内容失败，使用默认值: {}", e.getMessage(), e);
            textContent.setContent(String.valueOf(chatMessageVo.getContent()));
            textContent.setContentType(TextContentType.TEXT.getCode());
        }

        message.setContent(textContent);
        message.setMessageType(chatMessageVo.getMessageType() != null ? chatMessageVo.getMessageType() : 1);

        try {
            Message result = chatService.sendMessageBySharedChatId(userId, sharedChatId, message);
            log.info("消息发送成功: messageId={}", result != null ? result.getId() : "null");
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("消息发送失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("消息发送失败: " + e.getMessage()));
        }
    }

    /**
     * 标记消息为已读
     * 接口说明：
     *  - 将指定聊天的未读消息数清零
     *  - ✅ 修复：使用sharedChatId（Long类型）作为路径参数
     *
     * @param sharedChatId 共享聊天ID（Long类型）
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回操作结果
     */
    @UrlLimit
    @PostMapping("/{sharedChatId}/read")
    public ResponseEntity<ApiResponse<?>> markAsRead(@PathVariable Long sharedChatId,
            @Userid Long userId, @RequestBody(required = false) @Valid com.web.vo.chat.ReadMessageVo request) {
        if (request == null) {
            return ResponseEntity.ok(ApiResponse.success(chatService.markAsReadBySharedChatId(userId, sharedChatId)));
        }
        return ResponseEntity.ok(ApiResponse.success(chatService.markAsReadBySharedChatId(userId, sharedChatId,
                request.getLastReadMessageId())));
    }

    @GetMapping("/{sharedChatId}/sync")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> syncMessages(@PathVariable Long sharedChatId,
            @Userid Long userId, @RequestParam(defaultValue = "0") Long afterMessageId,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(ApiResponse.success(chatService.syncMessages(userId, sharedChatId, afterMessageId, size)));
    }

    @GetMapping("/{sharedChatId}/messages/state")
    public ResponseEntity<ApiResponse<List<Message>>> getMessageStates(@PathVariable Long sharedChatId,
            @Userid Long userId, @RequestParam List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessageStates(userId, sharedChatId, ids)));
    }

    /**
     * 删除聊天会话
     * 接口说明：
     *  - 删除指定的聊天会话
     *  - ✅ 修复：使用sharedChatId（Long类型）作为路径参数
     *
     * @param sharedChatId 共享聊天ID（Long类型）
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回删除结果
     */
    @UrlLimit
    @DeleteMapping("/{sharedChatId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteChat(@PathVariable Long sharedChatId,
                                                          @Userid Long userId) {
        boolean result = chatService.deleteChatBySharedChatId(userId, sharedChatId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 对消息添加反应
     * 接口说明：
     *  - 对指定消息添加或取消表情反应
     *
     * @param messageId 消息ID
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @param reactionType 反应类型（如👍、❤️等）
     * @return 返回操作结果
     */
    @UrlLimit
    @PostMapping("/messages/{messageId}/react")
    public ResponseEntity<ApiResponse<String>> addReaction(@PathVariable Long messageId,
                                                          @Userid Long userId,
                                                          @RequestParam String reactionType) {
        chatService.addReaction(userId, messageId, reactionType);
        return ResponseEntity.ok(ApiResponse.success("反应操作成功"));
    }

    @PutMapping("/messages/{messageId}/react")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> ensureReaction(@PathVariable Long messageId,
            @Userid Long userId, @RequestParam String reactionType) {
        return ResponseEntity.ok(ApiResponse.success(chatService.setReaction(userId, messageId, reactionType, true)));
    }

    @DeleteMapping("/messages/{messageId}/react")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> removeReaction(@PathVariable Long messageId,
            @Userid Long userId, @RequestParam String reactionType) {
        return ResponseEntity.ok(ApiResponse.success(chatService.setReaction(userId, messageId, reactionType, false)));
    }

    /**
     * 撤回消息
     * 接口说明：
     *  - 撤回指定的消息（仅限发送者，且发送时间不超过2分钟）
     *
     * @param messageId 消息ID
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回撤回结果
     */
    @UrlLimit
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Boolean>> recallMessage(@PathVariable Long messageId,
                                                             @Userid Long userId) {
        boolean result = chatService.recallMessage(userId, messageId);
        return ResponseEntity.ok(ApiResponse.success("消息撤回成功", result));
    }

    @Autowired
    private com.web.service.UserOnlineStatusService onlineStatusService;

    @Autowired
    private com.web.service.UserPreferencesService preferencesService;

    /**
     * 获取在线用户列表
     * 接口说明：
     *  - 获取当前所有在线用户的ID列表
     *
     * @return 返回在线用户ID集合
     */
    @UrlLimit
    @GetMapping("/online-users")
    public ResponseEntity<ApiResponse<java.util.Set<Object>>> getOnlineUsers() {
        java.util.Set<Object> onlineUsers = onlineStatusService.getOnlineUsers().stream()
                .filter(id -> preferencesService.isOnlineVisible(Long.valueOf(id.toString())))
                .collect(java.util.stream.Collectors.toSet());
        return ResponseEntity.ok(ApiResponse.success(onlineUsers));
    }

    /**
     * 检查用户是否在线
     * 接口说明：
     *  - 检查指定用户是否在线
     *
     * @param targetUserId 目标用户ID
     * @return 返回在线状态
     */
    @UrlLimit
    @GetMapping("/users/{targetUserId}/online")
    public ResponseEntity<ApiResponse<Boolean>> checkUserOnline(@PathVariable Long targetUserId) {
        boolean isOnline = preferencesService.isOnlineVisible(targetUserId) && onlineStatusService.isUserOnline(targetUserId);
        return ResponseEntity.ok(ApiResponse.success(isOnline));
    }

    // ==================== 未读计数相关API ====================

    /**
     * 获取未读消息统计
     * 接口说明：
     *  - 获取用户的未读消息总数和详细列表
     *
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回未读统计信息
     */
    @UrlLimit
    @GetMapping("/unread/stats")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getUnreadStats(@Userid Long userId) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        // 获取总未读数
        int totalUnread = chatUnreadCountService.getTotalUnreadCount(userId);
        stats.put("totalUnread", totalUnread);
        
        // 获取未读列表
        java.util.List<java.util.Map<String, Object>> unreadList = chatUnreadCountService.getUnreadCountList(userId);
        stats.put("unreadList", unreadList);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取单个聊天的未读数
     * 接口说明：
     *  - 获取指定聊天的未读消息数
     *
     * @param chatId 聊天ID（String类型，因为chat_list.id是VARCHAR）
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回未读数
     */
    @UrlLimit
    @GetMapping("/{chatId}/unread")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(@PathVariable String chatId,
                                                               @Userid Long userId) {
        // ✅ 需要将String chatId转换为sharedChatId
        List<ChatList> chatLists = chatService.getChatList(userId);
        ChatList targetChat = chatLists.stream()
                .filter(c -> c.getId().equals(chatId))
                .findFirst()
                .orElseThrow(() -> new com.web.exception.WeebException("聊天会话不存在"));
        
        Long sharedChatId = targetChat.getSharedChatId();
        if (sharedChatId == null) {
            throw new com.web.exception.WeebException("聊天会话配置错误：缺少共享聊天ID");
        }
        
        int unreadCount = chatUnreadCountService.getUnreadCount(userId, sharedChatId);
        return ResponseEntity.ok(ApiResponse.success(unreadCount));
    }

    /**
     * 批量标记已读
     * 接口说明：
     *  - 批量将多个聊天标记为已读
     *
     * @param chatIds 聊天ID列表（String类型，因为chat_list.id是VARCHAR）
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回操作结果
     */
    @UrlLimit
    @PostMapping("/read/batch")
    public ResponseEntity<ApiResponse<String>> batchMarkAsRead(@RequestBody java.util.List<String> chatIds,
                                                               @Userid Long userId) {
        // ✅ 将String类型的chatId转换为Long类型的sharedChatId
        java.util.List<Long> sharedChatIds = new java.util.ArrayList<>();
        List<ChatList> userChats = chatService.getChatList(userId);
        
        for (String chatId : chatIds) {
            ChatList targetChat = userChats.stream()
                    .filter(c -> c.getId().equals(chatId))
                    .findFirst()
                    .orElse(null);
            
            if (targetChat != null && targetChat.getSharedChatId() != null) {
                sharedChatIds.add(targetChat.getSharedChatId());
            }
        }
        
        if (!sharedChatIds.isEmpty()) {
            chatUnreadCountService.batchMarkAsRead(userId, sharedChatIds);
        }
        
        return ResponseEntity.ok(ApiResponse.success("批量标记已读成功"));
    }

    /**
     * 获取群组未读数（优化版）
     * 接口说明：
     *  - 获取群组聊天的未读消息数（仅追踪最后已读消息ID）
     *
     * @param groupId 群组ID
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回未读数
     */
    @UrlLimit
    @GetMapping("/groups/{groupId}/unread")
    public ResponseEntity<ApiResponse<Integer>> getGroupUnreadCount(@PathVariable Long groupId,
                                                                    @Userid Long userId) {
        int unreadCount = chatUnreadCountService.getGroupUnreadCount(userId, groupId);
        return ResponseEntity.ok(ApiResponse.success(unreadCount));
    }
}
