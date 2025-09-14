package com.web.constant;

/**
 * 联系人状态枚举
 * 简化注释：联系人状态
 */
public enum ContactStatus {
    PENDING(0, "待处理"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝"),
    BLOCKED(3, "已屏蔽");

    private final int code;
    private final String description;

    ContactStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
