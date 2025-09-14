package com.web.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor; // Added for builder flexibility
import lombok.NoArgsConstructor;  // Added for deserialization flexibility

import java.io.Serializable;

/**
 * 在Redis Pub/Sub中广播的消息对象
 * 简化注释：Redis广播消息
 */
@Data
@Builder
@NoArgsConstructor  // Important for Jackson deserialization
@AllArgsConstructor // Useful for @Builder
public class RedisBroadcastMsg implements Serializable {
    private static final long serialVersionUID = 1L; // Good practice for Serializable classes

    /**
     * 目标用户ID
     * 简化注释：目标用户ID
     */
    private Long targetUserId;

    /**
     * 实际要发送的消息体（JSON字符串）
     * This will typically be a serialized DTO like NotifyDto or MessageDto
     * 简化注释：消息内容
     */
    private String messageBody;
}
