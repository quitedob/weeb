package com.web.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider;
    private Ollama ollama;
    private Deepseek deepseek;

    @Data
    public static class Ollama {
        private String baseUrl;
        private String chatModel;
    }

    @Data
    public static class Deepseek {
        private String baseUrl;
        private String apiKey;
        private String chatModel;
    }
}
