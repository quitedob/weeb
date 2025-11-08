package com.web.vo.message;

import lombok.Data;
import java.sql.Timestamp;

/**
 * WebSocket消息响应对象
 * 用于向前端返回标准化的消息格式
 */
@Data
public class MessageResponse {
    /**
     * 消息ID
     */
    private Long id;

    /**
     * 消息ID（前端期望的字段名）
     */
    private Long messageId;

    /**
     * 客户端消息ID（用于关联临时消息）
     */
    private String clientMessageId;

    /**
     * 发送者ID
     */
    private Long fromId;

    /**
     * 发送者用户名
     */
    private String fromName;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 显示内容（与content相同，兼容前端）
     */
    private String msgContent;

    /**
     * 消息类型（1=文本，2=文件，3=图片等）
     */
    private Integer messageType;

    /**
     * 聊天ID（String类型，因为chat_list.id是VARCHAR）
     */
    private String chatId;

    /**
     * 目标ID（前端期望的字段）
     */
    private Long targetId;

    /**
     * 房间ID（兼容旧格式）
     */
    private String roomId;

    /**
     * 消息状态（0=发送中，1=已发送，2=已送达，3=已读，4=失败）
     */
    private Integer status;

    /**
     * 是否是自己发的消息
     */
    private Boolean isFromMe;

    /**
     * 是否已撤回
     */
    private Integer isRecalled;

    /**
     * 消息时间戳
     */
    private Timestamp timestamp;

    /**
     * 文件数据（如果是文件消息）
     */
    private Object fileData;
}
