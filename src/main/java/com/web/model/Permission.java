package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 权限实体类
 * 表示系统中的细粒度权限
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Permission extends BaseEntity {

    /**
     * 权限名称（如：USER_CREATE, ARTICLE_DELETE等）
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 权限资源（如：user, article, group等）
     */
    private String resource;

    /**
     * 权限操作（如：create, read, update, delete等）
     */
    private String action;

    /**
     * 权限条件（可选，用于更复杂的权限控制，如：own, any等）
     */
    private String condition;

    /**
     * 权限状态（0: 禁用, 1: 启用）
     */
    private Integer status;

    /**
     * 权限类型（0: 系统权限, 1: 业务权限）
     */
    private Integer type;

    /**
     * 权限分组（如：用户管理, 内容管理等）
     */
    private String group;

    /**
     * 排序号
     */
    private Integer sortOrder;

    public Permission() {
        this.status = 1;
        this.type = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Permission(String name, String description, String resource, String action) {
        this();
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.action = action;
    }

    public Permission(String name, String description, String resource, String action, String condition, Integer status, Integer type, String group) {
        this();
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.action = action;
        this.condition = condition;
        this.status = status;
        this.type = type;
        this.group = group;
    }
}
