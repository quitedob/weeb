package com.web.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * URL限制统计信息数据传输对象 (UrlLimitStats)。
 * 用于记录用户访问某个URL时的限制相关数据，例如违规次数、封禁状态等。
 */
@Data // Lombok注解，自动生成Getter、Setter、toString等方法
public class UrlLimitStats {

    /**
     * 违规次数。
     * 表示用户对某个URL的访问已经超出限制的次数。
     * 初始值为0。
     */
    private int violationCount = 0;

    /**
     * 是否被封禁。
     * 当用户的违规次数达到某个阈值时，该字段会被设置为true。
     * 默认值为false。
     */
    private boolean blocked = false;

    /**
     * 上一次违规时间。
     * 用于记录用户最近一次违规的时间点。
     * 可以帮助实现对封禁时长或其他时间限制的逻辑控制。
     */
    private LocalDateTime lastViolationTime;
}
