package com.web.Config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务配置类
 * 配置 Spring AI 和 OpenAI 集成
 */
@Configuration
public class AIConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String chatModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private Integer maxTokens;

    /**
     * 创建 OpenAI API 客户端
     */
    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(openaiApiKey);
    }

    /**
     * 创建 OpenAI 聊天模型
     */
    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return new OpenAiChatModel(openAiApi);
    }

    /**
     * 创建聊天客户端
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个专业、友好的AI助手，专门为WEEB平台的用户提供帮助。")
                .defaultOptions(createChatOptions())
                .build();
    }

    /**
     * 创建聊天选项
     */
    private OpenAiChatOptions createChatOptions() {
        return OpenAiChatOptions.builder()
                .withModel(chatModel)
                .withTemperature(temperature.floatValue())
                .withMaxTokens(maxTokens)
                .build();
    }
}
