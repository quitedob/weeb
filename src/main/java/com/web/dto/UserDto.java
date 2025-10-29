package com.web.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象 (UserDto)。
 * 用于封装用户相关信息，便于在系统中传递和处理。
 */
@Data // Lombok注解，自动生成Getter、Setter、toString等方法
public class UserDto {

    /**
     * 用户ID。
     * 唯一标识用户的标识符，例如数据库中的主键ID或UUID。
     */
    private Long id;

    /**
     * 用户名。
     * 用户的登录名，唯一标识用户身份。
     */
    private String username;

    /**
     * 用户昵称。
     * 用户的显示名称，用于界面展示。
     */
    private String nickname;

    /**
     * 用户名称（向后兼容）。
     * 表示用户的昵称或真实姓名，用于显示或交互。
     */
    private String name;

    /**
     * 用户邮箱。
     * 用户的电子邮箱地址。
     */
    private String email;

    /**
     * 用户头像。
     * 存储用户头像的URL地址或路径，便于前端渲染用户形象。
     */
    private String avatar;

    /**
     * 个人简介。
     * 用户的个人介绍或签名。
     */
    private String bio;

    /**
     * 用户类型。
     * 表示用户的类型，例如普通用户、管理员、访客等。
     * 可以根据类型决定用户的权限和功能范围。
     */
    private String type;

    /**
     * 用户徽章信息。
     * 存储用户拥有的徽章信息，例如荣誉、等级标识等。
     */
    private String badge;

    /**
     * 用户在线状态。
     * 表示用户当前是否在线：0-离线，1-在线，2-忙碌等。
     */
    private Integer onlineStatus;

    /**
     * 用户IP归属地。
     * 表示用户当前IP地址的归属地信息，例如城市、国家等。
     * 可用于安全审计或显示地理位置。
     */
    private String ipOwnership;

    /**
     * 创建时间。
     * 用户账号的创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     * 用户信息的最后更新时间。
     */
    private LocalDateTime updatedAt;
}
