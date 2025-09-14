package com.web.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "weeb")
public class WeebConfig {

    private String password;
    private int limit;
    private String name;
    private int expires;
    private AiConfig doubao;

    @Data
    public static class AiConfig {
        private String apiKey;
        private int countLimit;
        private int lengthLimit;
        private String model;
    }
}
