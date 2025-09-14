package com.web.constant;

/**
 * 群组角色枚举
 * 简化注释：群组角色
 */
public enum GroupRole {
    OWNER(1, "群主"),
    ADMIN(2, "管理员"),
    MEMBER(3, "普通成员");

    private final int code;
    private final String description;

    GroupRole(int code, String description) {
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
