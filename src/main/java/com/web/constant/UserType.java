package com.web.constant;

public class UserType {
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";
    public static final String BOT = "BOT";

    public static String normalize(String type) {
        if (type == null) return USER;
        return switch (type.toUpperCase(java.util.Locale.ROOT)) {
            case ADMIN -> ADMIN;
            case BOT -> BOT;
            default -> USER;
        };
    }
    //普通用户
    public static final String User = "user";
    //机器人
    public static final String Bot = "bot";
}
