package com.web.vo.message;

import com.web.constant.TextContentType; // Import the new enum
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息内容的VO（视图对象）
 * 扩展以支持多种内容类型
 * 简化注释：消息内容视图
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TextMessageContent {

    /**
     * 主要内容
     * 对于文本类型，这是文本字符串
     * 对于图片/文件，这可以是描述或文件名
     * 简化注释：主要内容
     */
    private String content;

    /**
     * 内容类型
     * @see com.web.constant.TextContentType
     * 简化注释：内容类型
     */
    @Builder.Default
    private Integer contentType = TextContentType.TEXT.getCode();

    /**
     * 附加URL
     * 用于图片、文件、语音消息的资源地址
     * 简化注释：资源URL
     */
    private String url;

    /**
     * 消息中@的用户ID列表
     * 简化注释：@用户列表
     */
    @Builder.Default
    private List<Integer> atUidList = new ArrayList<>();
}
