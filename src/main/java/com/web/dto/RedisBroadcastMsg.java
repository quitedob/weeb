package com.web.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Message object broadcast in Redis Pub/Sub
 * Redis broadcast message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisBroadcastMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Target user ID
     */
    private Long targetUserId;

    /**
     * Actual message body to be sent (JSON string)
     * This will typically be a serialized DTO like NotifyDto or MessageDto
     */
    private String messageBody;
}