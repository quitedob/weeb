package com.web.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文本消息的内容类型枚举
 * 用于支持富文本、图片、文件等多种消息格式
 * 简化注释：消息内容类型
 */
@Getter
@AllArgsConstructor
public enum TextContentType {
    TEXT(0, "普通文本"),
    MARKDOWN(1, "Markdown格式文本"),
    IMAGE(2, "图片"),
    FILE(3, "文件"),
    VOICE(4, "语音");

    private final Integer type;
    private final String description;
}
