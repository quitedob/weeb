package com.web.constant;

/**
 * 文本内容类型枚举
 * 简化注释：文本内容类型
 */
public enum TextContentType {
    TEXT(1, "纯文本"),
    IMAGE(2, "图片"),
    FILE(3, "文件"),
    VOICE(4, "语音"),
    VIDEO(5, "视频");

    private final int code;
    private final String description;

    TextContentType(int code, String description) {
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
