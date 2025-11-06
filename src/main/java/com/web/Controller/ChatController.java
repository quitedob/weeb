package com.web.Controller;

import com.web.annotation.Userid;
import com.web.annotation.UrlLimit;
import com.web.common.ApiResponse;
import com.web.model.ChatList;
import com.web.model.Message;
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

/**
 * 聊天控制器 - 统一聊天API
 * 提供统一的聊天相关功能的REST API接口
 * 整合了原有的MessageController和ChatListController功能
 */
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
    public ResponseEntity<ApiResponse<List<ChatList>>> getChatList(@Userid Long userId) {
        List<ChatList> result = chatService.getChatList(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 创建新的聊天会话
     * 接口说明：
     *  - 根据目标用户ID创建私聊会话
     *  - 如果会话已存在则返回现有会话
     *
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @param chatCreateVo 聊天创建请求对象，包含目标用户ID
     * @return 返回创建的聊天会话
     */
    @UrlLimit
    @PostMapping
    public ResponseEntity<ApiResponse<ChatList>> createChat(@Userid Long userId,
                                                           @RequestBody @Valid ChatCreateVo chatCreateVo) {
        ChatList result = chatService.createChat(userId, chatCreateVo.getTargetId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取聊天消息历史记录
     * 接口说明：
     *  - 分页获取指定聊天的消息历史
     *
     * @param chatId 聊天ID
     * @param chatMessagesVo 分页参数
     * @return 返回消息列表
     */
    @UrlLimit
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getChatMessages(@PathVariable Long chatId,
                                                                     @ModelAttribute @Valid ChatMessagesVo chatMessagesVo) {
        List<Message> result = chatService.getChatMessages(chatId, chatMessagesVo.getPage(), chatMessagesVo.getSize());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 发送聊天消息
     * 接口说明：
     *  - 向指定聊天会话发送消息
     *
     * @param chatId 聊天ID
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @param chatMessageVo 消息内容
     * @return 返回发送的完整消息对象
     */
    @UrlLimit
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ApiResponse<Message>> sendMessage(@PathVariable Long chatId,
                                                           @Userid Long userId,
                                                           @RequestBody @Valid ChatMessageVo chatMessageVo) {
        Message message = new Message();
        message.setSenderId(userId);
        message.setChatId(chatId);

        // Convert Object content to TextMessageContent
        TextMessageContent textContent = new TextMessageContent();
        textContent.setContent((String) chatMessageVo.getContent());
        message.setContent(textContent);

        message.setMessageType(chatMessageVo.getMessageType() != null ? chatMessageVo.getMessageType() : 1);

        Message result = chatService.sendMessage(userId, message);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 标记消息为已读
     * 接口说明：
     *  - 将指定聊天的未读消息数清零
     *
     * @param chatId 聊天ID
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回操作结果
     */
    @UrlLimit
    @PostMapping("/{chatId}/read")
    public ResponseEntity<ApiResponse<Boolean>> markAsRead(@PathVariable Long chatId,
                                                          @Userid Long userId) {
        boolean result = chatService.markAsRead(userId, chatId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除聊天会话
     * 接口说明：
     *  - 删除指定的聊天会话
     *
     * @param chatId 聊天ID
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回删除结果
     */
    @UrlLimit
    @DeleteMapping("/{chatId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteChat(@PathVariable Long chatId,
                                                          @Userid Long userId) {
        boolean result = chatService.deleteChat(userId, chatId);
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
        java.util.Set<Object> onlineUsers = onlineStatusService.getOnlineUsers();
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
        boolean isOnline = onlineStatusService.isUserOnline(targetUserId);
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
     * @param chatId 聊天ID
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回未读数
     */
    @UrlLimit
    @GetMapping("/{chatId}/unread")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(@PathVariable Long chatId,
                                                               @Userid Long userId) {
        int unreadCount = chatUnreadCountService.getUnreadCount(userId, chatId);
        return ResponseEntity.ok(ApiResponse.success(unreadCount));
    }

    /**
     * 批量标记已读
     * 接口说明：
     *  - 批量将多个聊天标记为已读
     *
     * @param chatIds 聊天ID列表
     * @param userId 当前用户ID，由注解 @Userid 提供
     * @return 返回操作结果
     */
    @UrlLimit
    @PostMapping("/read/batch")
    public ResponseEntity<ApiResponse<String>> batchMarkAsRead(@RequestBody java.util.List<Long> chatIds,
                                                               @Userid Long userId) {
        chatUnreadCountService.batchMarkAsRead(userId, chatIds);
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