package com.web.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基础实体类
 * 包含所有实体类的公共字段
 */
@Data
public class BaseEntity {

    /**
     * 主键ID
     */
    protected Long id;

    /**
     * 创建时间
     */
    protected LocalDateTime createdAt;

    /**
     * 更新时间
     */
    protected LocalDateTime updatedAt;
}
