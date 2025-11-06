package com.web.Controller;

import com.web.annotation.Userid;
import com.web.annotation.UrlLimit;
import com.web.common.ApiResponse;
import com.web.model.Message;
import com.web.model.ChatList;
import com.web.service.UnifiedMessageService;
import com.web.vo.message.SendMessageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 统一消息控制器
 * 
 * ⚠️ 已废弃 (DEPRECATED) - 2025-11-06
 * 
 * 此Controller的功能已合并到 ChatController (/api/chats)
 * 
 * 迁移指南：
 * - /api/messages/send → /api/chats/{chatId}/messages (POST)
 * - /api/messages/private → /api/chats (POST) + /api/chats/{chatId}/messages (POST)
 * - /api/messages/group → /api/chats/{chatId}/messages (POST)
 * - /api/messages → /api/chats (GET)
 * - /api/messages/chats → /api/chats (GET)
 * - /api/messages/unread/stats → /api/chats/unread/stats (GET)
 * - /api/messages/{messageId}/read → /api/chats/{chatId}/read (POST)
 * - /api/messages/{messageId}/recall → /api/chats/messages/{messageId} (DELETE)
 * 
 * 请更新前端代码使用新的API端点
 * 此Controller将在下一个版本中移除
 */
@Deprecated
@Slf4j
@RestController
@RequestMapping("/api/messages")
public class UnifiedMessageController {

    @Autowired
    private UnifiedMessageService unifiedMessageService;

    /**
     * 统一发送消息接口
     * POST /api/messages/send
     *
     * @param sendMessageVo 消息内容
     * @param userId 发送者ID
     * @return 发送的消息
     */
    @PostMapping("/send")
    @UrlLimit
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @Valid @RequestBody SendMessageVo sendMessageVo,
            @Userid Long userId) {
        try {
            Message message = unifiedMessageService.sendMessage(sendMessageVo, userId);
            return ResponseEntity.ok(ApiResponse.success("消息发送成功", message));
        } catch (Exception e) {
            log.error("发送消息失败: userId={}, targetType={}, targetId={}",
                userId, sendMessageVo.getTargetType(), sendMessageVo.getTargetId(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("发送消息失败: " + e.getMessage()));
        }
    }

    /**
     * 发送私聊消息
     * POST /api/messages/private
     *
     * @param requestBody 请求体 {"targetUserId": 123, "content": "Hello"}
     * @param userId 发送者ID
     * @return 发送的消息
     */
    @PostMapping("/private")
    @UrlLimit
    public ResponseEntity<ApiResponse<Message>> sendPrivateMessage(
            @RequestBody Map<String, Object> requestBody,
            @Userid Long userId) {
        try {
            Long targetUserId = Long.valueOf(requestBody.get("targetUserId").toString());
            String content = requestBody.get("content").toString();

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("消息内容不能为空"));
            }

            Message message = unifiedMessageService.sendPrivateMessage(targetUserId, content, userId);
            return ResponseEntity.ok(ApiResponse.success("私聊消息发送成功", message));
        } catch (Exception e) {
            log.error("发送私聊消息失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("发送私聊消息失败: " + e.getMessage()));
        }
    }

    /**
     * 发送群聊消息
     * POST /api/messages/group
     *
     * @param requestBody 请求体 {"groupId": 123, "content": "Hello group"}
     * @param userId 发送者ID
     * @return 发送的消息
     */
    @PostMapping("/group")
    @UrlLimit
    public ResponseEntity<ApiResponse<Message>> sendGroupMessage(
            @RequestBody Map<String, Object> requestBody,
            @Userid Long userId) {
        try {
            Long groupId = Long.valueOf(requestBody.get("groupId").toString());
            String content = requestBody.get("content").toString();

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("消息内容不能为空"));
            }

            Message message = unifiedMessageService.sendGroupMessage(groupId, content, userId);
            return ResponseEntity.ok(ApiResponse.success("群聊消息发送成功", message));
        } catch (Exception e) {
            log.error("发送群聊消息失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("发送群聊消息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取统一消息列表
     * GET /api/messages?page=1&size=20
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnifiedMessageList(
            @Userid Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> messageList = unifiedMessageService.getUnifiedMessageList(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success(messageList));
        } catch (Exception e) {
            log.error("获取消息列表失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取消息列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取私聊消息历史
     * GET /api/messages/private/{targetUserId}?page=1&size=20
     *
     * @param targetUserId 目标用户ID
     * @param userId 当前用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 私聊消息历史
     */
    @GetMapping("/private/{targetUserId}")
    public ResponseEntity<ApiResponse<List<Message>>> getPrivateMessageHistory(
            @PathVariable Long targetUserId,
            @Userid Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Message> messages = unifiedMessageService.getPrivateMessageHistory(userId, targetUserId, page, size);
            return ResponseEntity.ok(ApiResponse.success(messages));
        } catch (Exception e) {
            log.error("获取私聊消息历史失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取私聊消息历史失败: " + e.getMessage()));
        }
    }

    /**
     * 获取群聊消息历史
     * GET /api/messages/group/{groupId}?page=1&size=20
     *
     * @param groupId 群组ID
     * @param userId 当前用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 群聊消息历史
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<Message>>> getGroupMessageHistory(
            @PathVariable Long groupId,
            @Userid Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Message> messages = unifiedMessageService.getGroupMessageHistory(groupId, userId, page, size);
            return ResponseEntity.ok(ApiResponse.success(messages));
        } catch (Exception e) {
            log.error("获取群聊消息历史失败: userId={}, groupId={}", userId, groupId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取群聊消息历史失败: " + e.getMessage()));
        }
    }

    /**
     * 获取统一聊天列表
     * GET /api/messages/chats
     *
     * @param userId 用户ID
     * @return 聊天列表
     */
    @GetMapping("/chats")
    public ResponseEntity<ApiResponse<List<ChatList>>> getUnifiedChatList(@Userid Long userId) {
        try {
            List<ChatList> chatList = unifiedMessageService.getUnifiedChatList(userId);
            return ResponseEntity.ok(ApiResponse.success(chatList));
        } catch (Exception e) {
            log.error("获取聊天列表失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取聊天列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取未读消息统计
     * GET /api/messages/unread/stats
     *
     * @param userId 用户ID
     * @return 未读消息统计
     */
    @GetMapping("/unread/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadMessageStats(@Userid Long userId) {
        try {
            Map<String, Object> stats = unifiedMessageService.getUnreadMessageStats(userId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取未读消息统计失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取未读消息统计失败: " + e.getMessage()));
        }
    }

    /**
     * 标记消息为已读
     * POST /api/messages/{messageId}/read
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 是否成功
     */
    @PostMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<String>> markMessageAsRead(
            @PathVariable Long messageId,
            @Userid Long userId) {
        try {
            boolean success = unifiedMessageService.markMessageAsRead(messageId, userId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("消息已标记为已读"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("标记消息已读失败"));
            }
        } catch (Exception e) {
            log.error("标记消息已读失败: messageId={}, userId={}", messageId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("标记消息已读失败: " + e.getMessage()));
        }
    }

    /**
     * 标记私聊会话为已读
     * POST /api/messages/private/{targetUserId}/read
     *
     * @param targetUserId 目标用户ID
     * @param userId 当前用户ID
     * @return 是否成功
     */
    @PostMapping("/private/{targetUserId}/read")
    public ResponseEntity<ApiResponse<String>> markPrivateChatAsRead(
            @PathVariable Long targetUserId,
            @Userid Long userId) {
        try {
            boolean success = unifiedMessageService.markPrivateChatAsRead(targetUserId, userId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("私聊已标记为已读"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("标记私聊已读失败"));
            }
        } catch (Exception e) {
            log.error("标记私聊已读失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("标记私聊已读失败: " + e.getMessage()));
        }
    }

    /**
     * 标记群聊为已读
     * POST /api/messages/group/{groupId}/read
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否成功
     */
    @PostMapping("/group/{groupId}/read")
    public ResponseEntity<ApiResponse<String>> markGroupChatAsRead(
            @PathVariable Long groupId,
            @Userid Long userId) {
        try {
            boolean success = unifiedMessageService.markGroupChatAsRead(groupId, userId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("群聊已标记为已读"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("标记群聊已读失败"));
            }
        } catch (Exception e) {
            log.error("标记群聊已读失败: userId={}, groupId={}", userId, groupId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("标记群聊已读失败: " + e.getMessage()));
        }
    }

    /**
     * 删除消息
     * DELETE /api/messages/{messageId}
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 是否成功
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<String>> deleteMessage(
            @PathVariable Long messageId,
            @Userid Long userId) {
        try {
            boolean success = unifiedMessageService.deleteMessage(messageId, userId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("消息删除成功"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("删除消息失败"));
            }
        } catch (Exception e) {
            log.error("删除消息失败: messageId={}, userId={}", messageId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("删除消息失败: " + e.getMessage()));
        }
    }

    /**
     * 撤回消息
     * POST /api/messages/{messageId}/recall
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 是否成功
     */
    @PostMapping("/{messageId}/recall")
    public ResponseEntity<ApiResponse<String>> recallMessage(
            @PathVariable Long messageId,
            @Userid Long userId) {
        try {
            boolean success = unifiedMessageService.recallMessage(messageId, userId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("消息撤回成功"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("撤回消息失败"));
            }
        } catch (Exception e) {
            log.error("撤回消息失败: messageId={}, userId={}", messageId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("撤回消息失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索消息
     * GET /api/messages/search?keyword=hello&page=1&size=20
     *
     * @param keyword 搜索关键词
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchMessages(
            @RequestParam String keyword,
            @Userid Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索关键词不能为空"));
            }

            Map<String, Object> results = unifiedMessageService.searchMessages(userId, keyword, page, size);
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("搜索消息失败: userId={}, keyword={}", userId, keyword, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("搜索消息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取消息详情
     * GET /api/messages/{messageId}
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 消息详情
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Message>> getMessageById(
            @PathVariable Long messageId,
            @Userid Long userId) {
        try {
            Message message = unifiedMessageService.getMessageById(messageId, userId);
            if (message != null) {
                return ResponseEntity.ok(ApiResponse.success(message));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取消息详情失败: messageId={}, userId={}", messageId, userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取消息详情失败: " + e.getMessage()));
        }
    }
}