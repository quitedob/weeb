package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.ChatList;
import com.web.service.ChatListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @deprecated 此Controller已废弃，请使用 ChatController 替代
 * 所有聊天相关功能已统一到 ChatController 中
 */
@Deprecated
@RestController
@RequestMapping("/api/v1/chat-list")
public class ChatListController {

    @Resource
    private ChatListService chatListService;

    /**
     * 获取用户的私聊列表
     *
     * @param userId 用户ID
     * @return 私聊列表
     */
    @GetMapping("/list/private")
    public ResponseEntity<ApiResponse<List<ChatList>>> privateList(@RequestParam @NotNull Long userId) {
        List<ChatList> result = chatListService.getPrivateChatList(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取用户的群聊记录
     *
     * @param userId 用户ID
     * @return 群聊记录
     */
    @GetMapping("/group")
    public ResponseEntity<ApiResponse<ChatList>> group(@RequestParam @NotNull Long userId) {
        ChatList result = chatListService.getOrCreateGroupChat(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 创建私聊记录
     *
     * @param userId   用户ID
     * @param targetId 目标用户ID
     * @return 创建的聊天记录
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ChatList>> create(@RequestParam @NotNull Long userId, @RequestParam @NotNull Long targetId) {
        ChatList result = chatListService.createPrivateChat(userId, targetId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 将未读消息数清零
     *
     * @param userId   用户ID
     * @param targetId 目标用户ID
     * @return 操作结果
     */
    @PostMapping("/read")
    public ResponseEntity<ApiResponse<Boolean>> read(@RequestParam @NotNull Long userId, @RequestParam @NotNull Long targetId) {
        boolean result = chatListService.resetUnreadCount(userId, targetId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除聊天记录
     *
     * @param userId     用户ID
     * @param chatListId 聊天记录ID
     * @return 删除结果
     */
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Boolean>> delete(@RequestParam @NotNull Long userId, @RequestParam @NotNull Long chatListId) {
        boolean result = chatListService.deleteChatRecord(userId, chatListId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
