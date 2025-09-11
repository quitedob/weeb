package com.web.Controller; // 修改包名为全小写（原包名为 com.web.Controller）

// 导入所需依赖类
import com.web.annotation.UrlLimit;          // 导入自定义注解，用于限制请求频率
import com.web.annotation.UserIp;            // 导入自定义注解，用于获取用户IP地址
import com.web.annotation.Userid;            // 导入自定义注解，用于获取用户ID
import com.web.constant.MessageType;         // 导入消息类型常量类，包含默认消息类型（注意：MessageType.Text 应为 String 类型）
import com.web.model.Message;                // 导入消息实体类，参考 model.Message 类
import com.web.service.MessageService;       // 导入消息服务，用于处理业务逻辑
import com.web.util.ResultUtil;              // 导入结果工具类，用于封装返回结果
import org.springframework.web.bind.annotation.PostMapping; // 导入处理POST请求的注解
import org.springframework.web.bind.annotation.RequestBody; // 导入将请求体转换为对象的注解
import org.springframework.web.bind.annotation.RequestMapping; // 导入请求映射注解
import org.springframework.web.bind.annotation.RestController; // 导入REST风格控制器注解

import jakarta.annotation.Resource;            // 导入资源注入注解
import jakarta.validation.Valid;               // 导入校验注解，用于校验请求体数据
import jakarta.validation.constraints.Max;     // 导入最大值校验注解
import jakarta.validation.constraints.NotNull; // 导入非空校验注解
import java.util.List;                       // 导入List集合类
import com.web.vo.message.ReactionVo;      // Import for the new VO
import com.web.common.ApiResponse;
import com.web.vo.message.RecallVo;
import com.web.vo.message.SendMessageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 消息控制器
 * 提供消息发送、撤回、反应等功能的REST API接口
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 发送消息接口
     * 接口说明：
     *  - 通过 @Userid 注解自动获取当前用户ID
     *  - 从请求体 SendMessageVo 中获取目标用户ID和消息内容
     *
     * @param userId     当前用户ID，由注解 @Userid 提供，类型为 Long
     * @param messageBody 请求体，包含目标ID及消息内容
     * @return 返回发送结果，成功时返回发送后的消息对象，否则返回错误信息
     */
    @UrlLimit // 使用默认的请求频率限制
    @PostMapping("/send") // 将该方法映射到 POST 请求 /send 上
    public ResponseEntity<ApiResponse<Message>> send(@Userid Long userId,             // 通过 @Userid 注解获取当前用户ID
                                                    @RequestBody @Valid Message messageBody) { // 从请求体中获取并校验 Message 对象
        // 设置发送者ID到消息对象中
        messageBody.setSenderId(userId);
        
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
     * @param userId 用户ID (from @Userid annotation)
     * @return 操作结果
     * 简化注释：处理消息反应
     */
    @PostMapping("/react") // Assuming POST is appropriate
    public ResponseEntity<ApiResponse<String>> handleReaction(@RequestBody @Valid ReactionVo reactionVo, @Userid Long userId) { // Changed userId to Long
        // The @Userid annotation in the existing controller provides Long.
        // Service layer now also expects Long.
        messageService.handleReaction(reactionVo, userId);
        return ResponseEntity.ok(ApiResponse.success("反应操作成功"));
    }

    /**
     * 聊天记录请求体类，用于获取聊天记录时接收请求数据
     */
    public static class RecordRequest {
        @NotNull(message = "目标ID不能为空~") // 校验目标ID不能为空
        private Long targetId; // 目标聊天对象的ID

        private int index; // 分页查询的起始索引

        @Max(value = 100, message = "查询条数不能超过100条") // 限制查询条数最大值为100
        private int num; // 查询条数

        // 以下为 RecordRequest 的 Getter 和 Setter 方法

        public Long getTargetId() {
            return targetId; // 返回目标ID
        }

        public void setTargetId(Long targetId) {
            this.targetId = targetId; // 设置目标ID
        }

        public int getIndex() {
            return index; // 返回起始索引
        }

        public void setIndex(int index) {
            this.index = index; // 设置起始索引
        }

        public int getNum() {
            return num; // 返回查询条数
        }

        public void setNum(int num) {
            this.num = num; // 设置查询条数
        }
    }

    /**
     * 撤回消息请求体类，用于撤回消息时接收请求数据
     */
    public static class RecallRequest {
        @NotNull(message = "消息ID不能为空~") // 校验消息ID不能为空
        private Long msgId; // 要撤回的消息ID

        // 以下为 RecallRequest 的 Getter 和 Setter 方法

        public Long getMsgId() {
            return msgId; // 返回消息ID
        }

        public void setMsgId(Long msgId) {
            this.msgId = msgId; // 设置消息ID
        }
    }
}
