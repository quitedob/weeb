package com.web.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 联系人分组实体类
 * 用于管理用户的好友分组
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("contact_group")
public class ContactGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id; // 分组ID

    private Long userId; // 用户ID

    private String groupName; // 分组名称

    private Integer groupOrder; // 分组排序

    private Boolean isDefault; // 是否为默认分组

    private LocalDateTime createdAt; // 创建时间

    private LocalDateTime updatedAt; // 更新时间

    // 默认构造函数
    public ContactGroup() {
        this.groupOrder = 0;
        this.isDefault = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 创建分组的构造函数
    public ContactGroup(Long userId, String groupName, Integer groupOrder, Boolean isDefault) {
        this.userId = userId;
        this.groupName = groupName;
        this.groupOrder = groupOrder;
        this.isDefault = isDefault;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
