package com.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户安全认证数据传输对象
 * 用于Spring Security认证流程中的用户信息封装
 * 符合项目规范，使用DTO模式进行数据传输
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名（用于Spring Security认证）
     */
    private String username;

    /**
     * 密码（用于Spring Security认证，@JsonIgnore防止序列化泄露）
     */
    @JsonIgnore
    private String password;

    /**
     * 用户状态（1-正常，0-禁用）
     */
    private Integer status;

    /**
     * 权限列表
     */
    private List<String> authorities;

    /**
     * 账户是否未过期
     */
    @Builder.Default
    private boolean accountNonExpired = true;

    /**
     * 账户是否未锁定
     */
    @Builder.Default
    private boolean accountNonLocked = true;

    /**
     * 凭证是否未过期
     */
    @Builder.Default
    private boolean credentialsNonExpired = true;

    /**
     * 账户是否启用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    /**
     * 获取权限字符串数组
     */
    public String[] getAuthorityArray() {
        if (authorities == null || authorities.isEmpty()) {
            return new String[]{"ROLE_USER"};
        }
        return authorities.toArray(new String[0]);
    }

    /**
     * 添加权限
     */
    public void addAuthority(String authority) {
        if (authorities == null) {
            authorities = new java.util.ArrayList<>();
        }
        authorities.add(authority);
    }

    /**
     * 检查是否包含指定权限
     */
    public boolean hasAuthority(String authority) {
        return authorities != null && authorities.contains(authority);
    }

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN") || hasAuthority("ADMIN");
    }
}