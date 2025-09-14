package com.web.vo.message;

import lombok.Data;
// Using jakarta.validation.constraints.* for Spring Boot 3+
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 消息反应的VO
 * 简化注释：消息反应视图
 */
@Data
public class ReactionVo {
    @NotNull
    private Long messageId; // 消息ID

    @NotNull
    @Size(min = 1, max = 10) // Emoji通常是一个或几个字符
    private String emoji; // Emoji内容
}
