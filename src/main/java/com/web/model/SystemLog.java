package com.web.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统操作日志实体类
 */
@Data
public class SystemLog {
    private Long id;
    private Long operatorId;
    private String action;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
