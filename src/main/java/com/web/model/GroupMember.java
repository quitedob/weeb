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
 * 增强版：支持申请审批流程、邀请追踪、移除审计
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "group_member")
public class GroupMember implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id; // 成员关系ID

    private Long groupId; // 群组ID
    private Long userId; // 用户ID
    private Integer role; // 成员角色 (1=群主, 2=管理员, 3=普通成员)
    
    // 新增字段：支持申请审批流程
    private String joinStatus; // 加入状态：PENDING=待审批，ACCEPTED=已接受，REJECTED=已拒绝，BLOCKED=已屏蔽
    private Long invitedBy; // 邀请人ID
    private String inviteReason; // 邀请/申请原因
    
    private Date joinTime; // 加入时间
    private Date updateTime; // 更新时间
    
    // 新增字段：支持移除审计
    private Date kickedAt; // 被移除时间
    private String kickReason; // 被移除原因

    // 默认构造函数
    public GroupMember() {
        Date now = new Date();
        this.joinTime = now;
        this.updateTime = now;
        this.joinStatus = "ACCEPTED"; // 默认已接受
    }

    // 创建时的构造函数
    public GroupMember(Long groupId, Long userId, Integer role) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        Date now = new Date();
        this.joinTime = now;
        this.updateTime = now;
        this.joinStatus = "ACCEPTED";
    }
    
    // 带邀请人的构造函数
    public GroupMember(Long groupId, Long userId, Integer role, Long invitedBy, String inviteReason) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.invitedBy = invitedBy;
        this.inviteReason = inviteReason;
        Date now = new Date();
        this.joinTime = now;
        this.updateTime = now;
        this.joinStatus = "ACCEPTED";
    }
}
