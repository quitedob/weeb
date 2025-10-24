package com.web.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * 支持多种配置类型：STRING, NUMBER, BOOLEAN, JSON, TEXT, EMAIL, URL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 配置键名（唯一）
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置显示名称
     */
    private String displayName;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 配置类型：STRING, NUMBER, BOOLEAN, JSON, TEXT, EMAIL, URL
     */
    private String configType;

    /**
     * 配置分组：system, user, message, notification, moderation, monitoring
     */
    private String configGroup;

    /**
     * 配置分类：security, performance, feature, ui, integration
     */
    private String configCategory;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 验证规则（JSON格式）
     */
    private String validationRules;

    /**
     * 是否为敏感配置（需要加密存储）
     */
    private Boolean isSensitive;

    /**
     * 是否为系统核心配置（谨慎修改）
     */
    private Boolean isSystem;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 排序字段
     */
    private Integer sortOrder;

    /**
     * 最后修改者ID
     */
    private Long updatedBy;

    /**
     * 最后修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 配置版本号
     */
    private Integer version;

    /**
     * 配置标签（逗号分隔）
     */
    private String tags;

    /**
     * 配置选项（用于SELECT类型，JSON格式）
     */
    private String configOptions;

    /**
     * 是否需要在重启后生效
     */
    private Boolean requiresRestart;

    /**
     * 配置环境标识：dev, test, prod
     */
    private String environment;

    /**
     * 配置依赖关系（JSON格式，表示此配置依赖的其他配置）
     */
    private String dependencies;

    /**
     * 配置影响范围（JSON格式，表示修改此配置会影响的功能模块）
     */
    private String impactScope;
}