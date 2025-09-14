package com.web.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.web.constant.MessageSource;
import com.web.constant.MessageType;
import com.web.constant.TextContentType;
import com.web.model.User;
import com.web.model.Message;
import com.web.service.AuthService;
import com.web.service.DoubaoAiService;
import com.web.vo.message.TextMessageContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * AiChatService 类，用于处理 AI 机器人回复
 */
@Service
@Slf4j
public class AiChatService {

    @Resource
    @Lazy
    private AuthService userService;

    @Resource
    @Lazy
    private MessageService messageService;

    @Resource
    private DoubaoAiService doubaoAiService;

    /**
     * 发送机器人回复
     *
     * @param userId    当前用户ID，类型为 Long
     * @param targetId  目标聊天对象ID，类型为 Long
     * @param botUser   机器人用户对象，类型为 User
     * @param content   机器人回复的内容，类型为 String
     */
    @Async("taskExecutor")
    public void sendBotReply(Long userId, Long targetId, User botUser, String content) {
        // 获取当前用户信息
        User user = userService.getUserByIdForTalk(userId); // 确保该方法存在并正确实现
        if (user == null) {
            log.warn("用户的id {} 找不到.", userId);
            return;
        }

        // 创建 @ 用户的内容
        TextMessageContent atUser = new TextMessageContent();
        atUser.setContentType(TextContentType.TEXT.getCode());
        JSONConfig config = new JSONConfig().setIgnoreNullValue(true);
        atUser.setContent(JSONUtil.toJsonStr(user, config));

        // 获取 AI 回复
        String aiReply = doubaoAiService.ask(userId, content); // 修正为传递 Long 类型的 userId
        TextMessageContent msgText = new TextMessageContent();
        msgText.setContentType(TextContentType.TEXT.getCode());
        msgText.setContent(aiReply);

        // 合并消息内容
        JSONArray msgContentArray = new JSONArray();
        msgContentArray.add(atUser);
        msgContentArray.add(msgText);
        String msgContentJson = msgContentArray.toJSONString(0);

        // 创建 Message 对象
        Message message = new Message();
        message.setSenderId(botUser.getId()); // 机器人用户的 ID
        message.setChatId(targetId);
        
        // 创建TextMessageContent对象
        TextMessageContent messageContent = new TextMessageContent();
        messageContent.setContentType(TextContentType.TEXT.getCode());
        messageContent.setContent(msgContentJson);
        message.setContent(messageContent);
        
        message.setMessageType(1); // 使用数字类型
        message.setUserIp("机器人");
        message.setSource(MessageSource.Group);
        message.setReadStatus(1); // 已读
        message.setIsRecalled(0); // 未撤回
        message.setIsShowTime(0); // 根据需要设置，默认不显示
        // 其他字段如 createdAt 和 updatedAt 将在 MessageServiceImpl 中设置

        // 发送消息到群组
        messageService.sendMessageToGroup(botUser.getId(), message);

        log.info("传递 机器人 回复 给 用户 {} 在对话 {}", userId, targetId);
    }
}
