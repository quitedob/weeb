package com.web.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 配置类
 * 提供 AI 相关的配置信息给前端使用
 */
@Configuration
public class AIConfig {

    @Value("${ai.provider:deepseek}")
    private String aiProvider;

    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.chat-model:deepseek-chat}")
    private String deepseekChatModel;

    @Value("${ai.ollama.chat-model:gemma3:4b}")
    private String ollamaChatModel;

    /**
     * 获取当前使用的AI提供商
     */
    public String getAiProvider() {
        return aiProvider;
    }

    /**
     * 获取当前使用的模型名称
     */
    public String getCurrentModel() {
        if ("deepseek".equalsIgnoreCase(aiProvider)) {
            return deepseekChatModel;
        } else if ("ollama".equalsIgnoreCase(aiProvider)) {
            return ollamaChatModel;
        }
        return "unknown";
    }

    /**
     * 获取AI配置信息（用于前端展示）
     * 注意：不包含敏感信息如API key
     */
    @Bean
    public Map<String, Object> aiConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", aiProvider);
        config.put("model", getCurrentModel());
        config.put("hasApiKey", deepseekApiKey != null && !deepseekApiKey.trim().isEmpty());
        return config;
    }
}
