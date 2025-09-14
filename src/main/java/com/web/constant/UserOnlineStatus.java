package com.web.constant;

/**
 * 用户在线状态枚举
 * 简化注释：用户在线状态
 */
public enum UserOnlineStatus {
    ONLINE(1, "在线"),
    OFFLINE(0, "离线"),
    AWAY(2, "离开"),
    BUSY(3, "忙碌");

    private final int code;
    private final String description;

    UserOnlineStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
