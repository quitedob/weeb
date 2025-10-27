package com.web.vo.userlevel;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户等级历史查询VO
 */
@Data
public class UserLevelHistoryQueryVo {

    /**
     * 用户ID（可选）
     */
    private Long userId;

    /**
     * 变更类型（可选）
     * 1: 系统自动, 2: 管理员操作, 3: 用户行为触发
     */
    private Integer changeType;

    /**
     * 操作者ID（可选）
     */
    private Long operatorId;

    /**
     * 开始时间（可选）
     */
    private LocalDateTime startTime;

    /**
     * 结束时间（可选）
     */
    private LocalDateTime endTime;

    /**
     * 页码（默认1）
     */
    private int page = 1;

    /**
     * 每页大小（默认10）
     */
    private int pageSize = 10;
}
