package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serializable;
import java.util.Date;

/**
 * 群组实体类
 * 简化注释：群组实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "`group`") // MyBatis Plus注解, 使用反引号避免关键字冲突
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO) // MyBatis Plus主键注解
    private Long id; // 群组ID

    private String groupName; // 群名称

    private String groupDescription; // 群描述

    private Long ownerId; // 群主的用户ID

    private String groupAvatarUrl; // 群头像URL

    private Integer status; // 群组状态: 0=已解散, 1=正常, 2=冻结

    private Integer maxMembers; // 最大成员数

    private Integer memberCount; // 当前成员数

    private Date lastTransferAt; // 最后转让时间

    private Integer transferCount; // 转让次数

    private Date createTime; // 创建时间

    // 默认构造函数
    public Group() {
        this.createTime = new Date();
        this.status = 1; // 默认正常状态
        this.maxMembers = 500; // 默认最大500人
        this.memberCount = 0; // 默认0人
        this.transferCount = 0; // 默认未转让
    }

    // 创建时的构造函数
    public Group(String groupName, Long ownerId, String groupAvatarUrl) {
        this.groupName = groupName;
        this.ownerId = ownerId;
        this.groupAvatarUrl = groupAvatarUrl;
        this.createTime = new Date();
    }
}
