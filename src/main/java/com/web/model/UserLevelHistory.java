package com.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 用户等级变更历史实体类
 * 用于记录用户等级变化的完整历史轨迹
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLevelHistory {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 变更前等级
     */
    private Integer oldLevel;

    /**
     * 变更后等级
     */
    private Integer newLevel;

    /**
     * 变更原因
     */
    private String changeReason;

    /**
     * 变更类型 (1: 系统自动, 2: 管理员操作, 3: 用户行为触发)
     */
    private Integer changeType;

    /**
     * 操作者ID（如果为管理员操作）
     */
    private Long operatorId;

    /**
     * 操作者名称
     */
    private String operatorName;

    /**
     * 变更时间
     */
    private LocalDateTime changeTime;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理信息
     */
    private String userAgent;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 状态 (0: 无效, 1: 有效)
     */
    private Integer status;

    /**
     * 用户信息（关联查询时使用）
     */
    private transient String username;

    /**
     * 用户昵称（关联查询时使用）
     */
    private transient String userNickname;

    /**
     * 操作者信息（关联查询时使用）
     */
    private transient String operatorUsername;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

  
    /**
     * 获取变更类型描述
     */
    public String getChangeTypeDescription() {
        switch (this.changeType) {
            case 1:
                return "系统自动";
            case 2:
                return "管理员操作";
            case 3:
                return "用户行为触发";
            default:
                return "未知";
        }
    }

    /**
     * 获取等级变化描述
     */
    public String getLevelChangeDescription() {
        if (this.oldLevel == null) {
            return "初始等级: " + this.newLevel;
        }
        if (this.oldLevel.equals(this.newLevel)) {
            return "等级保持: " + this.newLevel;
        }
        if (this.newLevel > this.oldLevel) {
            return "等级提升: " + this.oldLevel + " → " + this.newLevel;
        } else {
            return "等级降低: " + this.oldLevel + " → " + this.newLevel;
        }
    }
}