package com.web.Controller;

import com.web.annotation.UrlLimit;
import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.Message;
import com.web.service.MessageService;
import com.web.vo.message.SendMessageVo;
import com.web.vo.message.TextMessageContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

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
                                                             @RequestBody @Valid RecordRequest recordReq) { // 从请求体中获取并校验 RecordRequest 对象
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
                                                       @RequestBody @Valid RecallRequest recallReq) { // 从请求体中获取并校验 RecallRequest 对象
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
    public ResponseEntity<ApiResponse<String>> handleReaction(@RequestBody @Valid com.web.vo.message.ReactionVo reactionVo, @Userid Long userId) {
        messageService.handleReaction(reactionVo, userId);
        return ResponseEntity.ok(ApiResponse.success("反应操作成功"));
    }

    /**
     * 聊天记录请求体类，用于获取聊天记录时接收请求数据
     */
    public static class RecordRequest {
        @NotNull(message = "目标ID不能为空")
        private Long targetId; // 目标聊天对象的ID

        private int index; // 分页查询的起始索引

        @jakarta.validation.constraints.Max(value = 100, message = "查询条数不能超过100条")
        private int num; // 查询条数

        public Long getTargetId() {
            return targetId;
        }

        public void setTargetId(Long targetId) {
            this.targetId = targetId;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getNum() {
            return num;
        }

        public void setNum(int num) {
            this.num = num;
        }
    }

    /**
     * 撤回消息请求体类，用于撤回消息时接收请求数据
     */
    public static class RecallRequest {
        @NotNull(message = "消息ID不能为空")
        private Long msgId; // 要撤回的消息ID

        public Long getMsgId() {
            return msgId;
        }

        public void setMsgId(Long msgId) {
            this.msgId = msgId;
        }
    }
}
