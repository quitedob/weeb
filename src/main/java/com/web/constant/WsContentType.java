package com.web.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WsContentType {
    MSG("msg", "Normal message"),
    NOTIFY("notify", "Notification message"),
    VIDEO("video", "Video stream message"),
    FILE("file", "File message"),
    STATUS_CHANGE("status_change", "User status change notification"); // New type

    private final String type; // Keep 'type' as the field name to match existing sendMsg usage
    private final String description;
}
