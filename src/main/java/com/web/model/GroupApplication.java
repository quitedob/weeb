package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serializable;
import java.util.Date;

/**
 * 群组申请实体类
 * 用于管理用户申请加入群组的记录
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "group_application")
public class GroupApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id; // 申请ID

    private Long groupId; // 群组ID
    private Long userId; // 申请人ID
    private String message; // 申请留言
    private String status; // 申请状态：PENDING=待审批，APPROVED=已通过，REJECTED=已拒绝
    private Long reviewerId; // 审核人ID
    private String reviewNote; // 审核备注
    private Date createdAt; // 申请时间
    private Date reviewedAt; // 审核时间

    // 默认构造函数
    public GroupApplication() {
        this.createdAt = new Date();
        this.status = "PENDING";
    }

    // 创建时的构造函数
    public GroupApplication(Long groupId, Long userId, String message) {
        this();
        this.groupId = groupId;
        this.userId = userId;
        this.message = message;
    }
}
