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

    // AI配置已移除，因为AI功能已停用
    // private AiConfig doubao;
}
