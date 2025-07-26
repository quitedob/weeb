package com.web.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 联系人关系状态枚举
 * 简化注释：联系人状态
 */
@Getter
@AllArgsConstructor
public enum ContactStatus {
    PENDING(0, "待处理/申请中"),
    ACCEPTED(1, "已接受/好友"),
    DECLINED(2, "已拒绝"),
    BLOCKED(3, "已拉黑");

    private final Integer code;
    private final String description;
}
