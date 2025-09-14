package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serializable;
import java.util.Date;

/**
 * 群组成员实体类
 * 简化注释：群组成员实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "group_member") // MyBatis Plus注解
public class GroupMember implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO) // MyBatis Plus主键注解
    private Long id; // 成员关系ID

    private Long groupId; // 群组ID
    private Long userId; // 用户ID
    private Integer role; // 成员角色 (1: 群主, 2: 管理员, 3: 普通成员)
    private Date joinTime; // 加入时间
    private Date updateTime; // 更新时间

    // 默认构造函数
    public GroupMember() {
        Date now = new Date();
        this.joinTime = now;
        this.updateTime = now;
    }

    // 创建时的构造函数
    public GroupMember(Long groupId, Long userId, Integer role) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        Date now = new Date();
        this.joinTime = now;
        this.updateTime = now;
    }
}
