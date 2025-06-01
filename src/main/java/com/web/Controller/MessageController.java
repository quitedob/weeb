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

import javax.annotation.Resource;            // 导入资源注入注解
import javax.validation.Valid;               // 导入校验注解，用于校验请求体数据
import javax.validation.constraints.Max;     // 导入最大值校验注解
import javax.validation.constraints.NotNull; // 导入非空校验注解
import java.util.List;                       // 导入List集合类

/**
 * 消息控制器，处理消息的发送、记录获取和撤回操作
 */
@RestController // 标记该类为REST风格的控制器
@RequestMapping("/api/v1/message") // 设置该控制器的请求根路径
public class MessageController {

    @Resource // 注入消息服务实现类
    private MessageService messageService; // 消息服务对象，用于调用消息相关的业务逻辑

    /**
     * 发送消息接口
     * 接口说明：
     *  - 通过 @Userid 注解自动获取当前用户ID
     *  - 通过 @UserIp 注解自动获取当前用户IP
     *  - 通过 @RequestBody 获取请求体中的 Message 对象，内容包括消息内容、目标ID等
     *
     * @param userId      当前用户ID，由注解 @Userid 提供，类型为 Long
     * @param userIp      当前用户IP，由注解 @UserIp 提供
     * @param messageBody 请求体，包含消息内容、目标等信息，类型为 Message
     * @return 返回发送结果，成功时返回消息对象，否则返回错误信息
     */
    @UrlLimit(maxRequests = 100) // 限制每个用户每分钟最多请求100次
    @PostMapping("/send") // 将该方法映射到 POST 请求 /send 上
    public Object send(@Userid Long userId,        // 通过 @Userid 注解获取用户ID
                       @UserIp String userIp,        // 通过 @UserIp 注解获取用户IP地址
                       @RequestBody @Valid Message messageBody) { // 从请求体中获取并校验 Message 对象
        // 设置发送者ID到消息对象中
        messageBody.setSenderId(userId); // 将当前用户ID赋值给消息的发送者ID

        // 设置发送者IP到消息对象中
        messageBody.setUserIp(userIp); // 将当前用户IP赋值给消息的用户IP字段

        // 如果消息类型为空，则设置默认消息类型为文本类型
        if (messageBody.getMsgType() == null) {
            // 修改说明：此处确保设置的消息类型为 String 类型，与 model.Message 中 msgType 属性保持一致
            messageBody.setMsgType(MessageType.Text); // 将默认消息类型设置为文本类型
        }

        // 调用消息服务进行发送消息操作，并接收返回结果
        Message result = messageService.send(userId, messageBody); // 调用业务逻辑层的发送方法

        // 如果返回结果为空，表示发送失败，返回错误提示
        if (result == null) {
            return ResultUtil.Fail("发送消息失败~"); // 返回发送失败信息
        }
        // 返回发送成功后的消息对象
        return ResultUtil.Succeed(result); // 返回封装成功信息的结果对象
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
    public Object record(@Userid Long userId,             // 通过 @Userid 注解获取当前用户ID
                         @RequestBody @Valid RecordRequest recordReq) { // 从请求体中获取并校验 RecordRequest 对象
        // 调用消息服务获取当前用户与目标用户的聊天记录
        List<Message> result = messageService.record(userId,
                recordReq.getTargetId(),
                recordReq.getIndex(),
                recordReq.getNum());
        // 返回获取到的聊天记录列表
        return ResultUtil.Succeed(result); // 封装成功结果并返回
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
    public Object recall(@Userid Long userId,             // 通过 @Userid 注解获取当前用户ID
                         @RequestBody @Valid RecallRequest recallReq) { // 从请求体中获取并校验 RecallRequest 对象
        // 调用消息服务撤回消息，并获取操作后的结果
        Message result = messageService.recall(userId, recallReq.getMsgId()); // 调用撤回消息的业务逻辑方法

        // 如果返回结果为空，表示撤回失败，返回错误提示
        if (result == null) {
            return ResultUtil.Fail("撤回消息失败~"); // 返回撤回失败信息
        }
        // 返回撤回成功后的消息对象
        return ResultUtil.Succeed(result); // 封装成功结果并返回
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
