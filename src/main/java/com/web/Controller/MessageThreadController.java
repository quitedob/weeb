package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.MessageThread;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.MessageThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 消息线索控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class MessageThreadController {

    private final MessageThreadService messageThreadService;

    /**
     * 创建消息线索
     */
    @PostMapping
    @PreAuthorize("hasPermission(null, 'MESSAGE_CREATE_OWN')")
    public ResponseEntity<ApiResponse<MessageThread>> createThread(
            @Valid @RequestBody CreateThreadRequest request,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();

        MessageThread thread = messageThreadService.createThread(
            request.getRootMessageId(),
            request.getTitle(),
            userId
        );

        return ResponseEntity.ok(ApiResponse.success(thread));
    }

    /**
     * 获取线索详情
     */
    @GetMapping("/{threadId}")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_THREAD_READ_OWN')")
    public ResponseEntity<ApiResponse<MessageThread>> getThread(@PathVariable Long threadId) {
        MessageThread thread = messageThreadService.getThreadById(threadId);
        return ResponseEntity.ok(ApiResponse.success(thread));
    }

    /**
     * 获取线索中的消息列表
     */
    @GetMapping("/{threadId}/messages")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_THREAD_READ_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getThreadMessages(
            @PathVariable Long threadId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize) {

        Map<String, Object> result = messageThreadService.getThreadMessages(threadId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 回复消息到线索
     */
    @PostMapping("/{threadId}/replies")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_REPLY_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replyToThread(
            @PathVariable Long threadId,
            @Valid @RequestBody ReplyRequest request,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();

        Map<String, Object> result = messageThreadService.replyToThread(
            threadId,
            request.getContent(),
            userId
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 加入线索
     */
    @PostMapping("/{threadId}/join")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_THREAD_JOIN_OWN')")
    public ResponseEntity<ApiResponse<Boolean>> joinThread(
            @PathVariable Long threadId,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        boolean success = messageThreadService.joinThread(threadId, userId);

        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 离开线索
     */
    @DeleteMapping("/{threadId}/leave")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_THREAD_LEAVE_OWN')")
    public ResponseEntity<ApiResponse<Boolean>> leaveThread(
            @PathVariable Long threadId,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        boolean success = messageThreadService.leaveThread(threadId, userId);

        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 归档线索
     */
    @PostMapping("/{threadId}/archive")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_THREAD_ARCHIVE_OWN')")
    public ResponseEntity<ApiResponse<Boolean>> archiveThread(
            @PathVariable Long threadId,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        boolean success = messageThreadService.archiveThread(threadId, userId);

        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 关闭线索
     */
    @PostMapping("/{threadId}/close")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_THREAD_CLOSE_OWN')")
    public ResponseEntity<ApiResponse<Boolean>> closeThread(
            @PathVariable Long threadId,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        boolean success = messageThreadService.closeThread(threadId, userId);

        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 置顶/取消置顶线索
     */
    @PostMapping("/{threadId}/pin")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_THREAD_PIN_OWN')")
    public ResponseEntity<ApiResponse<Boolean>> pinThread(
            @PathVariable Long threadId,
            @RequestParam @NotNull Boolean isPinned,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        boolean success = messageThreadService.pinThread(threadId, userId, isPinned);

        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 锁定/解锁线索
     */
    @PostMapping("/{threadId}/lock")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_THREAD_LOCK_OWN')")
    public ResponseEntity<ApiResponse<Boolean>> lockThread(
            @PathVariable Long threadId,
            @RequestParam @NotNull Boolean isLocked,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        boolean success = messageThreadService.lockThread(threadId, userId, isLocked);

        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 获取用户参与的线索列表
     */
    @GetMapping("/my-threads")
    @PreAuthorize("hasPermission(null, 'MESSAGE_THREAD_READ_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserThreads(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        Map<String, Object> result = messageThreadService.getUserThreads(userId, page, pageSize);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取活跃线索列表
     */
    @GetMapping("/active")
    @PreAuthorize("hasPermission(null, 'MESSAGE_THREAD_READ_ANY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveThreads(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize) {

        Map<String, Object> result = messageThreadService.getActiveThreads(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取用户创建的线索列表
     */
    @GetMapping("/created")
    @PreAuthorize("hasPermission(null, 'MESSAGE_THREAD_READ_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserCreatedThreads(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        Map<String, Object> result = messageThreadService.getUserCreatedThreads(userId, page, pageSize);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 搜索线索
     */
    @GetMapping("/search")
    @PreAuthorize("hasPermission(null, 'MESSAGE_THREAD_READ_ANY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchThreads(
            @RequestParam @NotBlank String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize) {

        Map<String, Object> result = messageThreadService.searchThreads(keyword, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取线索统计信息
     */
    @GetMapping("/{threadId}/statistics")
    @PreAuthorize("hasPermission(#threadId, 'MESSAGE_THREAD_READ_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getThreadStatistics(@PathVariable Long threadId) {
        Map<String, Object> statistics = messageThreadService.getThreadStatistics(threadId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 获取消息的线索上下文
     */
    @GetMapping("/context/{messageId}")
    @PreAuthorize("hasPermission(#messageId, 'MESSAGE_READ_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getThreadContext(
            @PathVariable Long messageId,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        Map<String, Object> context = messageThreadService.getThreadContext(messageId, userId);

        return ResponseEntity.ok(ApiResponse.success(context));
    }

    // 内部类定义

    /**
     * 创建线索请求
     */
    public static class CreateThreadRequest {
        @NotNull(message = "根消息ID不能为空")
        private Long rootMessageId;

        @NotBlank(message = "线索标题不能为空")
        private String title;

        // Getters and Setters
        public Long getRootMessageId() { return rootMessageId; }
        public void setRootMessageId(Long rootMessageId) { this.rootMessageId = rootMessageId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    /**
     * 回复请求
     */
    public static class ReplyRequest {
        @NotBlank(message = "回复内容不能为空")
        private String content;

        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}