package com.web.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 群组成员角色枚举
 * 简化注释：群组角色
 */
@Getter
@AllArgsConstructor
public enum GroupRole {
    MEMBER(0, "普通成员"),
    ADMIN(1, "管理员"),
    OWNER(2, "群主");

    private final Integer code;
    private final String description;
}
