package com.web.Config;

/**
 * 安全验证常量和工具类
 * 提供密码策略、用户名策略等安全验证相关的常量和方法
 */
public class SecurityConstants {

    /**
     * 密码策略配置
     */
    public static class PasswordPolicy {
        public static final int MIN_LENGTH = 6;
        public static final int MAX_LENGTH = 100;
        public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,}$";
        public static final String REQUIREMENT = "密码必须包含至少一个字母和一个数字，长度在6-100个字符之间";
    }

    /**
     * 登录安全策略
     */
    public static class LoginSecurity {
        public static final int MAX_LOGIN_ATTEMPTS = 5;
        public static final long LOCK_TIME_MINUTES = 30;
        public static final String REQUIREMENT = "连续登录失败5次后，账号将被锁定30分钟";
    }

    /**
     * 用户名策略
     */
    public static class UsernamePolicy {
        public static final int MIN_LENGTH = 3;
        public static final int MAX_LENGTH = 50;
        public static final String PATTERN = "^[a-zA-Z0-9_]{3,50}$";
        public static final String REQUIREMENT = "用户名只能包含字母、数字和下划线，长度在3-50个字符之间";
    }

    /**
     * 邮箱策略
     */
    public static class EmailPolicy {
        public static final String PATTERN = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        public static final String REQUIREMENT = "请输入有效的邮箱地址";
    }

    /**
     * 手机号策略
     */
    public static class PhonePolicy {
        public static final String PATTERN = "^1[3-9]\\d{9}$";
        public static final String REQUIREMENT = "请输入有效的手机号码（支持国际区号格式，如+8613800138000）";
    }

    /**
     * 验证密码是否符合策略
     */
    public static boolean validatePassword(String password) {
        if (password == null || password.length() < PasswordPolicy.MIN_LENGTH || password.length() > PasswordPolicy.MAX_LENGTH) {
            return false;
        }
        return password.matches(PasswordPolicy.PATTERN);
    }

    /**
     * 验证用户名是否符合策略
     */
    public static boolean validateUsername(String username) {
        if (username == null || username.length() < UsernamePolicy.MIN_LENGTH || username.length() > UsernamePolicy.MAX_LENGTH) {
            return false;
        }
        return username.matches(UsernamePolicy.PATTERN);
    }

    /**
     * 验证邮箱是否符合策略
     */
    public static boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches(EmailPolicy.PATTERN);
    }

    /**
     * 验证手机号是否符合策略
     */
    public static boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return phone.matches(PhonePolicy.PATTERN);
    }
}
