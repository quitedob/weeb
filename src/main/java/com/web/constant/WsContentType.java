package com.web.constant;

/**
 * WebSocket内容类型枚举
 * 简化注释：WebSocket内容类型
 */
public enum WsContentType {
    MSG("MSG", "消息"),
    STATUS_CHANGE("STATUS_CHANGE", "状态变更"),
    NOTIFICATION("NOTIFICATION", "通知"),
    VIDEO("VIDEO", "视频");

    private final String code;
    private final String description;

    WsContentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
