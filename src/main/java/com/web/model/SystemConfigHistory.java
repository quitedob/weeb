package com.web.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统配置变更历史实体类
 * 记录配置的修改历史，支持审计和回滚
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigHistory {

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 配置键名
     */
    private String configKey;

    /**
     * 变更前的值
     */
    private String oldValue;

    /**
     * 变更后的值
     */
    private String newValue;

    /**
     * 操作类型：CREATE, UPDATE, DELETE, IMPORT, RESTORE
     */
    private String actionType;

    /**
     * 操作者ID
     */
    private Long operatorId;

    /**
     * 操作者用户名
     */
    private String operatorName;

    /**
     * 操作原因/备注
     */
    private String remark;

    /**
     * 操作IP地址
     */
    private String ipAddress;

    /**
     * 用户代理信息
     */
    private String userAgent;

    /**
     * 变更时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 变更来源：web, api, scheduled, backup_restore
     */
    private String source;

    /**
     * 批次ID（用于批量操作的关联）
     */
    private String batchId;

    /**
     * 是否为回滚操作
     */
    private Boolean isRollback;

    /**
     * 关联的历史记录ID（回滚时的原记录ID）
     */
    private Long relatedHistoryId;

    /**
     * 变更影响的功能模块
     */
    private String affectedModules;

    /**
     * 配置版本号（变更前）
     */
    private Integer oldVersion;

    /**
     * 配置版本号（变更后）
     */
    private Integer newVersion;

    /**
     * 变更是否成功
     */
    private Boolean success;

    /**
     * 错误信息（如果变更失败）
     */
    private String errorMessage;

    /**
     * 变更的上下文信息（JSON格式）
     */
    private String context;
}