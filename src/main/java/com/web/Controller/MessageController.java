package com.web.Controller;

import com.web.annotation.UrlLimit;
import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.Message;
import com.web.service.MessageService;
import com.web.vo.message.SendMessageVo;
import com.web.vo.message.RecordRequestVo;
import com.web.vo.message.RecallRequestVo;
import com.web.vo.message.ReactionVo;
import com.web.vo.message.TextMessageContent;
import com.web.vo.message.ThreadCreationRequestVo;
import com.web.vo.message.ThreadReplyRequestVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 * 提供消息发送、撤回、反应等功能的REST API接口
 */
@RestController
@RequestMapping("/api/v1/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 发送消息接口
     * 接口说明：
     *  - 通过 @Userid 注解自动获取当前用户ID
     *  - 从请求体 Message 中获取目标用户ID和消息内容
     *
     * @param userId     当前用户ID，由注解 @Userid 提供，类型为 Long
     * @param messageBody 请求体，包含目标ID及消息内容
     * @return 返回发送结果，成功时返回发送后的消息对象，否则返回错误信息
     */
    @UrlLimit // 使用默认的请求频率限制
    @PostMapping("/send") // 将该方法映射到 POST 请求 /send 上
    public ResponseEntity<ApiResponse<Message>> send(@Userid Long userId,             // 通过 @Userid 注解获取当前用户ID
                                                    @RequestBody @Valid SendMessageVo messageVo) { // 从请求体中获取并校验 SendMessageVo 对象
        
        // 将VO转换为Message实体
        Message messageBody = new Message();
        messageBody.setSenderId(userId); // 设置发送者ID
        messageBody.setMessageType(messageVo.getMessageType());

        // 将SendMessageVo的content转换为TextMessageContent
        TextMessageContent textContent = new TextMessageContent();
        if (messageVo.getContent() instanceof String) {
            textContent.setContent((String) messageVo.getContent());
        } else {
            // 如果不是字符串类型，转换为字符串
            textContent.setContent(messageVo.getContent().toString());
        }
        messageBody.setContent(textContent);

        // 根据消息类型设置chatId
        if (messageVo.getMessageType() != null && messageVo.getMessageType() == 1) {
            // 群组消息，使用groupId作为chatId
            messageBody.setChatId(messageVo.getGroupId());
        } else {
            // 私聊消息，使用targetId作为chatId
            messageBody.setChatId(messageVo.getTargetId());
        }

        // 设置是否显示时间
        messageBody.setIsShowTime(messageVo.getShowTime() != null ? (messageVo.getShowTime() ? 1 : 0) : 0);

        // 如果消息类型为空，则设置默认消息类型为文本类型
        if (messageBody.getMessageType() == null) {
            messageBody.setMessageType(1); // 将默认消息类型设置为文本类型
        }

        // 调用消息服务进行发送消息操作，并接收返回结果
        Message result = messageService.send(userId, messageBody); // 调用业务逻辑层的发送方法

        // 如果返回结果为空，表示发送失败，返回错误提示
        if (result == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "发送消息失败"));
        }
        // 返回发送成功后的消息对象
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取聊天记录接口
     * 接口说明：
     *  - 通过 @Userid 注解自动获取当前用户ID
     *  - 从请求体 RecordRequest 中获取目标用户ID、起始索引和查询条数
     *
     * @param userId    当前用户ID，由注解 @Userid 提供，类型为 Long
     * @param recordReq 请求体，包含目标ID及分页信息
     * @return 返回聊天记录列表
     */
    @UrlLimit // 使用默认的请求频率限制
    @PostMapping("/record") // 将该方法映射到 POST 请求 /record 上
    public ResponseEntity<ApiResponse<List<Message>>> record(@Userid Long userId,             // 通过 @Userid 注解获取当前用户ID
                                                             @RequestBody @Valid RecordRequestVo recordReq) { // 从请求体中获取并校验 RecordRequestVo 对象
        // 调用消息服务获取当前用户与目标用户的聊天记录
        List<Message> result = messageService.record(userId,
                recordReq.getTargetId(),
                recordReq.getIndex(),
                recordReq.getNum());
        // 返回获取到的聊天记录列表
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 撤回消息接口
     * 接口说明：
     *  - 通过 @Userid 注解自动获取当前用户ID
     *  - 从请求体 RecallRequest 中获取要撤回的消息ID
     *
     * @param userId    当前用户ID，由注解 @Userid 提供，类型为 Long
     * @param recallReq 请求体，包含需要撤回的消息ID
     * @return 返回撤回结果，成功时返回撤回后的消息对象，否则返回错误信息
     */
    @UrlLimit // 使用默认的请求频率限制
    @PostMapping("/recall") // 将该方法映射到 POST 请求 /recall 上
    public ResponseEntity<ApiResponse<Message>> recall(@Userid Long userId,             // 通过 @Userid 注解获取当前用户ID
                                                       @RequestBody @Valid RecallRequestVo recallReq) { // 从请求体中获取并校验 RecallRequestVo 对象
        // 调用消息服务撤回消息，并获取操作后的结果
        Message result = messageService.recall(userId, recallReq.getMsgId()); // 调用撤回消息的业务逻辑方法

        // 如果返回结果为空，表示撤回失败，返回错误提示
        if (result == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "撤回消息失败"));
        }
        // 返回撤回成功后的消息对象
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 对消息添加或取消Emoji反应
     * @param reactionVo 反应视图对象
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/react")
    public ResponseEntity<ApiResponse<String>> handleReaction(@RequestBody @Valid ReactionVo reactionVo, @Userid Long userId) {
        messageService.handleReaction(reactionVo, userId);
        return ResponseEntity.ok(ApiResponse.success("反应操作成功"));
    }

    // ==================== 消息线程相关API ====================

    /**
     * 创建消息线程
     * @param threadRequest 线程创建请求
     * @param userId 用户ID
     * @return 创建结果
     */
    @PostMapping("/thread/create")
    public ResponseEntity<ApiResponse<Message>> createThread(@RequestBody @Valid ThreadCreationRequestVo threadRequest, @Userid Long userId) {
        try {
            Long parentMessageId = threadRequest.getParentMessageId();
            String content = threadRequest.getContent();
            String contentType = threadRequest.getContentType();
            String mentions = threadRequest.getMentions();
            Object attachments = threadRequest.getAttachments();

            Message threadMessage = messageService.createThread(userId, parentMessageId, content);
            return ResponseEntity.ok(ApiResponse.success(threadMessage));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "创建线程失败: " + e.getMessage()));
        }
    }

    /**
     * 获取消息线程列表
     * @param parentMessageId 父消息ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 线程消息列表
     */
    @GetMapping("/thread/{parentMessageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getThreadMessages(
            @PathVariable Long parentMessageId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Map<String, Object> result = messageService.getThreadMessages(parentMessageId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取消息的所有线程摘要
     * @param parentMessageId 父消息ID
     * @return 线程摘要信息
     */
    @GetMapping("/thread/{parentMessageId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getThreadSummary(@PathVariable Long parentMessageId) {
        Map<String, Object> summary = messageService.getThreadSummary(parentMessageId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * 在线程中回复消息
     * @param threadId 线程ID
     * @param replyRequest 回复请求
     * @param userId 用户ID
     * @return 回复结果
     */
    @PostMapping("/thread/{threadId}/reply")
    public ResponseEntity<ApiResponse<Message>> replyToThread(
            @PathVariable Long threadId,
            @RequestBody @Valid ThreadReplyRequestVo replyRequest,
            @Userid Long userId) {

        try {
            String content = replyRequest.getContent();
            String contentType = replyRequest.getContentType();
            String mentions = replyRequest.getMentions();
            Object attachments = replyRequest.getAttachments();
            Boolean silent = replyRequest.getSilent();

            Message replyMessage = messageService.replyToThread(userId, threadId, content);
            return ResponseEntity.ok(ApiResponse.success(replyMessage));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "回复线程失败: " + e.getMessage()));
        }
    }

    /**
     * 删除消息线程
     * @param threadId 线程ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/thread/{threadId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteThread(@PathVariable Long threadId, @Userid Long userId) {
        try {
            boolean deleted = messageService.deleteThread(threadId, userId);
            return ResponseEntity.ok(ApiResponse.success(deleted));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "删除线程失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户参与的线程列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户线程列表
     */
    @GetMapping("/thread/user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserThreads(
            @Userid Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Map<String, Object> result = messageService.getUserThreads(userId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}
