package com.web.service;

import cn.hutool.core.util.StrUtil;
import com.web.Config.WeebConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
// 暂时注释掉火山引擎相关导入，需要正确的依赖
// import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
// import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
// import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
// import com.volcengine.ark.runtime.service.ArkService;
// import okhttp3.ConnectionPool;
// import okhttp3.Dispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DoubaoAiService 类，用于与 Doubao AI 进行交互，处理用户的 AI 请求并返回回复。
 * 暂时注释掉，需要正确的火山引擎SDK依赖
 */
@Service
@Slf4j
@ConditionalOnProperty(
    value = "weeb.doubao.api-key",
    matchIfMissing = false
)
public class DoubaoAiService {

    // private ArkService service;

    private final Cache<Long, AtomicInteger> limitCache;

    @Resource
    private WeebConfig weebConfig;

    /**
     * 构造函数，初始化 Caffeine 缓存。
     */
    public DoubaoAiService() {
        this.limitCache = Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS) // 缓存项在写入后 24 小时过期
                .maximumSize(10_000) // 设置缓存的最大容量
                .build();
    }

    /**
     * 初始化 ArkService 实例，用于与 Doubao AI API 交互。
     */
    // @PostConstruct
    public void init() {
        // 暂时注释掉火山引擎相关代码
        /*
        String apiKey = weebConfig.getDoubao().getApiKey();
        if (StrUtil.isBlank(apiKey)) {
            log.error("Doubao API Key is missing in the configuration.");
            throw new IllegalArgumentException("Doubao API Key cannot be null or empty.");
        }

        ConnectionPool connectionPool = new ConnectionPool(10, 20, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(100); // 根据需要设置最大请求数
        dispatcher.setMaxRequestsPerHost(20); // 根据需要设置每个主机的最大请求数

        service = ArkService.builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                .apiKey(apiKey)
                .build();

        log.info("DoubaoAiService 加载成功.");
        */
    }

    /**
     * 处理用户的 AI 请求，并返回 AI 的回复。
     *
     * @param userId  当前用户ID，类型为 Long
     * @param content 用户发送的内容，类型为 String
     * @return AI 的回复，类型为 String
     */
    public String ask(Long userId, String content) {
        // 暂时返回默认消息，需要正确的火山引擎SDK依赖
        return "AI服务暂时不可用，请稍后再试~";
        
        /*
        if (userId == null) {
            log.warn("用户id不能为空.");
            return "用户ID不能为空~";
        }

        // 获取或初始化用户的请求计数器
        AtomicInteger count = limitCache.getIfPresent(userId);
        if (count == null) {
            count = new AtomicInteger(0);
            limitCache.put(userId, count);
        }

        // 检查用户是否超过每日请求限制
        int countLimit = weebConfig.getDoubao().getCountLimit();
        if (countLimit > 0) {
            if (count.incrementAndGet() > countLimit) {
                log.warn("用户 {} 达到每天的次数限制 {} 请求.", userId, countLimit);
                return "您已经达到限制了，请24小时后再来吧~";
            }
        }

        // 检查内容是否为空
        if (StrUtil.isBlank(content)) {
            log.warn("客户输入的内容为空 {}.", userId);
            return "内容不能为空~";
        }

        // 检查内容长度是否超过限制
        int lengthLimit = weebConfig.getDoubao().getLengthLimit();
        if (lengthLimit > 0 && content.length() > lengthLimit) {
            log.warn("文本内容长度超出 用户提出的名称为： {}. 长度: {}, 限制: {}", userId, content.length(), lengthLimit);
            return "问一些简单的问题吧~";
        }

        try {
            // 构建用户消息
            List<ChatMessage> messages = new ArrayList<>();
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(content)
                    .build();
            messages.add(userMessage);

            // 构建聊天完成请求
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                    .model(weebConfig.getDoubao().getModel())
                    .messages(messages)
                    .build();

            // 发送请求到 Doubao AI 并获取回复
            StringBuilder sb = new StringBuilder();
            service.createChatCompletion(chatCompletionRequest).getChoices().forEach(choice ->
                    sb.append(choice.getMessage().getContent()).append("\n"));

            String aiReply = sb.toString().trim(); // 去除末尾的换行符

            log.info("AI reply for user {}: {}", userId, aiReply);
            return aiReply;
        } catch (Exception e) {
            log.error("Error while communicating with Doubao AI for user {}", userId, e);
            return "豆包已离家出走了，请稍后再试~";
        }
        */
    }
}
