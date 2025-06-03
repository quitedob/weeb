package com.web.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户在线状态枚举
 * 简化注释：用户在线状态
 */
@Getter
@AllArgsConstructor
public enum UserOnlineStatus {
    OFFLINE(0, "离线"),
    ONLINE(1, "在线"),
    AWAY(2, "离开"),
    BUSY(3, "忙碌");

    private final Integer code;
    private final String description;
}
