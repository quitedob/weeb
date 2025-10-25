package com.web.model;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色实体类
 * 表示系统中的用户角色，每个角色包含多个权限
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Role extends BaseEntity {

    /**
     * 角色名称（如：管理员, 版主, 用户等）
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色状态（0: 禁用, 1: 启用）
     */
    private Integer status;

    /**
     * 角色类型（0: 系统角色, 1: 自定义角色）
     */
    private Integer type;

    /**
     * 角色层级（数字越小权限越大）
     */
    private Integer level;

    /**
     * 是否为默认角色（新用户注册时自动分配）
     */
    private Boolean isDefault;

    /**
     * 角色拥有的权限列表（不持久化到数据库，只在查询时填充）
     */
    @TableField(exist = false)
    private List<Permission> permissions;

    public Role() {
        this.status = 1;
        this.type = 1;
        this.level = 100;
        this.isDefault = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Role(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
}
